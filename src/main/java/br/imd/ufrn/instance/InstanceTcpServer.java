package br.imd.ufrn.instance;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Lado da instância: escuta TCP na porta local.
 * Aceita linha {@code TIME br|pt} ou HTTP {@code GET /time/br|pt}.
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
            System.out.println("[" + instanceId + "] ouvindo TCP/HTTP " + listenPort + " zone=" + zoneId);
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
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

            String firstLine = in.readLine();
            if (firstLine == null || firstLine.isBlank()) {
                writePlainLine(s, "ERROR empty");
                return;
            }

            if (firstLine.toUpperCase().startsWith("GET ")) {
                handleHttp(s, in, firstLine);
                return;
            }

            writePlainLine(s, answerTimeLine(firstLine));
        } catch (IOException e) {
            System.err.println("[" + instanceId + "] falha: " + e.getMessage());
        }
    }

    private void handleHttp(Socket socket, BufferedReader in, String requestLine) throws IOException {
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            // ignora headers
        }

        StringTokenizer tokenizer = new StringTokenizer(requestLine);
        if (tokenizer.countTokens() < 2) {
            sendHttp(socket, 400, "Bad Request\n");
            return;
        }

        tokenizer.nextToken();
        String pathAndQuery = tokenizer.nextToken();
        String path = pathAndQuery;
        int q = pathAndQuery.indexOf('?');
        if (q >= 0) {
            path = pathAndQuery.substring(0, q);
        }

        if ("/time/br".equals(path) || "/time/pt".equals(path)) {
            String zone = path.substring("/time/".length());
            String body = answerZone(zone) + "\n";
            int status = body.startsWith("OK ") ? 200 : 400;
            sendHttp(socket, status, body);
            return;
        }

        sendHttp(socket, 404, "Not Found\n");
    }

    private String answerTimeLine(String line) {
        String[] parts = line.trim().split("\\s+");
        if (parts.length < 2 || !"TIME".equalsIgnoreCase(parts[0])) {
            return "ERROR usage: TIME br|pt";
        }
        return answerZone(parts[1].toLowerCase());
    }

    private String answerZone(String requested) {
        if (!serviceType.equals(requested)) {
            return "ERROR this instance is " + serviceType + ", got TIME " + requested;
        }
        String now = ZonedDateTime.now(zoneId).format(FORMATTER);
        return "OK " + now;
    }

    private void writePlainLine(Socket socket, String response) throws IOException {
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        out.println(response);
    }

    private void sendHttp(Socket socket, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String statusText = switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            default -> "Error";
        };

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeBytes("HTTP/1.0 " + status + " " + statusText + "\r\n");
        out.writeBytes("Content-Type: text/plain; charset=utf-8\r\n");
        out.writeBytes("Content-Length: " + bytes.length + "\r\n");
        out.writeBytes("Connection: close\r\n");
        out.writeBytes("\r\n");
        out.write(bytes);
        out.flush();
    }
}
