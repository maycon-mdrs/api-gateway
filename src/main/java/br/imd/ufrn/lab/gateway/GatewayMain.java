package br.imd.ufrn.lab.gateway;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Processo do gateway (Fase 1):
 *   :9000 HeartbeatServer
 *   :9091 ClientTcpServer
 */
public class GatewayMain {

    public static final int HEARTBEAT_PORT = 9000;
    public static final int CLIENT_TCP_PORT = 9091;
    public static final long HEARTBEAT_TIMEOUT_MS = 6_000;

    private final InstanceRegistry registry;
    private final TcpForwarder forwarder;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public GatewayMain() {
        this.registry = new InstanceRegistry(HEARTBEAT_TIMEOUT_MS);
        this.forwarder = new TcpForwarder(2_000, 5_000);
    }

    public void start() {
        System.out.println("[lab-gateway] iniciando...");
        System.out.println("  heartbeat: TCP " + HEARTBEAT_PORT);
        System.out.println("  cliente:   TCP " + CLIENT_TCP_PORT);
        System.out.println("  timeout:   " + HEARTBEAT_TIMEOUT_MS + " ms");

        cleaner.scheduleAtFixedRate(registry::removeDeadInstances, 1, 1, TimeUnit.SECONDS);

        Thread heartbeat = new Thread(new HeartbeatServer(HEARTBEAT_PORT, registry), "lab-heartbeat");
        Thread client = new Thread(new ClientTcpServer(CLIENT_TCP_PORT, registry, forwarder), "lab-client");
        heartbeat.start();
        client.start();
    }

    public static void main(String[] args) {
        new GatewayMain().start();
    }
}
