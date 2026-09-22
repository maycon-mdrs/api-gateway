package br.imd.ufrn.instance;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

public class InstanceUdpServer implements Runnable {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final String instanceId;
    private final String serviceType;
    private final int listenPort;
    private final ZoneId zoneId;

    public InstanceUdpServer(String instanceId, String serviceType, int listenPort) {
        this.instanceId = instanceId;
        this.serviceType = serviceType;
        this.listenPort = listenPort;
        this.zoneId = "br".equals(serviceType)
                ? ZoneId.of("America/Sao_Paulo")
                : ZoneId.of("Europe/Lisbon");
    }

    @Override
    public void run() {
        try (DatagramSocket socket = new DatagramSocket(listenPort)) {
            System.out.println("[" + instanceId + "] ouvindo UDP " + listenPort + " zone=" + zoneId);
            byte[] buffer = new byte[2048];
            while (!Thread.currentThread().isInterrupted()) {
                DatagramPacket request = new DatagramPacket(buffer, buffer.length);
                socket.receive(request);

                String line = new String(
                        request.getData(), request.getOffset(), request.getLength(), StandardCharsets.UTF_8);
                String response = answer(line);
                byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
                DatagramPacket responsePacket = new DatagramPacket(
                        bytes, bytes.length, request.getAddress(), request.getPort());
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            System.err.println("[" + instanceId + "] UDP erro: " + e.getMessage());
        }
    }

    private String answer(String line) {
        if (line == null || line.isBlank()) {
            return "ERROR empty";
        }

        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2 || !"TIME".equalsIgnoreCase(parts[0])) {
            return "ERROR usage: TIME br|pt";
        }

        String requested = parts[1].toLowerCase();
        if (!serviceType.equals(requested)) {
            return "ERROR this instance is " + serviceType + ", got TIME " + requested;
        }

        String now = ZonedDateTime.now(zoneId).format(FORMATTER);
        return "OK " + now;
    }
}
