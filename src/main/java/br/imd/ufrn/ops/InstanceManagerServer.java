package br.imd.ufrn.ops;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;

/**
 * Ferramenta auxiliar, fora do API Gateway e dos protocolos avaliados.
 * Sobe e derruba processos {@code br.imd.ufrn.Main} na mesma máquina.
 *
 * <pre>
 *   POST   /instances   {"type":"br","id":"br-1","port":9101}
 *   DELETE /instances   {"id":"br-1"}
 *   GET    /instances
 * </pre>
 */
public class InstanceManagerServer {

    private static final int DEFAULT_PORT = 8081;
    private static final Path LOGS_DIR = Path.of("logs");
    private static final String CLASSPATH = "target/classes";
    private static final String MAIN_CLASS = "br.imd.ufrn.Main";

    public static void main(String[] args) throws IOException {
        int port = DEFAULT_PORT;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        }

        HttpServer server = HttpServer.create(new InetSocketAddress(port), 0);
        server.createContext("/instances", InstanceManagerServer::handle);
        server.setExecutor(Executors.newVirtualThreadPerTaskExecutor());
        server.start();
        System.out.println("[ops] ouvindo HTTP " + port + " (POST/DELETE/GET /instances)");
    }

    private static void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod().toUpperCase();
            String path = exchange.getRequestURI().getPath();
            if (!"/instances".equals(path)) {
                send(exchange, 404, "Not Found\n");
                return;
            }

            switch (method) {
                case "POST" -> startInstance(exchange);
                case "DELETE" -> stopInstance(exchange);
                case "GET" -> listInstances(exchange);
                default -> send(exchange, 405, "Method Not Allowed\n");
            }
        } catch (Exception e) {
            System.err.println("[ops] falha: " + e.getMessage());
            send(exchange, 500, "ERROR " + e.getMessage() + "\n");
        }
    }

    private static void startInstance(HttpExchange exchange) throws IOException {
        JSONObject body = readJson(exchange);
        if (body == null) {
            send(exchange, 400, "ERROR json invalido\n");
            return;
        }

        String type = body.optString("type", "").trim().toLowerCase();
        String id = body.optString("id", "").trim();
        String gatewayHost = body.optString("gatewayHost", "127.0.0.1").trim();
        String advertiseHost = body.optString("advertiseHost", "127.0.0.1").trim();
        int port = body.optInt("port", -1);

        if (!"br".equals(type) && !"pt".equals(type)) {
            send(exchange, 400, "ERROR type deve ser br ou pt\n");
            return;
        }
        if (!isSafeId(id)) {
            send(exchange, 400, "ERROR id invalido\n");
            return;
        }
        if (port < 1 || port > 65535) {
            send(exchange, 400, "ERROR port invalida\n");
            return;
        }
        if (!isSafeHost(gatewayHost) || !isSafeHost(advertiseHost)) {
            send(exchange, 400, "ERROR host invalido\n");
            return;
        }

        Optional<Long> alivePid = readAlivePid(id);
        if (alivePid.isPresent()) {
            send(exchange, 409, "ERROR " + id + " ja esta viva pid " + alivePid.get() + "\n");
            return;
        }

        Files.createDirectories(LOGS_DIR);
        Path logFile = LOGS_DIR.resolve(id + ".log");

        ProcessBuilder builder = new ProcessBuilder(
                javaBin(),
                "-cp", CLASSPATH,
                MAIN_CLASS,
                type,
                id,
                Integer.toString(port),
                gatewayHost,
                advertiseHost);
        builder.directory(Path.of("").toAbsolutePath().toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(ProcessBuilder.Redirect.appendTo(logFile.toFile()));

        Process process = builder.start();
        long pid = process.pid();
        Files.writeString(pidFile(id), Long.toString(pid));

        System.out.println("[ops] on " + id + " tipo=" + type + " porta=" + port + " pid=" + pid);
        send(exchange, 200, "OK on " + id + " pid " + pid + "\n");
    }

    private static void stopInstance(HttpExchange exchange) throws IOException, InterruptedException {
        JSONObject body = readJson(exchange);
        if (body == null) {
            send(exchange, 400, "ERROR json invalido\n");
            return;
        }
        String id = body.optString("id", "").trim();
        if (!isSafeId(id)) {
            send(exchange, 400, "ERROR id invalido\n");
            return;
        }

        Path file = pidFile(id);
        if (!Files.isRegularFile(file)) {
            send(exchange, 404, "ERROR " + id + " sem pid file\n");
            return;
        }

        long pid;
        try {
            pid = Long.parseLong(Files.readString(file).trim());
        } catch (NumberFormatException e) {
            Files.deleteIfExists(file);
            send(exchange, 404, "ERROR " + id + " pid file invalido\n");
            return;
        }

        Optional<ProcessHandle> handle = ProcessHandle.of(pid).filter(ProcessHandle::isAlive);
        if (handle.isEmpty()) {
            Files.deleteIfExists(file);
            send(exchange, 404, "ERROR " + id + " nao esta viva\n");
            return;
        }

        ProcessHandle process = handle.get();
        process.destroy();
        try {
            process.onExit().get(2, TimeUnit.SECONDS);
        } catch (Exception e) {
            process.destroyForcibly();
            try {
                process.onExit().get(2, TimeUnit.SECONDS);
            } catch (Exception ignored) {
                send(exchange, 500, "ERROR " + id + " nao encerrou pid " + pid + "\n");
                return;
            }
        }

        Files.deleteIfExists(file);
        System.out.println("[ops] off " + id + " pid=" + pid);
        send(exchange, 200, "OK off " + id + " pid " + pid + "\n");
    }

    private static void listInstances(HttpExchange exchange) throws IOException {
        if (!Files.isDirectory(LOGS_DIR)) {
            send(exchange, 200, "(nenhuma instancia)\n");
            return;
        }

        StringBuilder body = new StringBuilder();
        try (Stream<Path> files = Files.list(LOGS_DIR)) {
            files.filter(path -> path.getFileName().toString().endsWith(".pid"))
                    .sorted()
                    .forEach(path -> appendInstanceLine(body, path));
        }

        if (body.isEmpty()) {
            body.append("(nenhuma instancia)\n");
        }
        send(exchange, 200, body.toString());
    }

    private static void appendInstanceLine(StringBuilder body, Path pidPath) {
        String fileName = pidPath.getFileName().toString();
        String id = fileName.substring(0, fileName.length() - ".pid".length());
        String pidText;
        try {
            pidText = Files.readString(pidPath).trim();
        } catch (IOException e) {
            body.append(id).append(" pid=? alive=false\n");
            return;
        }

        boolean alive = false;
        try {
            long pid = Long.parseLong(pidText);
            alive = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
        } catch (NumberFormatException ignored) {
            pidText = "?";
        }
        body.append(id).append(" pid=").append(pidText).append(" alive=").append(alive).append('\n');
    }

    private static Optional<Long> readAlivePid(String id) {
        Path file = pidFile(id);
        if (!Files.isRegularFile(file)) {
            return Optional.empty();
        }
        try {
            long pid = Long.parseLong(Files.readString(file).trim());
            boolean alive = ProcessHandle.of(pid).map(ProcessHandle::isAlive).orElse(false);
            return alive ? Optional.of(pid) : Optional.empty();
        } catch (IOException | NumberFormatException e) {
            return Optional.empty();
        }
    }

    private static Path pidFile(String id) {
        return LOGS_DIR.resolve(id + ".pid");
    }

    private static String javaBin() {
        String executable = System.getProperty("os.name", "").toLowerCase().contains("win")
                ? "java.exe"
                : "java";
        return Path.of(System.getProperty("java.home"), "bin", executable).toString();
    }

    private static boolean isSafeId(String id) {
        return id != null && id.matches("[A-Za-z0-9._-]{1,64}");
    }

    private static boolean isSafeHost(String host) {
        return host != null && host.matches("[A-Za-z0-9._:-]{1,253}");
    }

    private static String readBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    private static JSONObject readJson(HttpExchange exchange) throws IOException {
        try {
            return new JSONObject(readBody(exchange));
        } catch (JSONException e) {
            return null;
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
        exchange.sendResponseHeaders(status, bytes.length);
        try (OutputStream out = exchange.getResponseBody()) {
            out.write(bytes);
        }
    }
}
