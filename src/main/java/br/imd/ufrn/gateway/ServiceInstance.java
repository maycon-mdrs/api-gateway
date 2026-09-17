package br.imd.ufrn.gateway;

import java.util.Objects;

public class ServiceInstance {

    private final String instanceId;
    private final String componentType;
    private final String host;
    private final int port;
    private volatile long lastHeartbeatMillis;

    public ServiceInstance(String instanceId, String componentType, String host, int port) {
        this.instanceId = Objects.requireNonNull(instanceId);
        this.componentType = Objects.requireNonNull(componentType);
        this.host = Objects.requireNonNull(host);
        this.port = port;
        this.lastHeartbeatMillis = System.currentTimeMillis();
    }

    public String getInstanceId() {
        return instanceId;
    }

    public String getComponentType() {
        return componentType;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public long getLastHeartbeatMillis() {
        return lastHeartbeatMillis;
    }

    public void touchHeartbeat() {
        this.lastHeartbeatMillis = System.currentTimeMillis();
    }

    public boolean isAlive(long timeoutMillis) {
        return System.currentTimeMillis() - lastHeartbeatMillis <= timeoutMillis;
    }

    @Override
    public String toString() {
        return instanceId + " [" + componentType + "] " + host + ":" + port
                + " lastHb=" + lastHeartbeatMillis;
    }
}
