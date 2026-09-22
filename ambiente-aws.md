# Ambiente AWS — API Gateway (1× EC2)

Roteiro prático para subir o lab numa única instância EC2.  
Atualizar este arquivo conforme for avançando.

---

## Decisão de arquitetura

- **1 EC2** roda gateway + workers BR/PT no mesmo host.
- Comunicação interna: `127.0.0.1` (não precisa `gatewayHost` / `advertiseHost`).
- JMeter / `curl` do PC apontam para o **IP público** do gateway.
- Demo de falha: matar/subir processos Java na mesma VM.

---

## Dados da instância


| Campo       | Valor                                                                    |
| ----------- | ------------------------------------------------------------------------ |
| Nome        | Aula                                                                     |
| Instance ID | `i-0dfac3592a81dc46e`                                                    |
| Tipo        | `t3.micro`                                                               |
| Região      | `us-east-2` (Ohio)                                                       |
| SO          | Amazon Linux 2023                                                        |
| Usuário SSH | `ec2-user`                                                               |
| IP privado  | `172.31.39.59`                                                           |
| IP público  | *(anotar no console após iniciar; muda se parar/iniciar sem Elastic IP)* |
| Key pair    | `my-key.pem` (na raiz do projeto; já no `.gitignore`)                    |


Variável sugestão no PowerShell (preencher com o IP do console):

```powershell
$ip = "IPV4_PUBLICO_AQUI"
$k  = "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\my-key.pem"
```

> **Atenção:** ao **interromper** a EC2, o IP público some. Ao **iniciar** de novo, anote o IP novo (ou associe um Elastic IP).

---



## Passo 1 — Ligar a instância

No console EC2 (`us-east-2`):

1. Selecionar a instância **Aula**.
2. **Estado da instância** → **Iniciar instância**.
3. Esperar **Em execução**.
4. Anotar o **Endereço IPv4 público** em `$ip`.

**Status:** feito (instância em execução; IP público anotado na sessão).

---



## Passo 2 — Security Group

Na aba **Segurança** da instância, liberar entrada:


| Porta | Protocolo | Origem                             | Uso                   |
| ----- | --------- | ---------------------------------- | --------------------- |
| 22    | TCP       | seu IP (ou temporário `0.0.0.0/0`) | SSH                   |
| 8080  | TCP       | `0.0.0.0/0` ou IP do professor     | HTTP cliente          |
| 9090  | **UDP**   | idem                               | UDP cliente           |
| 9091  | TCP       | idem                               | TCP cliente           |
| 50051 | TCP       | idem                               | gRPC (quando existir) |


Portas internas (`9000`, `9101`, `9102`, `9201`) **não** precisam estar abertas para a internet (tudo em localhost).

**Status:** conferir no console se 8080 / 9090(UDP) / 9091 estão liberadas antes dos testes externos.

---



## Passo 3 — Permissões da chave no Windows

No PowerShell (comando da aula, com o caminho da chave do projeto):

```powershell
$k = "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\my-key.pem"
icacls.exe $k /reset
icacls.exe $k /GRANT:R "$($env:USERNAME):R"
icacls.exe $k /inheritance:r
```

Saída observada: *Processados com sucesso 1 arquivos* em cada comando.

**Status:** feito.

---



## Passo 4 — SSH

```powershell
ssh -i $k ec2-user@$ip
```

O que aconteceu nesta sessão:

1. Prompt de autenticidade do host (primeira conexão) → respondido `yes`.
2. Fingerprint ED25519 registrado em `known_hosts`.
3. Banner **Amazon Linux 2023**.
4. Login ok: prompt `[ec2-user@ip-172-31-39-59 ~]$`
5. Usuário correto: `ec2-user` (não `ubuntu`).
6. A sessão caiu uma vez (`client_loop: send disconnect: Connection reset`) → reconectar com o mesmo `ssh -i $k ec2-user@$ip` resolveu.

**Status:** feito.

---



## Passo 5 — Instalar Java 21 na EC2

Dentro do SSH (Amazon Linux usa `dnf`, não `dfn`):

```bash
# opcional — nesta sessão: "Nothing to do"
sudo dnf update -y

sudo dnf install -y java-21-amazon-corretto-devel
java -version
```

Instalado com sucesso:


| Pacote                             | Versão               |
| ---------------------------------- | -------------------- |
| `java-21-amazon-corretto-devel`    | `21.0.12+9` (LTS)    |
| `java-21-amazon-corretto-headless` | dependência (~96 MB) |


O aviso de *newer release of Amazon Linux* (`dnf upgrade --releasever=…`) **não** é obrigatório para o lab; pode ignorar por enquanto.

Confirmar na VM:

```bash
java -version
javac -version
```

**Status:** feito (faltando só rodar `java -version` / `javac -version` se ainda não conferiu).

---



## Passo 6 — Enviar o código do PC para a EC2 *(próximo)*

Em **outro** PowerShell no PC:

```powershell
$k  = "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\my-key.pem"
$ip = "IPV4_PUBLICO_AQUI"

# Opção A — enviar fontes (compilar na VM)
scp -i $k -r `
  "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\src" `
  "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\pom.xml" `
  "ec2-user@${ip}:~/api-gateway/"

# Opção B — compilar no Windows e enviar só as classes
# (mais simples se Lombok já estiver no PC)
# .\scripts\lab-start.ps1   # ou só a parte de compile
# scp -i $k -r "c:\Users\mayco\OneDrive\Documentos\UF\api-gateway\target\classes" `
#   "ec2-user@${ip}:~/api-gateway/target/"
```

**Status:** pendente.

---



## Passo 7 — Compilar na VM *(se usou Opção A)*

Na EC2, se precisar de Lombok no `javac`, baixar o jar ou usar Maven.  
Se usou **Opção B** (classes já compiladas), pular.

**Status:** pendente.

---



## Passo 8 — Subir gateway + instâncias

Na EC2 (idealmente com `tmux` ou vários SSH):

```bash
cd ~/api-gateway

java -cp target/classes br.imd.ufrn.Main gateway

# outros terminais / painéis tmux:
java -cp target/classes br.imd.ufrn.Main br br-1 9101
java -cp target/classes br.imd.ufrn.Main br br-2 9102
java -cp target/classes br.imd.ufrn.Main pt pt-1 9201
```

Sem argumentos extras de host → usa `127.0.0.1` (correto nesta topologia).

**Status:** pendente.

---



## Passo 9 — Testar do PC

```powershell
curl "http://${ip}:8080/time/br"
curl "http://${ip}:8080/registry"
```

JMeter: host = valor de `$ip`, portas `8080` (HTTP), `9090` (UDP), `9091` (TCP).

**Status:** pendente.

---



## Passo 10 — Demo de falha (avaliação)

Na mesma EC2, matar um worker (ex. `br-1`) e observar erros no JMeter; subir de novo o processo e ver a taxa de erro cair.

**Status:** pendente.

---



## Checklist rápido

- [x] EC2 ligada (`t3.micro`, Amazon Linux 2023)
- [x] IP público anotado (console → colar em `$ip`)
- [x] `icacls` na chave `.pem`
- [x] SSH com `ec2-user`
- [x] Java 21 Corretto (`java-21-amazon-corretto-devel` 21.0.12+9)
- [ ] Security Group (8080, 9090/UDP, 9091)
- [ ] Código / classes na VM
- [ ] Gateway + br-1 + br-2 + pt-1 rodando
- [ ] `curl` / JMeter ok do PC
- [ ] Preencher seção 4 do relatório com estes dados

---



## Relatório (seção 4) — rascunho


| Papel                   | Tipo       | SO                                  | Observação             |
| ----------------------- | ---------- | ----------------------------------- | ---------------------- |
| Gateway + workers BR/PT | `t3.micro` | Amazon Linux 2023 + Corretto 21 LTS | Todos no mesmo host    |
| JMeter                  | PC local   | —                                   | Aponta para IP público |



| Nó      | IP privado   | IP público      | Comando             |
| ------- | ------------ | --------------- | ------------------- |
| Gateway | 172.31.39.59 | *(ver console)* | `Main gateway`      |
| br-1    | (mesmo host) | —               | `Main br br-1 9101` |
| br-2    | (mesmo host) | —               | `Main br br-2 9102` |
| pt-1    | (mesmo host) | —               | `Main pt pt-1 9201` |


---

*Última atualização: Java 21 instalado (terminal 46 —* `dnf install java-21-amazon-corretto-devel`*).*