package br.imd.ufrn.lab.gateway;

import br.imd.ufrn.lab.model.InstanceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Entrada TCP do cliente (:9091).
 * Uma linha: TIME br | TIME pt
 */
public class ClientTcpServer implements Runnable {

    private final int port;
    private final InstanceRegistry registry;
    private final TcpForwarder forwarder;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public ClientTcpServer(int port, InstanceRegistry registry, TcpForwarder forwarder) {
        this.port = port;
        this.registry = registry;
        this.forwarder = forwarder;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[lab-client] ouvindo TCP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[lab-client] erro: " + e.getMessage());
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

            line = line.replace("\uFEFF", "").trim();
            StringTokenizer tokenizer = new StringTokenizer(line);
            if (tokenizer.countTokens() < 2) {
                out.println("ERROR usage: TIME br|pt");
                return;
            }

            String command = tokenizer.nextToken().toUpperCase();
            String zone = tokenizer.nextToken().toLowerCase();

            if (!"TIME".equals(command) || (!"br".equals(zone) && !"pt".equals(zone))) {
                out.println("ERROR usage: TIME br|pt");
                return;
            }

            Optional<InstanceInfo> target = registry.nextHealthy(zone);
            if (target.isEmpty()) {
                out.println("ERROR no healthy instance for " + zone);
                return;
            }

            InstanceInfo instance = target.get();
            String payload = "TIME " + zone;
            System.out.println("[lab-client] " + payload + " -> " + instance);
            try {
                out.println(forwarder.forward(instance, payload));
            } catch (IOException e) {
                out.println("ERROR forward failed: " + e.getMessage());
            }
        } catch (Exception e) {
            System.err.println("[lab-client] falha: " + e.getMessage());
        }
    }
}
