#Requires -Version 5.1
<#
.SYNOPSIS
  银龄守护 Gradle 构建与诊断脚本（请在系统终端运行，不要在 Cursor Agent 终端运行）

.DESCRIPTION
  Cursor Agent 终端会阻止 Gradle Client 连接 localhost 守护进程。
  本脚本用于在 Windows Terminal / PowerShell 中构建 APK 和运行 Robolectric 测试。
#>

param(
    [ValidateSet("build", "test", "all", "diagnose")]
    [string]$Task = "all"
)

$ErrorActionPreference = "Stop"
$ProjectRoot = Split-Path -Parent $PSScriptRoot
$AndroidHome = if ($env:ANDROID_HOME) { $env:ANDROID_HOME } else { Join-Path (Split-Path -Parent $ProjectRoot) "android-sdk" }
$JavaHome = if ($env:JAVA_HOME) { $env:JAVA_HOME } else { "C:\Program Files\Eclipse Adoptium\jdk-17.0.20.8-hotspot" }
$GradleUserHome = if ($env:GRADLE_USER_HOME) { $env:GRADLE_USER_HOME } else { Join-Path (Split-Path -Parent $ProjectRoot) ".gradle" }

$env:JAVA_HOME = $JavaHome
$env:ANDROID_HOME = $AndroidHome
$env:GRADLE_USER_HOME = $GradleUserHome
$env:PATH = "$JavaHome\bin;$env:PATH"
$env:GRADLE_OPTS = "-Djava.net.preferIPv4Stack=true -Djava.net.preferIPv6Addresses=false"

function Test-Loopback {
    Write-Host "`n[诊断] 测试 localhost TCP 回环..." -ForegroundColor Cyan
    $listener = [System.Net.Sockets.TcpListener]::new([System.Net.IPAddress]::Loopback, 0)
    $listener.Start()
    $port = ($listener.LocalEndpoint).Port
    try {
        $client = New-Object System.Net.Sockets.TcpClient
        $client.Connect("127.0.0.1", $port)
        $client.Close()
        Write-Host "[通过] localhost TCP 正常 (port $port)" -ForegroundColor Green
        return $true
    } catch {
        Write-Host "[失败] localhost TCP 不可用: $_" -ForegroundColor Red
        Write-Host "  建议：临时关闭 VPN（Tailscale/UU加速器等），或检查 Windows 防火墙" -ForegroundColor Yellow
        return $false
    } finally {
        $listener.Stop()
    }
}

function Test-Prerequisites {
    Write-Host "`n[诊断] 检查环境..." -ForegroundColor Cyan
    if (-not (Test-Path "$JavaHome\bin\java.exe")) {
        throw "未找到 Java: $JavaHome"
    }
    & "$JavaHome\bin\java.exe" -version
    if (-not (Test-Path "$AndroidHome\platforms\android-34")) {
        Write-Host "[警告] 未找到 android-34 platform，正在安装..." -ForegroundColor Yellow
        $sdkmanager = Join-Path $AndroidHome "cmdline-tools\latest\bin\sdkmanager.bat"
        echo y | & $sdkmanager --sdk_root=$AndroidHome "platforms;android-34" "build-tools;34.0.0"
    }
    Write-Host "[通过] Android SDK: $AndroidHome" -ForegroundColor Green
}

function Invoke-Gradle {
    param([string[]]$Args)
    Push-Location $ProjectRoot
    try {
        Write-Host "`n> gradlew $($Args -join ' ')" -ForegroundColor Cyan
        & "$ProjectRoot\gradlew.bat" @Args
        if ($LASTEXITCODE -ne 0) { throw "Gradle 失败，exit code $LASTEXITCODE" }
    } finally {
        Pop-Location
    }
}

Test-Prerequisites
if (-not (Test-Loopback)) {
    Write-Host "`nGradle 需要 localhost 通信。若回环失败，构建也会失败。" -ForegroundColor Red
    if ($Task -eq "diagnose") { exit 1 }
}

switch ($Task) {
    "diagnose" { Write-Host "`n诊断完成。" -ForegroundColor Green }
    "build"    { Invoke-Gradle @(":app:assembleDebug", "--console=plain") }
    "test"     { Invoke-Gradle @(":app:testDebugUnitTest", ":core:test", "--console=plain") }
    "all"      {
        Invoke-Gradle @(":app:testDebugUnitTest", ":core:test", ":app:assembleDebug", "--console=plain")
    }
}

if ($Task -in @("build", "all")) {
    $apk = Join-Path $ProjectRoot "app\build\outputs\apk\debug\app-debug.apk"
    if (Test-Path $apk) {
        Write-Host "`n[成功] APK: $apk" -ForegroundColor Green
    }
}

Write-Host "`n完成。" -ForegroundColor Green
