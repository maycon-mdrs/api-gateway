package br.imd.ufrn.gateway;

import java.io.IOException;
import java.util.Optional;

public class RequestRouter {

    private final ServiceRegistry registry;
    private final TcpForwarder forwarder;

    public RequestRouter(ServiceRegistry registry, TcpForwarder forwarder) {
        this.registry = registry;
        this.forwarder = forwarder;
    }

    public String route(String componentType, String payload) {
        Optional<ServiceInstance> target = registry.nextHealthy(componentType);
        if (target.isEmpty()) {
            return "ERROR no healthy instance for " + componentType;
        }

        ServiceInstance instance = target.get();
        try {
            System.out.println("[router] " + componentType + " -> " + instance.getInstanceId()
                    + " (" + instance.getHost() + ":" + instance.getPort() + ")");
            return forwarder.forward(instance, payload);
        } catch (IOException e) {
            return "ERROR forward failed to " + instance.getInstanceId() + ": " + e.getMessage();
        }
    }
}
