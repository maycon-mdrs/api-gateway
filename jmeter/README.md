# JMeter — API Gateway BR/PT (avaliação)

Scripts de carga para o API Gateway (HTTP `:8080`, TCP `:9091`, UDP `:9090`).

## Planos

| Arquivo | Uso |
|---------|-----|
| [`carga-completa.jmx`](carga-completa.jmx) | TCP → HTTP → UDP em sequência (avaliação / Sigaa) |
| [`carga-tcp.jmx`](carga-tcp.jmx) | **Só TCP** — prints e knee isolados |
| [`carga-http.jmx`](carga-http.jmx) | **Só HTTP** |
| [`carga-udp.jmx`](carga-udp.jmx) | **Só UDP** (plugin jp@gc; stress alto ok) |

### `carga-completa.jmx`

```text
API Gateway BR/PT — TCP + HTTP + UDP
 ├── variáveis: gatewayHost, users, ramp, duration
 ├── 1 — TCP :9091     → Sumário TCP + Árvore TCP
 ├── 2 — HTTP :8080    → Sumário HTTP + Árvore HTTP
 ├── 3 — UDP :9090     → Sumário UDP + Árvore UDP
 ├── jp@gc - Transactions per Second
 └── jp@gc - Response Times Over Time
```

Roda em sequência. **Use o mesmo `users` nos três** — para demo sem erro: **`users=8`**. Não use 200 no completo (TCP/HTTP saturam).

### Planos isolados (`carga-tcp` / `carga-http` / `carga-udp`)

Mesmas variáveis e listeners (Sumário, Árvore, TPS, Response Times). Ideal para prints por protocolo sem um afetar o outro.

## Pré-requisitos

- JMeter 5.6+ (ex.: `C:\apache-jmeter-5.6.3`)
- Plugin **jp@gc - UDP Request** (só para `carga-udp` / `carga-completa`)
- App no ar na EC2 — ver [`../ambiente-aws.md`](../ambiente-aws.md)
- Security Group: **8080/TCP**, **9091/TCP**, **9090/UDP** (+ SSH)

## Variáveis

| Nome | Default | Efeito |
|------|--------:|--------|
| `gatewayHost` | IP público EC2 | host do gateway |
| `users` | 8 | threads (**>5** e &lt; knee; usable) |
| `ramp` | 5 | segundos para subir os users |
| `duration` | 20 | segundos de carga (demo: 60) |

CLI: `-J` via `__P`:

```powershell
jmeter -n -t carga-tcp.jmx "-JgatewayHost=IP" "-Jusers=8" "-Jramp=5" "-Jduration=20"
```

## Como executar (GUI) — prints

1. Abrir o `.jmx` do protocolo (ou o completo).
2. Conferir `gatewayHost` (IP público atual).
3. **Saudável:** `users=8`, `duration=60` → Error % ≈ 0.
4. **Erro TCP/HTTP:** `users=15` (ou 20) no `carga-tcp` / `carga-http`.
5. **UDP stress:** `carga-udp.jmx` com `users=200` ainda Error ≈ 0–0,3%.
6. Print do Sumário (Error %, Vazão, KB/s, Sent KB/sec) + TPS se quiser.

| users | TCP/HTTP Error % (remoto) | Uso |
|------:|--------------------------:|-----|
| **8** | ≈ **0** | demo / enunciado |
| 10 | &lt; 1% | limite fino |
| **15** | **&gt; 1%** | erro começa |
| 20+ | sobe até ~25% em 80 | saturação |

## CLI (exemplos)

```powershell
cd C:\Users\mayco\OneDrive\Documentos\UF\api-gateway\jmeter

# demo completo (avaliação)
jmeter -n -t carga-completa.jmx `
  "-JgatewayHost=18.219.12.66" `
  "-Jusers=8" "-Jramp=8" "-Jduration=60" `
  -l results-demo-u8.jtl

# só TCP com erro
jmeter -n -t carga-tcp.jmx `
  "-JgatewayHost=18.219.12.66" `
  "-Jusers=15" "-Jramp=5" "-Jduration=20" `
  -l results-tcp-u15.jtl

# só UDP stress
jmeter -n -t carga-udp.jmx `
  "-JgatewayHost=18.219.12.66" `
  "-Jusers=200" "-Jramp=5" "-Jduration=20" `
  -l results-udp-u200.jtl
```

## Demo de tolerância a falhas

Com `carga-completa` ou um isolado em `users=8`:

1. Sumário com Error ≈ 0.
2. Na EC2: `pkill -f "br-1 9101"` → erro sobe.
3. `nohup java -cp target/classes br.imd.ufrn.Main br br-1 9101 > br-1.log 2>&1 &`
4. Erros caem após REGISTER/heartbeat.

## Knee / Usable

Ver [`knee-usable-template.csv`](knee-usable-template.csv) e `docs/figuras/knee-*.png`.

| | TCP / HTTP | UDP |
|--|------------|-----|
| **Knee** | ≈ **5** (platô ~22 req/s) | ≈ **50–60** (soft) |
| **Usable (demo)** | **8** | **8** |
| Degradação | Error>1% ≈**15** | Error baixo até 200 |

## UDP

Plugin **jp@gc - UDP Request**, porta `9090`, data `TIME br`, decoder `DirectUDPDecoder`.
