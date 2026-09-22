param(
    [Parameter(Position = 0)]
    [ValidateSet("br", "pt")]
    [string]$Zone = "br",

    [string]$HostName = "127.0.0.1",
    [int]$Port = 9091
)

$utf8 = New-Object System.Text.UTF8Encoding $false
$client = New-Object System.Net.Sockets.TcpClient
try {
    $client.Connect($HostName, $Port)
    $stream = $client.GetStream()
    $writer = New-Object System.IO.StreamWriter($stream, $utf8)
    $reader = New-Object System.IO.StreamReader($stream, $utf8)
    $writer.NewLine = "`n"
    $writer.AutoFlush = $true
    $writer.WriteLine("TIME $Zone")
    $response = $reader.ReadLine()
    Write-Output $response
} finally {
    if ($client.Connected) { $client.Close() }
}
