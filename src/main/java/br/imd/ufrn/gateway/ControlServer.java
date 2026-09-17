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
 * Porta de controle: REGISTER / HEARTBEAT / UNREGISTER / LIST.
 * Componentes usam esta porta para se anunciar ao gateway.
 */
public class ControlServer implements Runnable {

    private final int port;
    private final ServiceRegistry registry;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public ControlServer(int port, ServiceRegistry registry) {
        this.port = port;
        this.registry = registry;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[control] ouvindo TCP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[control] erro: " + e.getMessage());
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
            System.err.println("[control] falha na conexão: " + e.getMessage());
        }
    }

    private String process(String line) {
        StringTokenizer tokenizer = new StringTokenizer(line);
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR empty";
        }

        String command = tokenizer.nextToken().toUpperCase();
        return switch (command) {
            case "REGISTER" -> handleRegister(tokenizer);
            case "HEARTBEAT" -> handleHeartbeat(tokenizer);
            case "UNREGISTER" -> handleUnregister(tokenizer);
            case "LIST" -> handleList();
            default -> "ERROR unknown command " + command;
        };
    }

    private String handleRegister(StringTokenizer tokenizer) {
        // REGISTER <type> <host> <port> <instanceId>
        if (tokenizer.countTokens() < 4) {
            return "ERROR usage: REGISTER type host port instanceId";
        }
        String type = tokenizer.nextToken();
        String host = tokenizer.nextToken();
        int port;
        try {
            port = Integer.parseInt(tokenizer.nextToken());
        } catch (NumberFormatException e) {
            return "ERROR invalid port";
        }
        String instanceId = tokenizer.nextToken();
        registry.register(new ServiceInstance(instanceId, type, host, port));
        return "OK registered " + instanceId;
    }

    private String handleHeartbeat(StringTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR usage: HEARTBEAT instanceId";
        }
        String instanceId = tokenizer.nextToken();
        registry.heartbeat(instanceId);
        return "OK heartbeat " + instanceId;
    }

    private String handleUnregister(StringTokenizer tokenizer) {
        if (!tokenizer.hasMoreTokens()) {
            return "ERROR usage: UNREGISTER instanceId";
        }
        String instanceId = tokenizer.nextToken();
        registry.unregister(instanceId);
        return "OK unregistered " + instanceId;
    }

    private String handleList() {
        StringBuilder sb = new StringBuilder("OK");
        for (ServiceInstance instance : registry.listAll()) {
            sb.append('|').append(instance.getInstanceId())
                    .append(',').append(instance.getComponentType())
                    .append(',').append(instance.getHost())
                    .append(':').append(instance.getPort())
                    .append(',').append(instance.isAlive(5_000) ? "UP" : "DOWN");
        }
        return sb.toString();
    }
}
