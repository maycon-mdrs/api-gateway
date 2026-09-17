package br.imd.ufrn.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entrada TCP do cliente (ex.: JMeter TCP Sampler).
 * Formato: ROUTE <type> <payload...>
 * Ex.: ROUTE tables LIST
 *      ROUTE reservations CREATE Ana M03 2026-09-20T19:00 2026-09-20T21:00 4
 */
public class TcpGatewayServer implements Runnable {

    private final int port;
    private final RequestRouter router;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public TcpGatewayServer(int port, RequestRouter router) {
        this.port = port;
        this.router = router;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[tcp-gateway] ouvindo TCP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[tcp-gateway] erro: " + e.getMessage());
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = in.readLine();
            if (line == null || line.isBlank()) {
                out.println("ERROR empty");
                return;
            }
            out.println(process(line.trim()));
        } catch (IOException e) {
            System.err.println("[tcp-gateway] falha: " + e.getMessage());
        }
    }

    private String process(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR empty";
        }

        String command = tokenizer.nextToken().toUpperCase();
        if (!"ROUTE".equals(command)) {
            return "ERROR usage: ROUTE <type> <payload>";
        }
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR missing component type";
        }

        String type = tokenizer.nextToken();
        StringBuilder payload = new StringBuilder();
        while (tokenizer.hasMoreTokens()) {
            if (!payload.isEmpty()) {
                payload.append(' ');
            }
            payload.append(tokenizer.nextToken());
        }
        if (payload.isEmpty()) {
            return "ERROR missing payload";
        }

        return router.route(type, payload.toString());
    }
}
