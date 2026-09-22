package br.imd.ufrn.gateway;

import br.imd.ufrn.model.InstanceInfo;

import java.io.IOException;
import java.util.Optional;

public class TimeRequestHandler {

    private final InstanceRegistry registry;
    private final TcpForwarder tcpForwarder;
    private final UdpForwarder udpForwarder;
    private final HttpForwarder httpForwarder;

    public TimeRequestHandler(
            InstanceRegistry registry,
            TcpForwarder tcpForwarder,
            UdpForwarder udpForwarder,
            HttpForwarder httpForwarder) {
        this.registry = registry;
        this.tcpForwarder = tcpForwarder;
        this.udpForwarder = udpForwarder;
        this.httpForwarder = httpForwarder;
    }

    /**
     * @param zone     {@code "br"} ou {@code "pt"}
     * @param protocol protocolo da entrada do cliente (mesmo até a instância)
     * @return linha de resposta ({@code OK ...} ou {@code ERROR ...})
     */
    public String handleZone(String zone, TransportProtocol protocol) {
        if (zone == null) {
            return "ERROR usage: TIME br|pt";
        }
        String normalized = zone.trim().toLowerCase();
        if (!"br".equals(normalized) && !"pt".equals(normalized)) {
            return "ERROR usage: TIME br|pt";
        }

        Optional<InstanceInfo> target = registry.nextHealthy(normalized);
        if (target.isEmpty()) {
            return "ERROR no healthy instance for " + normalized;
        }

        InstanceInfo instance = target.get();
        String payload = "TIME " + normalized;
        System.out.println("[route] " + protocol + " " + payload + " -> " + instance);
        try {
            return switch (protocol) {
                case TCP -> tcpForwarder.forward(instance, payload);
                case UDP -> udpForwarder.forward(instance, payload);
                case HTTP -> httpForwarder.forward(instance, normalized);
            };
        } catch (IOException e) {
            return "ERROR forward failed: " + e.getMessage();
        }
    }

    /**
     * Parse de linha {@code TIME br|pt} (TCP/UDP).
     */
    public String handleLine(String line, TransportProtocol protocol) {
        if (line == null || line.isBlank()) {
            return "ERROR empty";
        }
        line = line.replace("\uFEFF", "").trim();
        String[] parts = line.split("\\s+");
        if (parts.length < 2 || !"TIME".equalsIgnoreCase(parts[0])) {
            return "ERROR usage: TIME br|pt";
        }
        return handleZone(parts[1], protocol);
    }
}
