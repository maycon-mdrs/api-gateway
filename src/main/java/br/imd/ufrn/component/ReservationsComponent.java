package br.imd.ufrn.component;

import br.imd.ufrn.exceptions.CamaroesBaseException;
import br.imd.ufrn.gateway.ApiGateway;
import br.imd.ufrn.models.Reservation;
import br.imd.ufrn.models.ServerRole;
import br.imd.ufrn.services.ReservationService;
import br.imd.ufrn.services.TableService;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.StringTokenizer;
import java.util.UUID;

public class ReservationsComponent {

    private final int listenPort;
    private final String gatewayHost;
    private final int gatewayControlPort;
    private final String advertiseHost;
    private final ServerRole role;
    private final ReservationService reservationService;

    public ReservationsComponent(
            int listenPort,
            String gatewayHost,
            int gatewayControlPort,
            String advertiseHost,
            ServerRole role) {
        this.listenPort = listenPort;
        this.gatewayHost = gatewayHost;
        this.gatewayControlPort = gatewayControlPort;
        this.advertiseHost = advertiseHost;
        this.role = role;
        this.reservationService = new ReservationService(new TableService(), role);
    }

    public void start() throws IOException {
        String instanceId = "reservations-" + role.name().toLowerCase() + "-" + listenPort
                + "-" + UUID.randomUUID().toString().substring(0, 8);

        HeartbeatClient heartbeat = new HeartbeatClient(
                gatewayHost,
                gatewayControlPort,
                "reservations",
                advertiseHost,
                listenPort,
                instanceId,
                1_000);
        heartbeat.start();

        Runtime.getRuntime().addShutdownHook(new Thread(heartbeat::close));

        System.out.println("[" + instanceId + "] papel=" + role);
        new ComponentTcpServer(listenPort, instanceId, this::handle).run();
    }

    private String handle(String request) {
        try {
            StringTokenizer tokenizer = new StringTokenizer(request);
            if (!tokenizer.hasMoreTokens()) {
                return "ERROR empty";
            }

            String command = tokenizer.nextToken().toUpperCase();
            return switch (command) {
                case "CREATE" -> handleCreate(tokenizer);
                case "GET" -> handleGet(tokenizer);
                case "CANCEL" -> handleCancel(tokenizer);
                case "ROLE" -> "OK " + role;
                default -> "ERROR unknown command";
            };
        } catch (CamaroesBaseException e) {
            return "ERROR " + e.getFriendlyMessage();
        } catch (Exception e) {
            return "ERROR " + e.getMessage();
        }
    }

    private String handleCreate(StringTokenizer tokenizer) {
        // CREATE customer tableId start end people
        if (tokenizer.countTokens() < 5) {
            return "ERROR usage: CREATE customer tableId start end people";
        }
        String customer = tokenizer.nextToken();
        String tableId = tokenizer.nextToken();
        LocalDateTime start = LocalDateTime.parse(tokenizer.nextToken());
        LocalDateTime end = LocalDateTime.parse(tokenizer.nextToken());
        int people = Integer.parseInt(tokenizer.nextToken());

        Reservation reservation = reservationService.create(customer, tableId, start, end, people);
        return "OK " + reservation;
    }

    private String handleGet(StringTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR usage: GET id";
        }
        return reservationService.findById(tokenizer.nextToken())
                .map(r -> "OK " + r)
                .orElse("ERROR reservation not found");
    }

    private String handleCancel(StringTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR usage: CANCEL id";
        }
        Reservation cancelled = reservationService.cancel(tokenizer.nextToken());
        return "OK " + cancelled;
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9201;
        String gatewayHost = args.length > 1 ? args[1] : "127.0.0.1";
        int controlPort = args.length > 2 ? Integer.parseInt(args[2]) : ApiGateway.CONTROL_PORT;
        String advertiseHost = args.length > 3 ? args[3] : "127.0.0.1";
        ServerRole role = args.length > 4
                ? ServerRole.valueOf(args[4].toUpperCase())
                : ServerRole.LEADING;
        new ReservationsComponent(port, gatewayHost, controlPort, advertiseHost, role).start();
    }
}
