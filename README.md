# api-gateway

Sistema distribuído mínimo: API Gateway + instâncias de horário BR/PT.
Protocolos HTTP, UDP e TCP ponta a ponta (mesmo protocolo da entrada até a instância).
gRPC fora do escopo atual.

## Pré-requisitos

- JDK 21+ (testado com 22)
- Maven 3.8+ (opcional; usado na EC2 / Amazon Linux)
- Lombok no Maven local (só para compilar `InstanceInfo` via `javac` manual)

## Como rodar

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
$json = "$env:USERPROFILE\.m2\repository\org\json\json\20250517\json-20250517.jar"
javac -encoding UTF-8 -cp "$lombok;$json" -processorpath $lombok -d target\classes `
  (Get-ChildItem -Recurse src\main\java\br\imd\ufrn -Filter *.java | ForEach-Object FullName)
```

## Subir

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
| 8081 | Ferramenta de operação (`POST`/`DELETE`/`GET /instances`) |
| 9090 | Cliente UDP (`TIME br\|pt`) |
| 9091 | Cliente TCP (`TIME br\|pt`) |
| 91xx / 92xx | Instâncias BR / PT |

## Ferramenta de operação (subir/derrubar instâncias)

Auxiliar, fora do API Gateway e dos protocolos avaliados. Usa o `HttpServer` do JDK só para ligar e desligar os processos das instâncias na mesma máquina (útil na avaliação de tolerância a falhas). O JSON entra pela biblioteca `org.json`; o `mvn compile` copia o jar para `target/lib`.

```bash
java -cp "target/classes:target/lib/*" br.imd.ufrn.ops.InstanceManagerServer 8081
```

```bash
curl -X POST "http://<IP>:8081/instances" \
  -H "Content-Type: application/json" \
  -d '{"type":"pt","id":"pt-1","port":9201}'

curl -X DELETE "http://<IP>:8081/instances" \
  -H "Content-Type: application/json" \
  -d '{"id":"pt-1"}'

curl "http://<IP>:8081/instances"
```

`POST` sobe `java ... br.imd.ufrn.Main` com o tipo, id e porta do JSON (`gatewayHost` e `advertiseHost` são opcionais, padrão `127.0.0.1`). `DELETE` encerra o processo lendo `logs/<id>.pid`. Cada instância também grava esse arquivo ao iniciar, então um worker subido manualmente com `nohup` também pode ser derrubado por aqui. Log do processo: `logs/<id>.log`.
