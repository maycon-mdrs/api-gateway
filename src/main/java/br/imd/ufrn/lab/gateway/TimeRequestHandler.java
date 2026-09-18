package br.imd.ufrn.lab.gateway;

import br.imd.ufrn.lab.model.InstanceInfo;

import java.io.IOException;
import java.util.Optional;

public class TimeRequestHandler {

    private final InstanceRegistry registry;
    private final TcpForwarder forwarder;

    public TimeRequestHandler(InstanceRegistry registry, TcpForwarder forwarder) {
        this.registry = registry;
        this.forwarder = forwarder;
    }

    /**
     * @param zone {@code "br"} ou {@code "pt"}
     * @return linha de resposta ({@code OK ...} ou {@code ERROR ...})
     */
    public String handleZone(String zone) {
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
        System.out.println("[lab-route] " + payload + " -> " + instance);
        try {
            return forwarder.forward(instance, payload);
        } catch (IOException e) {
            return "ERROR forward failed: " + e.getMessage();
        }
    }

    /**
     * Parse de linha {@code TIME br|pt} (TCP/UDP).
     */
    public String handleLine(String line) {
        if (line == null || line.isBlank()) {
            return "ERROR empty";
        }
        line = line.replace("\uFEFF", "").trim();
        String[] parts = line.split("\\s+");
        if (parts.length < 2 || !"TIME".equalsIgnoreCase(parts[0])) {
            return "ERROR usage: TIME br|pt";
        }
        return handleZone(parts[1]);
    }
}
