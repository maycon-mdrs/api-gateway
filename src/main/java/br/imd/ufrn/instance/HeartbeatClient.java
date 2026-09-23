package br.imd.ufrn.instance;

import br.imd.ufrn.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HeartbeatClient {

    private static final int HEARTBEAT_INTERVAL_MS = 3_000;
    private static final int SOCKET_TIMEOUT_MS = 2_000;

    private final String gatewayHost;
    private final int gatewayHeartbeatPort;
    private final String serviceType;
    private final String advertiseHost;
    private final int listenPort;
    private final String instanceId;

    public HeartbeatClient(
            String gatewayHost,
            int gatewayHeartbeatPort,
            String serviceType,
            String advertiseHost,
            int listenPort,
            String instanceId) {
        this.gatewayHost = gatewayHost;
        this.gatewayHeartbeatPort = gatewayHeartbeatPort;
        this.serviceType = serviceType;
        this.advertiseHost = advertiseHost;
        this.listenPort = listenPort;
        this.instanceId = instanceId;
    }

    public void start() {
        Thread thread = new Thread(this::loop, "hb-" + instanceId);
        thread.setDaemon(true);
        thread.start();
    }

    private void loop() {
        try {
            sendLine("REGISTER " + serviceType + " " + advertiseHost + " "
                    + listenPort + " " + instanceId);
        } catch (IOException e) {
            Log.error("[" + instanceId + "] REGISTER handshake falhou em "
                    + gatewayHost + ":" + gatewayHeartbeatPort, e);
        }

        while (!Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(HEARTBEAT_INTERVAL_MS);
                sendLine("HEARTBEAT " + instanceId);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            } catch (IOException e) {
                Log.error("[" + instanceId + "] HEARTBEAT handshake falhou em "
                        + gatewayHost + ":" + gatewayHeartbeatPort, e);
            }
        }
    }

    private void sendLine(String line) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(new InetSocketAddress(gatewayHost, gatewayHeartbeatPort), SOCKET_TIMEOUT_MS);
            socket.setSoTimeout(SOCKET_TIMEOUT_MS);
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            out.println(line);
            String response = in.readLine();
            if (response == null || response.startsWith("ERROR")) {
                Log.error("[" + instanceId + "] " + line + " -> " + response);
            }
        }
    }
}
