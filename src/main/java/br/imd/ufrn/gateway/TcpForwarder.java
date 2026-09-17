package br.imd.ufrn.gateway;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class TcpForwarder {

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public TcpForwarder(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String forward(ServiceInstance target, String payload) throws IOException {
        try (Socket socket = new Socket()) {
            socket.connect(
                    new java.net.InetSocketAddress(target.getHost(), target.getPort()),
                    connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.println(payload);
            String response = in.readLine();
            return response == null ? "ERROR empty response" : response;
        }
    }
}
