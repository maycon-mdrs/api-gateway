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
    /** Conexao ociosa alem disso e encerrada -- evita segurar sockets do pool do gateway para sempre. */
    private static final int IDLE_TIMEOUT_MILLIS = 30_000;

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
            s.setSoTimeout(IDLE_TIMEOUT_MILLIS);

            // Uma conexao pode carregar varias requisicoes seguidas (TCP-line ou HTTP
            // keep-alive) -- so encerra quando o peer fecha, fica ociosa demais ou pede close.
            String firstLine;
            while ((firstLine = in.readLine()) != null) {
                if (firstLine.isBlank()) {
                    continue;
                }

                if (firstLine.toUpperCase().startsWith("GET ")) {
                    if (!handleHttp(s, in, firstLine)) {
                        return;
                    }
                } else {
                    writePlainLine(s, answerTimeLine(firstLine));
                }
            }
        } catch (java.net.SocketTimeoutException e) {
            // ociosa por tempo demais -- fecha em silencio, e comportamento esperado
        } catch (IOException e) {
            System.err.println("[" + instanceId + "] falha: " + e.getMessage());
        }
    }

    /** @return true se a conexao deve continuar aberta para a proxima requisicao. */
    private boolean handleHttp(Socket socket, BufferedReader in, String requestLine) throws IOException {
        boolean clientWantsClose = false;
        String line;
        while ((line = in.readLine()) != null && !line.isEmpty()) {
            if (line.toLowerCase().startsWith("connection:") && line.toLowerCase().contains("close")) {
                clientWantsClose = true;
            }
        }

        StringTokenizer tokenizer = new StringTokenizer(requestLine);
        if (tokenizer.countTokens() < 2) {
            sendHttp(socket, 400, "Bad Request\n", clientWantsClose);
            return !clientWantsClose;
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
            sendHttp(socket, status, body, clientWantsClose);
            return !clientWantsClose;
        }

        sendHttp(socket, 404, "Not Found\n", clientWantsClose);
        return !clientWantsClose;
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

    private void sendHttp(Socket socket, int status, String body, boolean close) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String statusText = switch (status) {
            case 200 -> "OK";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            default -> "Error";
        };

        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeBytes("HTTP/1.1 " + status + " " + statusText + "\r\n");
        out.writeBytes("Content-Type: text/plain; charset=utf-8\r\n");
        out.writeBytes("Content-Length: " + bytes.length + "\r\n");
        out.writeBytes("Connection: " + (close ? "close" : "keep-alive") + "\r\n");
        out.writeBytes("\r\n");
        out.write(bytes);
        out.flush();
    }
}
