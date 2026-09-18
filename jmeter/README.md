# JMeter — Lab BR/PT

## Arquivo principal: `lab-completo.jmx`

```text
Lab BR/PT — TCP + HTTP + UDP
 ├── variáveis: users, ramp, loops
 ├── 1 — TCP :9091     → Sumário TCP + Árvore TCP
 ├── 2 — HTTP :8080    → Sumário HTTP + Árvore HTTP
 ├── 3 — UDP :9090     → Sumário UDP + Árvore UDP  (jp@gc - UDP Request)
 ├── jp@gc - Transactions per Second
 └── jp@gc - Response Times Over Time
```

Roda em sequência (TCP → HTTP → UDP). Espere o teste **terminar**.

**Duração (variáveis do Plano de Teste):**

| Variável | Valor | Efeito |
|----------|------:|--------|
| `users` | 20 | threads simultâneos |
| `ramp` | 10 | sobe a carga em 10 s |
| `duration` | **60** | cada protocolo roda **60 segundos** |

Total ≈ **3 × 60 ≈ 3 minutos** (TCP + HTTP + UDP em sequência).  
Nos seus gráficos (~200 req/s), `loops=100` acabava em ~10 s por protocolo — por isso parecia rápido.

Para alongar mais: suba `duration` para `120` ou `180` no Plano de Teste.

## Gráficos ao vivo (JMeter)

| Listener | O que mostra |
|----------|----------------|
| Transactions per Second | throughput ao longo do tempo |
| Response Times Over Time | latência ao longo do tempo |

Isso é parecido com o slide, mas o eixo X é **tempo**, não **carga**.

## Gráficos Knee / Usable (slide do curso) → Excel

O slide tem eixo X = **Load** (usuários). O JMeter **não desenha** Knee/Usable sozinho: você monta com várias rodadas.

### 1. Coletar dados

1. Abra `knee-usable-template.csv` (Excel / Google Sheets)
2. No plano, mude `users`: 5 → 10 → 20 → 40 → 60 → 80
3. A cada rodada, no **Sumário** de cada protocolo anote:
   - Throughput → coluna `throughput`
   - Average → coluna `average_ms`
   - Error % → coluna `error_pct`

### 2. Montar os 2 gráficos (por protocolo)

**Gráfico 1 — Throughput vs Load**
- X = `users`
- Y = `throughput`
- Marque:
  - **Knee**: onde a curva **desvia** da subida linear (começa a achatar)
  - **Usable capacity**: próximo do **pico** de throughput (antes de cair / explodir erro)
  - Depois do usable: throughput cai ou fica instável

**Gráfico 2 — Response Time vs Load**
- X = `users`
- Y = `average_ms`
- Antes do knee: latência baixa/estável
- Depois do usable: latência sobe forte (como no slide)

Repita **3 vezes** (TCP, HTTP, UDP) — o enunciado pede por protocolo.

### 3. Como ler (igual ao slide)

```text
Load baixa ──► knee ──► usable (pico) ──► saturação
                 │              │
                 │              └─ throughput máximo útil
                 └─ começa a desviar do ideal
```

## UDP

Plugin **jp@gc - UDP Request**, porta `9090`, data `TIME br`, decoder `DirectUDPDecoder`.
