# api-gateway

Sistema distribuído Camarões: API Gateway com heartbeat (TCP/HTTP primeiro).

## Pré-requisitos

- JDK 21+
- Maven 3.8+

## Compilar

```bash
mvn -q -DskipTests compile
```

## Subir (3 terminais)

```bash
# 1) Gateway
java -cp target/classes br.imd.ufrn.Main gateway

# 2) Componente tables (pode subir várias instâncias)
java -cp target/classes br.imd.ufrn.Main tables 9101
java -cp target/classes br.imd.ufrn.Main tables 9102

# 3) Componente reservations
java -cp target/classes br.imd.ufrn.Main reservations 9201
```

## Portas do gateway

| Função | Porta |
|--------|-------|
| Controle (REGISTER / HEARTBEAT) | TCP 7000 |
| Clientes TCP | TCP 9091 |
| Clientes HTTP | HTTP 8080 |

## Testes rápidos

```bash
curl http://127.0.0.1:8080/registry
curl http://127.0.0.1:8080/tables
```

TCP (payload de uma linha):

```text
ROUTE tables LIST
```

## Heartbeat

1. Ao iniciar, o componente envia `REGISTER type host port instanceId` na porta 7000.
2. A cada 1s envia `HEARTBEAT instanceId`.
3. Se o gateway não receber heartbeat em 5s, remove a instância da tabela.
4. Requisições só são encaminhadas para instâncias vivas.
