$ErrorActionPreference = "Stop"
$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$target = Join-Path $projectRoot "gradle\wrapper\gradle-wrapper.jar"
$url = "https://github.com/FabricMC/fabric-example-mod/raw/refs/heads/1.21.1/gradle/wrapper/gradle-wrapper.jar"

New-Item -ItemType Directory -Force -Path (Split-Path -Parent $target) | Out-Null
Write-Host "Downloading the official Fabric 1.21.1 Gradle wrapper..."
Invoke-WebRequest -Uri $url -OutFile $target
Write-Host "Saved: $target"
