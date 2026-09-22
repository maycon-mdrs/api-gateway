# Ambiente AWS — registro

Documentação do que foi feito na EC2 para o lab do api-gateway (1 máquina).

---

## Ambiente

| Campo       | Valor                                                   |
| ----------- | ------------------------------------------------------- |
| Instância   | UNIDADE1 (`i-0dfac3592a81dc46e`)                        |
| Tipo        | `t3.micro`                                              |
| Região      | `us-east-2`                                             |
| SO          | Amazon Linux 2023                                       |
| Usuário SSH | `ec2-user`                                              |
| IP privado  | `172.31.39.59`                                          |
| IP público  | *(anotar no console; muda se reiniciar sem Elastic IP)* |
| Chave       | `my-key.pem` (raiz do projeto, ignorada no Git)         |
| Topologia   | 1 EC2: gateway + workers no mesmo host (`127.0.0.1`)    |

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

### 3. Atualização e Java 21 (bash, na VM)

```bash
sudo dnf update -y
sudo dnf install -y java-21-amazon-corretto-devel

java -version
javac -version
which java
```

Resultado:

```
openjdk version "21.0.12.1" 2026-08-18 LTS
OpenJDK Runtime Environment Corretto-21.0.12.9.1 (build 21.0.12.1+9-LTS)
javac 21.0.12.1
/usr/bin/java
```

### 4. Clonar o código (bash, na VM)

```bash
git clone https://github.com/maycon-mdrs/api-gateway.git
cd api-gateway/
```

Diretório de trabalho atual: `~/api-gateway`

---

## O que ainda falta

1. **Compilar** em `~/api-gateway` (gerar `target/classes` — Maven/Lombok ou `javac` como no README; ou enviar classes compiladas do PC).
2. **Security Group** — liberar entrada: 8080/TCP, 9090/UDP, 9091/TCP (e 50051 se houver gRPC); 22 já para SSH.
3. **Subir processos** (vários terminais/`tmux`):

   ```bash
   java -cp target/classes br.imd.ufrn.Main gateway
   java -cp target/classes br.imd.ufrn.Main br br-1 9101
   java -cp target/classes br.imd.ufrn.Main br br-2 9102
   java -cp target/classes br.imd.ufrn.Main pt pt-1 9201
   ```

4. **Testar do PC** — `curl` no IP público (`/time/br`, `/registry`) e depois JMeter.
5. **Relatório** — preencher seção 4 (IPs, SG, comandos) com estes dados.

---

*Última atualização: clone em `~/api-gateway` (terminal 46).*
