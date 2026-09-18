package br.imd.ufrn.lab.gateway;

import br.imd.ufrn.lab.model.InstanceInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class InstanceRegistry {
    private final Map<String, InstanceInfo> instances = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> roundRobin = new ConcurrentHashMap<>();
    private final long heartbeatTimeoutMillis;

    public InstanceRegistry(long heartbeatTimeoutMillis) {
        this.heartbeatTimeoutMillis = heartbeatTimeoutMillis;
    }

    public void register(InstanceInfo instance) {
        String instanceId = instance.getInstanceId();
        boolean alreadyExisted = instances.containsKey(instanceId);

        instances.put(instanceId, instance);

        if (!alreadyExisted) {
            System.out.println("[NEW] " + instance);
        } else {
            instance.refreshLastSeen();
            System.out.println("[REGISTER again] " + instance);
        }
    }

    public void heartbeat(String instanceId) {
        InstanceInfo instance = instances.get(instanceId);
        if (instance == null) {
            System.out.println("[HEARTBEAT ignorado] desconhecido: " + instanceId);
            return;
        }
        instance.refreshLastSeen();
    }

    public void removeDeadInstances() {
        List<String> expired = new ArrayList<>();
        for (InstanceInfo instance : instances.values()) {
            if (!instance.isAlive(heartbeatTimeoutMillis)) {
                expired.add(instance.getInstanceId());
            }
        }
        for (String id : expired) {
            InstanceInfo removed = instances.remove(id);
            System.out.println("[TIMEOUT] removendo " + removed);
        }
    }

    /**
     * Round-robin entre instâncias vivas do tipo pedido.
     * <p>
     * Entrada: {@code "br"} ou {@code "pt"}.<br>
     * Saída: uma instância, ou empty se não houver ninguém vivo.
     * <p>
     * Simulação ({@code listHealthy("br")} → 2 instâncias):
     * <pre>
     * aliveOfType = [br-1, br-2]   // índices 0 e 1, tamanho = 2
     *
     * Pedido 1: getAndIncrement → 0 | 0 % 2 = 0 → br-1 | contador = 1
     * Pedido 2: getAndIncrement → 1 | 1 % 2 = 1 → br-2 | contador = 2
     * Pedido 3: getAndIncrement → 2 | 2 % 2 = 0 → br-1 | contador = 3  (voltou)
     *
     * Alterna 0,1,0,1… porque n % 2 só pode ser 0 ou 1.
     *
     * 1 instância:  [br-1] → n % 1 = 0 → sempre br-1
     * 0 instâncias: lista vazia → Optional.empty() (nem usa o contador)
     * </pre>
     */
    public Optional<InstanceInfo> nextHealthy(String serviceType) {
        List<InstanceInfo> aliveOfType = listHealthy(serviceType);
        if (aliveOfType.isEmpty()) {
            return Optional.empty();
        }

        AtomicInteger counter = roundRobin.computeIfAbsent(serviceType, type -> new AtomicInteger(0));
        int position = counter.getAndIncrement() % aliveOfType.size();
        return Optional.of(aliveOfType.get(position));
    }

    public List<InstanceInfo> listHealthy(String serviceType) {
        List<InstanceInfo> result = new ArrayList<>();
        for (InstanceInfo instance : instances.values()) {
            if (instance.getServiceType().equalsIgnoreCase(serviceType)
                    && instance.isAlive(heartbeatTimeoutMillis)) {
                result.add(instance);
            }
        }
        return result;
    }

    public List<InstanceInfo> listAllInstances() {
        return new ArrayList<>(instances.values());
    }
}
