# Abre gateway + 2 instancias BR + 1 PT em janelas separadas (Windows).
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$jdk22 = "C:\Program Files\Java\jdk-22"
if (Test-Path "$jdk22\bin\java.exe") {
    $env:JAVA_HOME = $jdk22
    $env:PATH = "$jdk22\bin;$env:PATH"
}

$outDir = Join-Path $root "target\lab-classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$sources = Get-ChildItem -Recurse (Join-Path $root "src\main\java\br\imd\ufrn\lab") -Filter *.java | ForEach-Object FullName
Write-Host "Compilando lab ($($sources.Count) arquivos)..." -ForegroundColor Cyan

# Lombok opcional (InstanceInfo usa @Getter)
$lombok = Join-Path $env:USERPROFILE ".m2\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar"
if (-not (Test-Path $lombok)) {
    $lombok = Join-Path $env:USERPROFILE ".m2\repository\org\projectlombok\lombok\1.18.30\lombok-1.18.30.jar"
}
$javacArgs = @("-encoding", "UTF-8", "-d", $outDir)
if (Test-Path $lombok) {
    $javacArgs += @("-cp", $lombok)
}
$javacArgs += $sources
& javac @javacArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

function Start-LabWindow([string]$Title, [string[]]$JavaArgs) {
    $java = if (Test-Path "$jdk22\bin\java.exe") { "$jdk22\bin\java.exe" } else { "java" }
    $argList = @("-cp", $outDir) + $JavaArgs
    $joined = ($argList | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join ' '
    $cmd = "cd `"$root`"; Write-Host `"=== $Title ===`" -ForegroundColor Cyan; & `"$java`" $joined"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
}

Start-LabWindow "lab-gateway" @("br.imd.ufrn.lab.gateway.GatewayMain")
Start-Sleep -Seconds 2
Start-LabWindow "lab-br-1" @("br.imd.ufrn.lab.instance.InstanceMain", "br", "br-1", "9101")
Start-LabWindow "lab-br-2" @("br.imd.ufrn.lab.instance.InstanceMain", "br", "br-2", "9102")
Start-LabWindow "lab-pt-1" @("br.imd.ufrn.lab.instance.InstanceMain", "pt", "pt-1", "9201")

Write-Host "Processos abertos. Teste: .\scripts\lab-time.ps1 br"
Write-Host "Demo kill: veja scripts\lab-kill-demo.md"
