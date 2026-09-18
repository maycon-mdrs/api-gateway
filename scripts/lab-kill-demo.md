# Demo: matar e recuperar instância (lab)

Pré-requisito: gateway + `br-1` + `br-2` rodando (ver `lab-start.ps1`).

## 1. Ver round-robin

Num loop, rode:

```powershell
1..6 | ForEach-Object { .\scripts\lab-time.ps1 br; Start-Sleep -Milliseconds 200 }
```

No terminal do gateway, os logs devem alternar `br-1` e `br-2`.

## 2. Matar br-2

Na janela do worker `br-2`: **Ctrl+C**.

Espere ~6 segundos. No gateway deve aparecer:

```text
[TIMEOUT] removendo br-2 [br] 127.0.0.1:9102
```

## 3. Pedidos ainda funcionam

```powershell
.\scripts\lab-time.ps1 br
```

Deve responder `OK ...` só via `br-1`.

## 4. Matar também br-1 (opcional)

Ctrl+C em `br-1`. Depois:

```powershell
.\scripts\lab-time.ps1 br
```

Esperado: `ERROR no healthy instance for br`

## 5. Recuperar

Suba de novo:

```powershell
$env:JAVA_HOME = "C:\Program Files\Java\jdk-22"
java -cp target\lab-classes br.imd.ufrn.lab.instance.InstanceMain br br-2 9102
```

Gateway: `[NEW] br-2 ...`  
`TIME br` volta a funcionar.
