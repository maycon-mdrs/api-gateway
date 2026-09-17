package br.imd.ufrn.component;

import br.imd.ufrn.gateway.ApiGateway;
import br.imd.ufrn.models.Table;
import br.imd.ufrn.services.TableService;

import java.io.IOException;
import java.util.StringJoiner;
import java.util.UUID;

public class TablesComponent {

    private final int listenPort;
    private final String gatewayHost;
    private final int gatewayControlPort;
    private final String advertiseHost;
    private final TableService tableService = new TableService();

    public TablesComponent(int listenPort, String gatewayHost, int gatewayControlPort, String advertiseHost) {
        this.listenPort = listenPort;
        this.gatewayHost = gatewayHost;
        this.gatewayControlPort = gatewayControlPort;
        this.advertiseHost = advertiseHost;
    }

    public void start() throws IOException {
        String instanceId = "tables-" + listenPort + "-" + UUID.randomUUID().toString().substring(0, 8);

        HeartbeatClient heartbeat = new HeartbeatClient(
                gatewayHost,
                gatewayControlPort,
                "tables",
                advertiseHost,
                listenPort,
                instanceId,
                1_000);
        heartbeat.start();

        Runtime.getRuntime().addShutdownHook(new Thread(heartbeat::close));

        new ComponentTcpServer(listenPort, instanceId, this::handle).run();
    }

    private String handle(String request) {
        String[] parts = request.split("\\s+", 2);
        String command = parts[0].toUpperCase();

        if ("LIST".equals(command)) {
            StringJoiner joiner = new StringJoiner(";");
            for (Table table : tableService.findActive()) {
                joiner.add(table.getId() + "," + table.getCapacity() + "," + table.getArea());
            }
            return "OK " + joiner;
        }

        if ("GET".equals(command) && parts.length == 2) {
            return tableService.findById(parts[1])
                    .map(t -> "OK " + t)
                    .orElse("ERROR table not found");
        }

        return "ERROR unknown command";
    }

    public static void main(String[] args) throws Exception {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : 9101;
        String gatewayHost = args.length > 1 ? args[1] : "127.0.0.1";
        int controlPort = args.length > 2 ? Integer.parseInt(args[2]) : ApiGateway.CONTROL_PORT;
        String advertiseHost = args.length > 3 ? args[3] : "127.0.0.1";
        new TablesComponent(port, gatewayHost, controlPort, advertiseHost).start();
    }
}
