$ErrorActionPreference = 'Stop'
$mavenVersion = '3.9.9'
$userHome = [System.Environment]::GetFolderPath('UserProfile')
$targetDir = Join-Path $userHome ".m2\wrapper\dists\apache-maven-$mavenVersion-bin"
$mvnCmd = Join-Path $targetDir "apache-maven-$mavenVersion\bin\mvn.cmd"

if (-not (Test-Path $mvnCmd)) {
    if (-not (Test-Path $targetDir)) {
        New-Item -ItemType Directory -Force -Path $targetDir | Out-Null
    }
    $zipPath = Join-Path $targetDir "apache-maven-$mavenVersion-bin.zip"
    Write-Host "Downloading Apache Maven $mavenVersion..."
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri "https://repo.maven.apache.org/maven2/org/apache/maven/apache-maven/$mavenVersion/apache-maven-$mavenVersion-bin.zip" -OutFile $zipPath
    Write-Host "Extracting Maven..."
    Expand-Archive -Path $zipPath -DestinationPath $targetDir -Force
    Remove-Item $zipPath -Force
}

if (Test-Path $mvnCmd) {
    & $mvnCmd $args
} else {
    Write-Error "Failed to execute Maven at $mvnCmd"
    exit 1
}
