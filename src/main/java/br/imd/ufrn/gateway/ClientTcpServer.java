package br.imd.ufrn.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ClientTcpServer implements Runnable {

    private final int port;
    private final TimeRequestHandler handler;
    private final ExecutorService pool = Executors.newVirtualThreadPerTaskExecutor();

    public ClientTcpServer(int port, TimeRequestHandler handler) {
        this.port = port;
        this.handler = handler;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("[tcp] ouvindo TCP " + port);
            while (!Thread.currentThread().isInterrupted()) {
                Socket socket = serverSocket.accept();
                pool.execute(() -> handle(socket));
            }
        } catch (IOException e) {
            System.err.println("[tcp] erro: " + e.getMessage());
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = in.readLine();
            out.println(handler.handleLine(line, TransportProtocol.TCP));
        } catch (Exception e) {
            System.err.println("[tcp] falha: " + e.getMessage());
        }
    }
}
