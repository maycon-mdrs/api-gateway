package br.imd.ufrn.lab.instance;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lado da instância: escuta a porta local e responde TIME br|pt.
 */
public class InstanceTcpServer implements Runnable {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final String instanceId;
    private final String serviceType;
    private final int listenPort;
    private final ZoneId zoneId;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public InstanceTcpServer(String instanceId, String serviceType, int listenPort) {
        this.instanceId = instanceId;
        this.serviceType = serviceType;
        this.listenPort = listenPort;
        this.zoneId = "br".equals(serviceType)
                ? ZoneId.of("America/Sao_Paulo")
                : ZoneId.of("Europe/Lisbon");
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(listenPort)) {
            System.out.println("[" + instanceId + "] ouvindo TCP " + listenPort + " zone=" + zoneId);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handleRequest(socket));
            }
        } catch (IOException e) {
            System.err.println("[" + instanceId + "] erro: " + e.getMessage());
        }
    }

    private void handleRequest(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = in.readLine();
            if (line == null || line.isBlank()) {
                out.println("ERROR empty");
                return;
            }

            String[] parts = line.trim().split("\\s+");
            if (parts.length < 2 || !"TIME".equalsIgnoreCase(parts[0])) {
                out.println("ERROR usage: TIME br|pt");
                return;
            }

            String requested = parts[1].toLowerCase();
            if (!serviceType.equals(requested)) {
                out.println("ERROR this instance is " + serviceType + ", got TIME " + requested);
                return;
            }

            String now = ZonedDateTime.now(zoneId).format(FORMATTER);
            out.println("OK " + now);
        } catch (IOException e) {
            System.err.println("[" + instanceId + "] falha: " + e.getMessage());
        }
    }
}
