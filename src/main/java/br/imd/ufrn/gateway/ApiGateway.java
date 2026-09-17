package br.imd.ufrn.gateway;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class ApiGateway {

    public static final int CONTROL_PORT = 7000;
    public static final int TCP_PORT = 9091;
    public static final int HTTP_PORT = 8080;
    public static final long HEARTBEAT_TIMEOUT_MS = 5_000;

    private final ServiceRegistry registry;
    private final RequestRouter router;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public ApiGateway() {
        this.registry = new ServiceRegistry(HEARTBEAT_TIMEOUT_MS);
        this.router = new RequestRouter(registry, new TcpForwarder(2_000, 5_000));
    }

    public void start() {
        System.out.println("[api-gateway] iniciando...");
        System.out.println("  controle (REGISTER/HEARTBEAT): TCP " + CONTROL_PORT);
        System.out.println("  clientes TCP:                  TCP " + TCP_PORT);
        System.out.println("  clientes HTTP:                 HTTP " + HTTP_PORT);
        System.out.println("  timeout heartbeat:             " + HEARTBEAT_TIMEOUT_MS + " ms");

        cleaner.scheduleAtFixedRate(registry::purgeExpired, 1, 1, TimeUnit.SECONDS);

        Thread control = new Thread(new ControlServer(CONTROL_PORT, registry), "control-server");
        Thread tcp = new Thread(new TcpGatewayServer(TCP_PORT, router), "tcp-gateway");
        Thread http = new Thread(new HttpGatewayServer(HTTP_PORT, router, registry), "http-gateway");

        control.setDaemon(false);
        tcp.setDaemon(false);
        http.setDaemon(false);

        control.start();
        tcp.start();
        http.start();
    }
}
