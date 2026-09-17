package br.imd.ufrn.gateway;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Mantém a tabela de componentes ativos (padrão Heartbeat).
 * Instâncias sem heartbeat dentro do timeout são removidas.
 */
public class ServiceRegistry {

    private final Map<String, ServiceInstance> instances = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobin = new ConcurrentHashMap<>();
    private final long heartbeatTimeoutMillis;

    public ServiceRegistry(long heartbeatTimeoutMillis) {
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
    }

    public void register(ServiceInstance instance) {
        instances.put(instance.getInstanceId(), instance);
        System.out.println("[registry] REGISTER " + instance);
    }

    public void heartbeat(String instanceId) {
        ServiceInstance instance = instances.get(instanceId);
        if (instance == null) {
            System.out.println("[registry] HEARTBEAT ignorado (desconhecido): " + instanceId);
            return;
        }
        instance.touchHeartbeat();
    }

    public void unregister(String instanceId) {
        ServiceInstance removed = instances.remove(instanceId);
        if (removed != null) {
            System.out.println("[registry] UNREGISTER " + removed);
        }
    }

    public int purgeExpired() {
        List<String> expired = new ArrayList<>();
        for (ServiceInstance instance : instances.values()) {
            if (!instance.isAlive(heartbeatTimeoutMillis)) {
                expired.add(instance.getInstanceId());
            }
        }
        for (String id : expired) {
            ServiceInstance removed = instances.remove(id);
            System.out.println("[registry] EXPIRED (sem heartbeat): " + removed);
        }
        return expired.size();
    }

    public Optional<ServiceInstance> nextHealthy(String componentType) {
        List<ServiceInstance> healthy = listHealthy(componentType);
        if (healthy.isEmpty()) {
            return Optional.empty();
        }
        AtomicInteger counter = roundRobin.computeIfAbsent(componentType, key -> new AtomicInteger(0));
        int index = Math.floorMod(counter.getAndIncrement(), healthy.size());
        return Optional.of(healthy.get(index));
    }

    public List<ServiceInstance> listHealthy(String componentType) {
        List<ServiceInstance> result = new ArrayList<>();
        for (ServiceInstance instance : instances.values()) {
            if (instance.getComponentType().equalsIgnoreCase(componentType)
                    && instance.isAlive(heartbeatTimeoutMillis)) {
                result.add(instance);
            }
        }
        return result;
    }

    public List<ServiceInstance> listAll() {
        return new ArrayList<>(instances.values());
    }
}
