# Lab Fase 2 — Gateway + Heartbeat (horário BR/PT)

## Pacotes

```text
br.imd.ufrn.lab
├── model/       → InstanceInfo (dados compartilhados)
├── gateway/     → processo do Gateway
│   ├── GatewayMain
│   ├── HeartbeatServer   (:9000)
│   ├── ClientHttpServer  (:8080)
│   ├── ClientUdpServer   (:9090)
│   ├── ClientTcpServer   (:9091)
│   ├── TimeRequestHandler
│   ├── InstanceRegistry
│   └── TcpForwarder
└── instance/    → processo de cada instância
    ├── InstanceMain
    ├── HeartbeatClient
    └── InstanceTcpServer
```

## Portas

| Porta | Papel |
|------:|-------|
| 9000 | REGISTER / HEARTBEAT |
| 8080 | Cliente HTTP: `GET /time/br`, `GET /time/pt`, `GET /registry` |
| 9090 | Cliente UDP: datagram `TIME br` / `TIME pt` |
| 9091 | Cliente TCP: linha `TIME br` / `TIME pt` |
| 91xx | Instâncias Brasil |
| 92xx | Instâncias Portugal |

As instâncias continuam só em TCP. HTTP e UDP no gateway viram `TIME br|pt` e usam o mesmo round-robin + `TcpForwarder`.

## Compilar

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mkdir -Force target\lab-classes | Out-Null
javac -encoding UTF-8 -d target\lab-classes (Get-ChildItem -Recurse src\main\java\br\imd\ufrn\lab -Filter *.java | ForEach-Object FullName)
```

Ou: `.\scripts\lab-start.ps1` (compila e sobe).

## Subir

```powershell
.\scripts\lab-start.ps1
```

Manual:

```powershell
java -cp target\lab-classes br.imd.ufrn.lab.gateway.GatewayMain
java -cp target\lab-classes br.imd.ufrn.lab.instance.InstanceMain br br-1 9101
java -cp target\lab-classes br.imd.ufrn.lab.instance.InstanceMain br br-2 9102
java -cp target\lab-classes br.imd.ufrn.lab.instance.InstanceMain pt pt-1 9201
```

## Testar

```powershell
# TCP
.\scripts\lab-time.ps1 br
.\scripts\lab-time.ps1 pt

# HTTP
curl http://127.0.0.1:8080/time/br
curl http://127.0.0.1:8080/time/pt
curl http://127.0.0.1:8080/registry

# UDP
.\scripts\lab-udp.ps1 br
.\scripts\lab-udp.ps1 pt
```

Demo kill: `scripts\lab-kill-demo.md`
