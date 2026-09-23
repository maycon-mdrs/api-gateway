package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Encaminha "TIME br|pt" para a instancia via TCP reaproveitando conexoes por
 * destino -- sem isso, cada forward() abria/fechava um socket novo (handshake +
 * TIME_WAIT a cada requisicao), o que esgota portas/soquetes sob carga sustentada.
 */
public class TcpForwarder {

    private record PooledConnection(Socket socket, BufferedReader in, PrintWriter out) {
    }

    private final int connectTimeoutMillis;
    private final int readTimeoutMillis;
    private final ConcurrentHashMap<String, ConcurrentLinkedDeque<PooledConnection>> pools =
            new ConcurrentHashMap<>();

    public TcpForwarder(int connectTimeoutMillis, int readTimeoutMillis) {
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String forward(InstanceInfo target, String line) throws IOException {
        String key = target.getHost() + ":" + target.getPort();
        PooledConnection conn = borrow(key, target);
        try {
            String response = exchange(conn, line);
            if (response != null) {
                release(key, conn);
                return response;
            }
            // peer fechou a conexao pooled (idle timeout do outro lado) -- tenta uma vez com conexao nova
            closeQuietly(conn);
        } catch (IOException e) {
            closeQuietly(conn);
        }

        PooledConnection fresh = connect(target);
        String response = exchange(fresh, line);
        if (response == null) {
            closeQuietly(fresh);
            return "ERROR empty response";
        }
        release(key, fresh);
        return response;
    }

    private String exchange(PooledConnection conn, String line) throws IOException {
        conn.out().println(line);
        if (conn.out().checkError()) {
            throw new IOException("falha ao escrever na conexao TCP pooled");
        }
        return conn.in().readLine();
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
        PrintWriter out = new PrintWriter(socket.getOutputStream(), true, StandardCharsets.UTF_8);
        BufferedReader in = new BufferedReader(
                new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
        return new PooledConnection(socket, in, out);
    }

    private void closeQuietly(PooledConnection conn) {
        try {
            conn.socket().close();
        } catch (IOException ignored) {
            // descartando conexao quebrada mesmo
        }
    }
}
