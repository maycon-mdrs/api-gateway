package br.imd.ufrn.gateway;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Processo do gateway:
 *   :9000 HeartbeatServer
 *   :8080 ClientHttpServer  → forward HTTP
 *   :9090 ClientUdpServer   → forward UDP
 *   :9091 ClientTcpServer   → forward TCP
 */
public class GatewayMain {

    public static final int HEARTBEAT_PORT = 9000;
    public static final int CLIENT_HTTP_PORT = 8080;
    public static final int CLIENT_UDP_PORT = 9090;
    public static final int CLIENT_TCP_PORT = 9091;
    public static final long HEARTBEAT_TIMEOUT_MS = 6_000;

    private final InstanceRegistry registry;
    private final TimeRequestHandler handler;
    private final ScheduledExecutorService cleaner = Executors.newSingleThreadScheduledExecutor();

    public GatewayMain() {
        this.registry = new InstanceRegistry(HEARTBEAT_TIMEOUT_MS);
        TcpForwarder tcpForwarder = new TcpForwarder(2_000, 5_000);
        UdpForwarder udpForwarder = new UdpForwarder(5_000);
        HttpForwarder httpForwarder = new HttpForwarder(2_000, 5_000);
        this.handler = new TimeRequestHandler(registry, tcpForwarder, udpForwarder, httpForwarder);
    }

    public void start() {
        System.out.println("[gateway] iniciando...");
        System.out.println("  heartbeat: TCP " + HEARTBEAT_PORT);
        System.out.println("  cliente:   HTTP " + CLIENT_HTTP_PORT + " (forward HTTP)");
        System.out.println("  cliente:   UDP " + CLIENT_UDP_PORT + " (forward UDP)");
        System.out.println("  cliente:   TCP " + CLIENT_TCP_PORT + " (forward TCP)");
        System.out.println("  timeout:   " + HEARTBEAT_TIMEOUT_MS + " ms");

        cleaner.scheduleAtFixedRate(registry::removeDeadInstances, 1, 1, TimeUnit.SECONDS);

        Thread heartbeat = new Thread(new HeartbeatServer(HEARTBEAT_PORT, registry), "heartbeat");
        Thread http = new Thread(new ClientHttpServer(CLIENT_HTTP_PORT, handler, registry), "http");
        Thread udp = new Thread(new ClientUdpServer(CLIENT_UDP_PORT, handler), "udp");
        Thread tcp = new Thread(new ClientTcpServer(CLIENT_TCP_PORT, handler), "tcp");

        heartbeat.start();
        http.start();
        udp.start();
        tcp.start();
    }

    public static void main(String[] args) {
        new GatewayMain().start();
    }
}
