# Abre gateway + 2 instancias BR + 2 PT em janelas separadas (Windows).
$root = Split-Path -Parent $PSScriptRoot
Set-Location $root

$jdk22 = "C:\Program Files\Java\jdk-22"
if (Test-Path "$jdk22\bin\java.exe") {
    $env:JAVA_HOME = $jdk22
    $env:PATH = "$jdk22\bin;$env:PATH"
}

$outDir = Join-Path $root "target\classes"
New-Item -ItemType Directory -Force -Path $outDir | Out-Null
$sources = Get-ChildItem -Recurse (Join-Path $root "src\main\java\br\imd\ufrn") -Filter *.java | ForEach-Object FullName
Write-Host "Compilando ($($sources.Count) arquivos)..." -ForegroundColor Cyan

$lombok = Join-Path $env:USERPROFILE ".m2\repository\org\projectlombok\lombok\1.18.38\lombok-1.18.38.jar"
if (-not (Test-Path $lombok)) {
    $lombok = Join-Path $env:USERPROFILE ".m2\repository\org\projectlombok\lombok\1.18.30\lombok-1.18.30.jar"
}
$javacArgs = @("-encoding", "UTF-8", "-d", $outDir)
if (Test-Path $lombok) {
    $javacArgs += @("-cp", $lombok, "-processorpath", $lombok)
}
$javacArgs += $sources
& javac @javacArgs
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

function Start-AppWindow([string]$Title, [string[]]$JavaArgs) {
    $java = if (Test-Path "$jdk22\bin\java.exe") { "$jdk22\bin\java.exe" } else { "java" }
    $argList = @("-cp", $outDir) + $JavaArgs
    $joined = ($argList | ForEach-Object { if ($_ -match '\s') { "`"$_`"" } else { $_ } }) -join ' '
    $cmd = "cd `"$root`"; Write-Host `"=== $Title ===`" -ForegroundColor Cyan; & `"$java`" $joined"
    Start-Process powershell -ArgumentList "-NoExit", "-Command", $cmd
}

Start-AppWindow "gateway" @("br.imd.ufrn.Main", "gateway")
Start-Sleep -Seconds 2
Start-AppWindow "br-1" @("br.imd.ufrn.Main", "br", "br-1", "9101")
Start-AppWindow "br-2" @("br.imd.ufrn.Main", "br", "br-2", "9102")
Start-AppWindow "pt-1" @("br.imd.ufrn.Main", "pt", "pt-1", "9201")
Start-AppWindow "pt-2" @("br.imd.ufrn.Main", "pt", "pt-2", "9202")

Write-Host "Processos abertos."
Write-Host "  TCP:  .\scripts\client-tcp.ps1 br"
Write-Host "  HTTP: curl http://127.0.0.1:8080/time/br"
Write-Host "  UDP:  .\scripts\client-udp.ps1 br"
Write-Host "Demo kill: veja scripts\kill-demo.md"
