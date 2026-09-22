package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class HttpForwarder {

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;

    public HttpForwarder(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String forward(InstanceInfo target, String zone) throws IOException {
        String path = "/time/" + zone.trim().toLowerCase();
        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(target.getHost(), target.getPort()),
                    connectTimeoutMillis);
            socket.setSoTimeout(readTimeoutMillis);

            String request = "GET " + path + " HTTP/1.0\r\n"
                    + "Host: " + target.getHost() + "\r\n"
                    + "Connection: close\r\n"
                    + "\r\n";

            OutputStream rawOut = socket.getOutputStream();
            rawOut.write(request.getBytes(StandardCharsets.UTF_8));
            rawOut.flush();

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));

            String statusLine = in.readLine();
            if (statusLine == null || statusLine.isBlank()) {
                return "ERROR empty HTTP response";
            }

            int contentLength = -1;
            String header;
            while ((header = in.readLine()) != null && !header.isEmpty()) {
                String lower = header.toLowerCase();
                if (lower.startsWith("content-length:")) {
                    contentLength = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
                }
            }

            String body;
            if (contentLength >= 0) {
                char[] buf = new char[contentLength];
                int read = 0;
                while (read < contentLength) {
                    int n = in.read(buf, read, contentLength - read);
                    if (n < 0) {
                        break;
                    }
                    read += n;
                }
                body = new String(buf, 0, read);
            } else {
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = in.readLine()) != null) {
                    if (!sb.isEmpty()) {
                        sb.append('\n');
                    }
                    sb.append(line);
                }
                body = sb.toString();
            }

            body = body.trim();
            if (!statusLine.contains(" 200 ")) {
                return body.isEmpty() ? "ERROR HTTP " + statusLine : body;
            }
            return body.isEmpty() ? "ERROR empty body" : body;
        }
    }
}
