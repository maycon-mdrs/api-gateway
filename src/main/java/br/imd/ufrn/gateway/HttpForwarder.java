package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Encaminha GET /time/{zone} para a instancia via HTTP reaproveitando conexoes
 * por destino -- sem isso, cada forward() abria/fechava um socket TCP novo
 * (handshake + TIME_WAIT a cada requisicao), esgotando soquetes sob carga sustentada.
 */
public class HttpForwarder {

    private record PooledConnection(Socket socket, BufferedReader in) {
    }

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<PooledConnection>> pools =
            new ConcurrentHashMap<>();

    public HttpForwarder(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String forward(InstanceInfo target, String zone) throws IOException {
        String path = "/time/" + zone.trim().toLowerCase();
        String key = target.getHost() + ":" + target.getPort();

        PooledConnection conn = borrow(key, target);
        try {
            return doRequest(conn, target, path, key);
        } catch (IOException e) {
            closeQuietly(conn);
            // conexao pooled pode ter sido fechada pelo peer (idle timeout) -- tenta 1x com conexao nova
            PooledConnection fresh = connect(target);
            try {
                return doRequest(fresh, target, path, key);
            } catch (IOException e2) {
                closeQuietly(fresh);
                throw e2;
            }
        }
    }

    private String doRequest(PooledConnection conn, InstanceInfo target, String path, String key)
            throws IOException {
        String request = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + target.getHost() + "\r\n"
                + "Connection: keep-alive\r\n"
                + "\r\n";

        OutputStream rawOut = conn.socket().getOutputStream();
        rawOut.write(request.getBytes(StandardCharsets.UTF_8));
        rawOut.flush();

        BufferedReader in = conn.in();
        String statusLine = in.readLine();
        if (statusLine == null || statusLine.isBlank()) {
            throw new IOException("conexao HTTP pooled fechada pelo peer");
        }

        int contentLength = -1;
        boolean serverWantsClose = false;
        String header;
        while ((header = in.readLine()) != null && !header.isEmpty()) {
            String lower = header.toLowerCase();
            if (lower.startsWith("content-length:")) {
                contentLength = Integer.parseInt(header.substring(header.indexOf(':') + 1).trim());
            } else if (lower.startsWith("connection:") && lower.contains("close")) {
                serverWantsClose = true;
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
            // sem Content-Length so da pra saber o fim lendo ate o socket fechar
            serverWantsClose = true;
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

        if (serverWantsClose) {
            closeQuietly(conn);
        } else {
            release(key, conn);
        }

        body = body.trim();
        if (!statusLine.contains(" 200 ")) {
            return body.isEmpty() ? "ERROR HTTP " + statusLine : body;
        }
        return body.isEmpty() ? "ERROR empty body" : body;
    }

    private PooledConnection borrow(String key, InstanceInfo target) throws IOException {
        ConcurrentLinkedDeque<PooledConnection> pool = pools.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>());
        PooledConnection conn = pool.poll();
        if (conn != null) {
            return conn;
        }
        return connect(target);
    }

    private void release(String key, PooledConnection conn) {
        pools.computeIfAbsent(key, k -> new ConcurrentLinkedDeque<>()).push(conn);
    }

    private PooledConnection connect(InstanceInfo target) throws IOException {
        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(target.getHost(), target.getPort()), connectTimeoutMillis);
        socket.setSoTimeout(readTimeoutMillis);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        return new PooledConnection(socket, in);
    }

    private void closeQuietly(PooledConnection conn) {
        try {
            conn.socket().close();
        } catch (IOException ignored) {
            // descartando conexao quebrada mesmo
        }
    }
}
