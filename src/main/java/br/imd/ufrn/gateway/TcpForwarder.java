package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

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

    public String forward(InstanceInfo target, String line) throws IOException {
        String where = target.getHost() + ":" + target.getPort();
        try (Socket socket = new Socket()) {
            try {
                socket.connect(new java.net.InetSocketAddress(target.getHost(), target.getPort()), connectTimeoutMillis);
            } catch (IOException e) {
                throw new IOException("TCP handshake falhou com " + where + " (timeout=" + connectTimeoutMillis + "ms)", e);
            }
            socket.setSoTimeout(readTimeoutMillis);

            PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            out.println(line);
            String response;
            try {
                response = in.readLine();
            } catch (IOException e) {
                throw new IOException("TCP leitura falhou em " + where + " (timeout=" + readTimeoutMillis + "ms)", e);
            }
            return response == null ? "ERROR empty response" : response;
        }
    }
}
