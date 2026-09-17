package br.imd.ufrn.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Ao iniciar: REGISTER no gateway.
 * Depois: HEARTBEAT periódico.
 */
public class HeartbeatClient implements AutoCloseable {

    private final String gatewayHost;
    private final int gatewayControlPort;
    private final String componentType;
    private final String advertiseHost;
    private final int advertisePort;
    private final String instanceId;
    private final long intervalMillis;
    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public HeartbeatClient(
            String gatewayHost,
            int gatewayControlPort,
            String componentType,
            String advertiseHost,
            int advertisePort,
            String instanceId,
            long intervalMillis) {
        this.gatewayHost = gatewayHost;
        this.gatewayControlPort = gatewayControlPort;
        this.componentType = componentType;
        this.advertiseHost = advertiseHost;
        this.advertisePort = advertisePort;
        this.instanceId = instanceId;
        this.intervalMillis = intervalMillis;
    }

    public void start() throws IOException {
        String register = "REGISTER " + componentType + " " + advertiseHost + " "
                + advertisePort + " " + instanceId;
        String response = send(register);
        System.out.println("[" + instanceId + "] " + register + " -> " + response);

        scheduler.scheduleAtFixedRate(() -> {
            try {
                send("HEARTBEAT " + instanceId);
            } catch (IOException e) {
                System.err.println("[" + instanceId + "] heartbeat falhou: " + e.getMessage());
            }
        }, intervalMillis, intervalMillis, TimeUnit.MILLISECONDS);
    }

    private String send(String message) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(gatewayHost, gatewayControlPort), 2_000);
            socket.setSoTimeout(2_000);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(message);
            String response = in.readLine();
            return response == null ? "ERROR empty" : response;
        }
    }

    @Override
    public void close() {
        scheduler.shutdownNow();
        try {
            send("UNREGISTER " + instanceId);
        } catch (IOException ignored) {
            // gateway pode já estar fora
        }
    }
}
