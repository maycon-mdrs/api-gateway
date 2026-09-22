package br.imd.ufrn.lab.instance;

import br.imd.ufrn.lab.gateway.GatewayMain;

/**
 * Processo de UMA instância:
 *   HeartbeatClient     → gateway :9000
 *   InstanceTcpServer   → TIME (TCP) + GET /time/* (HTTP) na porta local
 *   InstanceUdpServer   → TIME (UDP) na mesma porta
 *
 * Uso: java ... InstanceMain <br|pt> <instanceId> <port> [gatewayHost] [advertiseHost]
 */
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

        System.out.println("[lab-instance] " + instanceId
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
                "lab-udp-" + instanceId);
        udp.start();

        new InstanceTcpServer(instanceId, serviceType, listenPort).run();
    }
}
