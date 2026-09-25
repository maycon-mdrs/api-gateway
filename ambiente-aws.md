# Ambiente AWS — registro

Documentação do que foi feito na EC2 para o api-gateway (1 máquina).

---

## Ambiente


| Campo          | Valor                                                   |
| -------------- | ------------------------------------------------------- |
| Instância      | UNIDADE1 (`i-0dfac3592a81dc46e`)                        |
| Tipo           | `t3.micro`                                              |
| Região         | `us-east-2`                                             |
| SO             | Amazon Linux 2023                                       |
| Usuário SSH    | `ec2-user`                                              |
| IP privado     | `172.31.39.59`                                          |
| IP público     | Elastic IP `3.146.239.232` (permanece ao parar/iniciar) |
| Chave          | `my-key.pem` (raiz do projeto, ignorada no Git)         |
| Security Group | `launch-wizard-1` (`sg-0e4e6ed231cc5eab6`)              |
| Topologia      | 1 EC2: gateway + workers no mesmo host (`127.0.0.1`)    |


---



## O que já foi feito



### 1. Permissões da chave (PowerShell, no PC)

```powershell
$k = "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\my-key.pem"
icacls.exe $k /reset
icacls.exe $k /GRANT:R "$($env:USERNAME):R"
icacls.exe $k /inheritance:r
```



### 2. Conexão SSH (PowerShell, no PC)

```powershell
ssh -i $k ec2-user@<IP_PUBLICO>
```



### 3. Atualização, Java 21 e Maven (bash, na VM)

```bash
sudo dnf update -y
sudo dnf install -y java-21-amazon-corretto-devel
sudo dnf install -y maven

java -version
javac -version
which java
mvn -v
```

Resultado (Java):

```
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Corretto-21.0.12.9.1 (build 21.0.12.1+9-LTS)
javac 21.0.12.1
/usr/bin/java
```

Maven instalado: `maven` **3.8.4** (`amazonlinux`).

### 4. Clonar o código (bash, na VM)

```bash
git clone https://github.com/maycon-mdrs/api-gateway.git
cd api-gateway/
git switch develop
git pull
```

Diretório de trabalho: `~/api-gateway` (branch `develop`).

### 5. JAVA_HOME 21 + compilar (bash, na VM)

O Maven do Amazon Linux vinha amarrado ao Java 17; foi preciso apontar para o 21:

```bash
export JAVA_HOME=/usr/lib/jvm/java-21-amazon-corretto
export PATH="$JAVA_HOME/bin:$PATH"

java -version
mvn -version
```

`mvn -version` → Java **21.0.12.1**.

```bash
cd ~/api-gateway
mvn clean install
```

`BUILD SUCCESS` → classes em `target/classes`.

### 6. Subir gateway + workers (bash, na VM)

Antes de subir de novo, encerre o que já estiver no ar. Processos antigos seguram as portas e a subida nova fica pela metade (dois gateways, workers que morrem na hora).

```bash
pkill -f br.imd.ufrn
sleep 2
ps aux | grep br.imd.ufrn | grep -v grep
```

A segunda linha tem que voltar vazia. `pkill -f br.imd.ufrn` pega os cinco `Main` e o `InstanceManagerServer`.

```bash
cd ~/api-gateway

nohup java -cp target/classes br.imd.ufrn.Main gateway > gateway.log 2>&1 &
sleep 2
nohup java -cp target/classes br.imd.ufrn.Main br br-1 9101 > br-1.log 2>&1 &
nohup java -cp target/classes br.imd.ufrn.Main br br-2 9102 > br-2.log 2>&1 &
nohup java -cp target/classes br.imd.ufrn.Main pt pt-1 9201 > pt-1.log 2>&1 &
nohup java -cp target/classes br.imd.ufrn.Main pt pt-2 9202 > pt-2.log 2>&1 &
nohup java -cp "target/classes:target/lib/*" br.imd.ufrn.ops.InstanceManagerServer 8081 > manager.log 2>&1 &
```

Conferência dos 6 processos (`grep br.imd.ufrn.Main` não mostra o manager):

```bash
ps aux | grep br.imd.ufrn | grep -v grep
tail -n 20 gateway.log
```

Devem aparecer exatamente estas seis linhas, todas com horário de agora:

| Processo | Comando |
| --- | --- |
| gateway | `br.imd.ufrn.Main gateway` |
| br-1 | `br.imd.ufrn.Main br br-1 9101` |
| br-2 | `br.imd.ufrn.Main br br-2 9102` |
| pt-1 | `br.imd.ufrn.Main pt pt-1 9201` |
| pt-2 | `br.imd.ufrn.Main pt pt-2 9202` |
| manager | `br.imd.ufrn.ops.InstanceManagerServer 8081` |

Gateway ouvindo 8080/9090/9091/9000; registry com `br-1`, `br-2`, `pt-1`, `pt-2` em `127.0.0.1`.
A ferramenta de operação escuta em `8081` (`manager.log`).

Durante o teste de carga, derrubar uma instância (a taxa de erro deve subir) e recriá-la (a taxa deve cair):

```bash
curl -X DELETE "http://127.0.0.1:8081/instances" \
  -H "Content-Type: application/json" \
  -d '{"id":"pt-1"}'

curl -X POST "http://127.0.0.1:8081/instances" \
  -H "Content-Type: application/json" \
  -d '{"type":"pt","id":"pt-1","port":9201}'
```

`DELETE` encerra o processo pelo `logs/<id>.pid`. `POST` sobe de novo o mesmo tipo, id e porta.

### 7. Security Group

SG `launch-wizard-1`: além de SSH (22), liberar acesso à aplicação (ex. **Todo o tráfego** ou portas 8080/TCP, 9091/TCP, 9090/UDP) com origem adequada.

A porta `8081/TCP` só é necessária se a ferramenta de operação for chamada de fora da VM. Os `curl` acima rodam na própria EC2 e não precisam dessa regra.