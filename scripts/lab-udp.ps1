param(
    [Parameter(Position = 0)]
    [ValidateSet("br", "pt")]
    [string]$Zone = "br",

    [string]$HostName = "127.0.0.1",
    [int]$Port = 9090
)

$utf8 = New-Object System.Text.UTF8Encoding $false
$payload = $utf8.GetBytes("TIME $Zone")
$udp = New-Object System.Net.Sockets.UdpClient
try {
    $udp.Client.ReceiveTimeout = 5000
    $udp.Send($payload, $payload.Length, $HostName, $Port) | Out-Null
    $remote = New-Object System.Net.IPEndPoint([System.Net.IPAddress]::Any, 0)
    $bytes = $udp.Receive([ref]$remote)
    Write-Output $utf8.GetString($bytes)
} finally {
    $udp.Close()
}
