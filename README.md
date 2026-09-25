# api-gateway

Sistema distribuído mínimo: API Gateway + instâncias de horário BR/PT.
Protocolos HTTP, UDP e TCP ponta a ponta (mesmo protocolo da entrada até a instância).
gRPC fora do escopo atual.

## Pré-requisitos

- JDK 21+
- Maven 3.8+

## Como rodar

Compile e suba dois processos: o gateway e a ferramenta da porta 8081. As quatro instâncias entram pelos `POST` da seção seguinte, não por um `java` para cada uma.

```powershell
mvn clean install
java -cp target\classes br.imd.ufrn.Main gateway
java -cp "target\classes;target\lib\*" br.imd.ufrn.ops.InstanceManagerServer 8081
```

No Linux o separador do classpath é `:`.

```bash
mvn clean install
java -cp target/classes br.imd.ufrn.Main gateway
java -cp "target/classes:target/lib/*" br.imd.ufrn.ops.InstanceManagerServer 8081
```

Com os dois no ar, crie `br-1` (9101), `br-2` (9102), `pt-1` (9201) e `pt-2` (9202) com os `POST` abaixo.

## Portas


| Porta       | Papel                                                     |
| ----------- | --------------------------------------------------------- |
| 9000        | REGISTER / HEARTBEAT                                      |
| 8080        | Cliente HTTP (`GET /time/br`, `/time/pt`, `/registry`)    |
| 8081        | Ferramenta de operação (`POST`/`DELETE`/`GET /instances`) |
| 9090        | Cliente UDP (`TIME br` e `TIME pt`)                       |
| 9091        | Cliente TCP (`TIME br` e `TIME pt`)                       |
| 91xx / 92xx | Instâncias BR / PT                                        |




## Derrubar e subir instâncias

Com o gateway e a ferramenta da seção anterior no ar, estes pedidos criam e encerram as instâncias na mesma máquina.

São quatro instâncias (`br-1`, `br-2`, `pt-1`, `pt-2`). O ideal é derrubar no máximo uma do BR e/ou uma do PT. Se as duas de uma zona caírem, o horário dessa zona deixa de responder.

Os exemplos usam `127.0.0.1` (na própria máquina, local ou EC2). De fora, troque pelo endereço público.

**Derrubar** (`DELETE` encerra pelo `logs/<id>.pid`):

```bash
curl -X DELETE "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"id\":\"br-1\"}"
curl -X DELETE "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"id\":\"br-2\"}"
curl -X DELETE "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"id\":\"pt-1\"}"
curl -X DELETE "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"id\":\"pt-2\"}"
```

**Subir** (`POST` inicia de novo o mesmo tipo, id e porta):

```bash
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"br\",\"id\":\"br-1\",\"port\":9101}"
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"br\",\"id\":\"br-2\",\"port\":9102}"
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"pt\",\"id\":\"pt-1\",\"port\":9201}"
curl -X POST "http://127.0.0.1:8081/instances" -H "Content-Type: application/json" -d "{\"type\":\"pt\",\"id\":\"pt-2\",\"port\":9202}"
```

**Listar:** `curl "http://127.0.0.1:8081/instances"`

Resposta ao subir: `OK on <id> pid <pid>`. Ao desligar: `OK off <id> pid <pid>`. Log do processo: `logs/<id>.log`.

No Postman, importe `postman/instances.postman_collection.json` (Import, depois o arquivo). A coleção **Instance Manager** traz o GET que lista, a pasta **Ligar** com o POST de cada instância e a pasta **Desligar** com o DELETE de cada uma. A variável `baseUrl` vem como `http://127.0.0.1:8081`. De fora da máquina, troque pelo endereço público na porta 8081.

## Testes de carga (JMeter)

Três planos, um por protocolo. Cada um pede horário do BR e do PT no gateway. As variáveis do plano são `gatewayHost`, `users` (8), `ramp` (5 segundos) e `duration`. O host padrão do arquivo é o da EC2.


| Arquivo                 | O que dispara                                                                              |
| ----------------------- | ------------------------------------------------------------------------------------------ |
| `jmeter/carga-tcp.jmx`  | TCP na porta 9091, linhas `TIME br` e `TIME pt`                                            |
| `jmeter/carga-http.jmx` | HTTP na porta 8080, `GET /time/br` e `GET /time/pt`                                        |
| `jmeter/carga-udp.jmx`  | UDP na porta 9090, datagramas `TIME br` e `TIME pt`. Precisa do plugin jp@gc (UDP Request) |


Os três abrem Sumário, Árvore de resultados, transações por segundo e tempo de resposta. Com a carga rodando, derrubar e subir instâncias é pelos `DELETE` e `POST` da seção anterior.

```powershell
jmeter -n -t jmeter\carga-tcp.jmx "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5"
jmeter -n -t jmeter\carga-http.jmx "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5"
jmeter -n -t jmeter\carga-udp.jmx "-JgatewayHost=127.0.0.1" "-Jusers=8" "-Jramp=5"
```

