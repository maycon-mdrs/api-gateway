package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

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

public class ClientHttpServer implements Runnable {

    private final int port;
    private final TimeRequestHandler handler;
    private final InstanceRegistry registry;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public ClientHttpServer(int port, TimeRequestHandler handler, InstanceRegistry registry) {
        this.port = port;
        this.handler = handler;
        this.registry = registry;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[http] ouvindo HTTP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[http] erro: " + e.getMessage());
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

            String line;
            while ((line = in.readLine()) != null && !line.isEmpty()) {
                // ignora headers
            }

            StringTokenizer tokenizer = new StringTokenizer(headerLine);
            if (tokenizer.countTokens() < 2) {
                sendResponse(s, 400, "Bad Request");
                return;
            }

            String method = tokenizer.nextToken().toUpperCase();
            String pathAndQuery = tokenizer.nextToken();
            String path = pathAndQuery;
            int q = pathAndQuery.indexOf('?');
            if (q >= 0) {
                path = pathAndQuery.substring(0, q);
            }

            if (!"GET".equals(method)) {
                sendResponse(s, 405, "Method Not Allowed");
                return;
            }

            if ("/registry".equals(path)) {
                StringBuilder body = new StringBuilder();
                for (InstanceInfo instance : registry.listAllInstances()) {
                    body.append(instance).append('\n');
                }
                if (body.isEmpty()) {
                    body.append("(nenhuma instancia registrada)\n");
                }
                sendResponse(s, 200, body.toString());
                return;
            }

            if ("/time/br".equals(path)) {
                String body = handler.handleZone("br", TransportProtocol.HTTP) + "\n";
                sendResponse(s, body.startsWith("OK ") ? 200 : 502, body);
                return;
            }
            if ("/time/pt".equals(path)) {
                String body = handler.handleZone("pt", TransportProtocol.HTTP) + "\n";
                sendResponse(s, body.startsWith("OK ") ? 200 : 502, body);
                return;
            }

            sendResponse(s, 404, "Not Found\n");
        } catch (Exception e) {
            System.err.println("[http] falha: " + e.getMessage());
        }
    }

    private void sendResponse(Socket socket, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        String statusText;
        switch (status) {
            case 200:
                statusText = "OK";
                break;
            case 400:
                statusText = "Bad Request";
                break;
            case 404:
                statusText = "Not Found";
                break;
            case 405:
                statusText = "Method Not Allowed";
                break;
            case 502:
                statusText = "Bad Gateway";
                break;
            default:
                statusText = "Error";
                break;
        }

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
