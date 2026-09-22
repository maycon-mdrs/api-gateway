package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

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

public class HeartbeatServer implements Runnable {

    private final int port;
    private final InstanceRegistry registry;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public HeartbeatServer(int port, InstanceRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[heartbeat] ouvindo TCP " + port);

            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handleConnection(socket));
            }
        } catch (IOException e) {
            System.err.println("[heartbeat] erro: " + e.getMessage());
        }
    }

    private void handleConnection(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = in.readLine();
            if (line == null || line.isBlank()) {
                out.println("ERROR empty");
                return;
            }

            out.println(processCommandLine(line.trim()));
        } catch (Exception e) {
            System.err.println("[heartbeat] falha: " + e.getMessage());
        }
    }

    private String processCommandLine(String line) {
        StringTokenizer tokens = new StringTokenizer(line);
        if (!tokens.hasMoreTokens()) {
            return "ERROR empty";
        }

        String command = tokens.nextToken().toUpperCase();
        return switch (command) {
            case "REGISTER" -> handleRegister(tokens);
            case "HEARTBEAT" -> handleHeartbeat(tokens);
            case "LIST" -> handleList();
            default -> "ERROR unknown command";
        };
    }

    private String handleRegister(StringTokenizer tokens) {
        if (tokens.countTokens() < 4) {
            return "ERROR usage: REGISTER <br|pt> <host> <port> <instanceId>";
        }

        String type = tokens.nextToken().toLowerCase();
        String host = tokens.nextToken();
        int instancePort = Integer.parseInt(tokens.nextToken());
        String instanceId = tokens.nextToken();

        if (!"br".equals(type) && !"pt".equals(type)) {
            return "ERROR type must be br or pt";
        }

        registry.register(new InstanceInfo(instanceId, type, host, instancePort));
        return "OK registered";
    }

    private String handleHeartbeat(StringTokenizer tokens) {
        if (!tokens.hasMoreTokens()) {
            return "ERROR usage: HEARTBEAT <instanceId>";
        }
        registry.heartbeat(tokens.nextToken());
        return "OK";
    }

    private String handleList() {
        StringBuilder body = new StringBuilder();
        for (InstanceInfo info : registry.listAllInstances()) {
            body.append(info).append("\n");
        }
        return body.isEmpty() ? "OK (empty)" : "OK\n" + body;
    }
}
