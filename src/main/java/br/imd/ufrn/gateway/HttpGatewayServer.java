package br.imd.ufrn.gateway;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entrada HTTP do cliente (porta 8080).
 * GET /tables
 * GET /reservations/{id}
 * POST /reservations?customer=Ana&tableId=M03&start=...&end=...&people=4
 * GET /registry
 */
public class HttpGatewayServer implements Runnable {

    private final int port;
    private final RequestRouter router;
    private final ServiceRegistry registry;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public HttpGatewayServer(int port, RequestRouter router, ServiceRegistry registry) {
        this.port = port;
        this.router = router;
        this.registry = registry;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port, 100)) {
            System.out.println("[http-gateway] ouvindo HTTP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[http-gateway] erro: " + e.getMessage());
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8))) {

            String headerLine = in.readLine();
            if (headerLine == null || headerLine.isBlank()) {
                sendResponse(s, 400, "Bad Request");
                return;
            }

            // consome headers restantes
            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // ignore
            }

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            if (tokenizer.countTokens() < 2) {
                sendResponse(s, 400, "Bad Request");
                return;
            }

            String method = tokenizer.nextToken().toUpperCase();
            String pathAndQuery = tokenizer.nextToken();
            String path = pathAndQuery;
            String query = "";
            int q = pathAndQuery.indexOf('?');
            if (q >= 0) {
                path = pathAndQuery.substring(0, q);
                query = pathAndQuery.substring(q + 1);
            }

            routeHttp(s, method, path, query);
        } catch (Exception e) {
            System.err.println("[http-gateway] falha: " + e.getMessage());
        }
    }

    private void routeHttp(Socket socket, String method, String path, String query) throws IOException {
        if ("GET".equals(method) && "/registry".equals(path)) {
            StringBuilder body = new StringBuilder();
            for (ServiceInstance instance : registry.listAll()) {
                body.append(instance).append("\n");
            }
            if (body.isEmpty()) {
                body.append("(nenhum componente registrado)\n");
            }
            sendResponse(socket, 200, body.toString());
            return;
        }

        if ("GET".equals(method) && "/tables".equals(path)) {
            String response = router.route("tables", "LIST");
            sendResponse(socket, response.startsWith("ERROR") ? 503 : 200, response);
            return;
        }

        if ("GET".equals(method) && path.startsWith("/reservations/")) {
            String id = path.substring("/reservations/".length());
            String response = router.route("reservations", "GET " + id);
            sendResponse(socket, response.startsWith("ERROR") ? 404 : 200, response);
            return;
        }

        if ("POST".equals(method) && "/reservations".equals(path)) {
            String payload = buildCreatePayload(query);
            if (payload == null) {
                sendResponse(socket, 400, "ERROR missing query params");
                return;
            }
            String response = router.route("reservations", payload);
            sendResponse(socket, response.startsWith("ERROR") ? 400 : 201, response);
            return;
        }

        sendResponse(socket, 404, "Not Found");
    }

    private String buildCreatePayload(String query) {
        String customer = null;
        String tableId = null;
        String start = null;
        String end = null;
        String people = null;

        for (String part : query.split("&")) {
            if (part.isBlank()) {
                continue;
            }
            String[] kv = part.split("=", 2);
            if (kv.length != 2) {
                continue;
            }
            switch (kv[0]) {
                case "customer" -> customer = kv[1];
                case "tableId" -> tableId = kv[1];
                case "start" -> start = kv[1];
                case "end" -> end = kv[1];
                case "people" -> people = kv[1];
                default -> {
                }
            }
        }

        if (customer == null || tableId == null || start == null || end == null || people == null) {
            return null;
        }
        return "CREATE " + customer + " " + tableId + " " + start + " " + end + " " + people;
    }

    private void sendResponse(Socket socket, int statusCode, String body) throws IOException {
        String reason = switch (statusCode) {
            case 200 -> "OK";
            case 201 -> "Created";
            case 400 -> "Bad Request";
            case 404 -> "Not Found";
            case 503 -> "Service Unavailable";
            default -> "Error";
        };

        byte[] bodyBytes = body.getBytes(StandardCharsets.UTF_8);
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());
        out.writeBytes("HTTP/1.0 " + statusCode + " " + reason + "\r\n");
        out.writeBytes("Server: CamaroesApiGateway\r\n");
        out.writeBytes("Content-Type: text/plain; charset=utf-8\r\n");
        out.writeBytes("Content-Length: " + bodyBytes.length + "\r\n");
        out.writeBytes("Connection: close\r\n");
        out.writeBytes("\r\n");
        out.write(bodyBytes);
        out.flush();
    }
}
