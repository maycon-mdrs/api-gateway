package br.imd.ufrn.instance;

import br.imd.ufrn.gateway.GatewayMain;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class InstanceMain {

    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("""
                    Uso: InstanceMain <br|pt> <instanceId> <port> [gatewayHost] [advertiseHost]
                    Exemplos:
                      InstanceMain br br-1 9101
                      InstanceMain pt pt-1 9201
                    """);
            return;
        }

        String serviceType = args[0].trim().toLowerCase();
        if (!"br".equals(serviceType) && !"pt".equals(serviceType)) {
            System.err.println("tipo invalido: " + serviceType);
            return;
        }

        String instanceId = args[1].trim();
        int listenPort = Integer.parseInt(args[2]);
        String gatewayHost = args.length > 3 ? args[3] : "127.0.0.1";
        String advertiseHost = args.length > 4 ? args[4] : "127.0.0.1";

        writePidFile(instanceId);

        System.out.println("[instance] " + instanceId
                + " tipo=" + serviceType
                + " porta=" + listenPort
                + " (TCP/HTTP + UDP)");

        HeartbeatClient heartbeat = new HeartbeatClient(
                gatewayHost,
                GatewayMain.HEARTBEAT_PORT,
                serviceType,
                advertiseHost,
                listenPort,
                instanceId);
        heartbeat.start();

        Thread udp = new Thread(
                new InstanceUdpServer(instanceId, serviceType, listenPort),
                "udp-" + instanceId);
        udp.start();

        new InstanceTcpServer(instanceId, serviceType, listenPort).run();
    }

    private static void writePidFile(String instanceId) {
        try {
            Path dir = Path.of("logs");
            Files.createDirectories(dir);
            Files.writeString(dir.resolve(instanceId + ".pid"),
                    String.valueOf(ProcessHandle.current().pid()));
        } catch (IOException e) {
            System.err.println("[instance] falha ao gravar pid file: " + e.getMessage());
        }
    }
}
