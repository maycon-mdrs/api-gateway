package br.imd.ufrn.lab.model;

import lombok.Getter;

import java.util.Objects;

/**
 * Ficha de uma instância conhecida pelo gateway.
 */
@Getter
public class InstanceInfo {
    private final String instanceId;
    private final String serviceType;
    private final String host;
    private final int port;

    /** Momento do último REGISTER ou HEARTBEAT (ms). */
    private volatile long lastSeenMillis;

    public InstanceInfo(String instanceId, String serviceType, String host, int port) {
        this.instanceId = Objects.requireNonNull(instanceId);
        this.serviceType = Objects.requireNonNull(serviceType);
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.lastSeenMillis = System.currentTimeMillis();
    }

    public void refreshLastSeen() {
        this.lastSeenMillis = System.currentTimeMillis();
    }

    public boolean isAlive(long timeoutMillis) {
        return System.currentTimeMillis() - lastSeenMillis <= timeoutMillis;
    }

    @Override
    public String toString() {
        return instanceId + " [" + serviceType + "] " + host + ":" + port;
    }
}
