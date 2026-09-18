# Lab Fase 1 — Gateway + Heartbeat (horário BR/PT)

## Pacotes

```text
br.imd.ufrn.lab
├── model/       → InstanceInfo (dados compartilhados)
├── gateway/     → processo do Gateway
│   ├── GatewayMain
│   ├── HeartbeatServer   (:9000)
│   ├── ClientTcpServer   (:9091)
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
| 9091 | Cliente: `TIME br` / `TIME pt` |
| 91xx | Instâncias Brasil |
| 92xx | Instâncias Portugal |

## Compilar

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
mkdir -Force target\lab-classes | Out-Null
Get-ChildItem -Recurse src\main\java\br\imd\ufrn\lab -Filter *.java |
  ForEach-Object { $_.FullName } |
  ForEach-Object { $_ } |
  Out-Null
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

Teste: `.\scripts\lab-time.ps1 br`  
Demo kill: `scripts\lab-kill-demo.md`
