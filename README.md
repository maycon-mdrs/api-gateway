# api-gateway

Sistema distribuído mínimo: API Gateway + instâncias de horário BR/PT.
Protocolos HTTP, UDP e TCP ponta a ponta (mesmo protocolo da entrada até a instância).
gRPC fora do escopo atual.

## Pré-requisitos

- JDK 21+ (testado com 22)
- Maven 3.8+ (opcional; usado na EC2 / Amazon Linux)
- Lombok no Maven local (só para compilar `InstanceInfo` via `javac` manual)

Registro do ambiente AWS: [`ambiente-aws.md`](ambiente-aws.md).

## Compilar

Com Maven:

```bash
mvn -DskipTests compile
```

No Windows (script de start):

```powershell
.\scripts\start.ps1
```

Ou `javac` manualmente:

```powershell
mkdir -Force target\classes | Out-Null
$lombok = "$env:USERPROFILE\.m2\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar"
javac -encoding UTF-8 -cp $lombok -processorpath $lombok -d target\classes `
  (Get-ChildItem -Recurse src\main\java\br\imd\ufrn -Filter *.java | ForEach-Object FullName)
```

## Subir

```powershell
.\scripts\start.ps1
```

Manual (um processo = um `Main`):

```powershell
java -cp target\classes br.imd.ufrn.Main gateway
java -cp target\classes br.imd.ufrn.Main br br-1 9101
java -cp target\classes br.imd.ufrn.Main br br-2 9102
java -cp target\classes br.imd.ufrn.Main pt pt-1 9201
java -cp target\classes br.imd.ufrn.Main pt pt-2 9202
```

## Portas

| Porta | Papel |
|------:|-------|
| 9000 | REGISTER / HEARTBEAT |
| 8080 | Cliente HTTP (`GET /time/br`, `/time/pt`, `/registry`) |
| 9090 | Cliente UDP (`TIME br\|pt`) |
| 9091 | Cliente TCP (`TIME br\|pt`) |
| 91xx / 92xx | Instâncias BR / PT |

## Testar

```powershell
.\scripts\client-tcp.ps1 br
.\scripts\client-udp.ps1 br
curl http://127.0.0.1:8080/time/br
```

Demo de falha: `scripts/kill-demo.md`

Carga JMeter: `jmeter/carga-completa.jmx` ou `carga-tcp` / `carga-http` / `carga-udp` — ver [`jmeter/README.md`](jmeter/README.md).
