package br.imd.ufrn.gateway;

import br.imd.ufrn.Log;

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
            Log.error("[tcp] servidor parou na porta " + port, e);
        }
    }

    private void handle(Socket socket) {
        try (Socket s = socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(s.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(s.getOutputStream(), true, StandardCharsets.UTF_8)) {

            String line = in.readLine();
            String response = handler.handleLine(line, TransportProtocol.TCP);
            out.println(response);
        } catch (Exception e) {
            Log.error("[tcp] falha com cliente " + peer(socket), e);
        }
    }

    private static String peer(Socket socket) {
        try {
            return String.valueOf(socket.getRemoteSocketAddress());
        } catch (Exception e) {
            return "?";
        }
    }
}
