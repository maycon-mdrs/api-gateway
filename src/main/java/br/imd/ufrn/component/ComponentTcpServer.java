package br.imd.ufrn.component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

public class ComponentTcpServer implements Runnable {

    private final int port;
    private final String name;
    private final Function<String, String> handler;
    private final ExecutorService pool = Executors.newCachedThreadPool();

    public ComponentTcpServer(int port, String name, Function<String, String> handler) {
        this.port = port;
        this.name = name;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[" + name + "] ouvindo TCP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.submit(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[" + name + "] erro: " + e.getMessage());
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
            out.println(handler.apply(line.trim()));
        } catch (IOException e) {
            System.err.println("[" + name + "] falha: " + e.getMessage());
        }
    }
}
