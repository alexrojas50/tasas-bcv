# Publica una actualización.
#   .\publicar.ps1 -Notas "Arreglé el gráfico"                  → solo interfaz (OTA): llega a todos al reabrir la app 2 veces
#   .\publicar.ps1 -Apk -Version 2.1 -Notas "Widget nuevo"      → además publica un APK nuevo y muestra el aviso de descarga
# Usa -Apk solo si cambiaste algo en Java/Android (MainActivity, iconos, permisos...). Todo lo de index.html va por OTA.
param([switch]$Apk, [string]$Version, [string]$Notas = "")
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$gh = "C:\Program Files\GitHub CLI\gh.exe"
$gradle = "app\build.gradle.kts"

Copy-Item app\src\main\assets\index.html docs\index.html -Force

if ($Apk) {
    if (-not $Version) { throw "Indica la versión: -Version 2.1" }
    if (-not (Test-Path keystore.properties)) { throw "Falta keystore.properties (ejecuta .\crear-clave.ps1)" }
    # Sube versionCode en 1 y pone el nombre de versión
    $src = Get-Content $gradle -Raw
    $code = [int]([regex]::Match($src, 'versionCode = (\d+)').Groups[1].Value) + 1
    $src = $src -replace 'versionCode = \d+', "versionCode = $code" -replace 'versionName = "[^"]*"', "versionName = `"$Version`""
    Set-Content $gradle $src -Encoding utf8 -NoNewline

    $T = "$env:USERPROFILE\android-tools"
    $env:TMP = "$T\tmp"; $env:TEMP = "$T\tmp"; $env:GRADLE_OPTS = "-Djava.io.tmpdir=$T\tmp"
    $env:JAVA_HOME = "$T\jdk-17.0.20.1+1"; $env:ANDROID_HOME = "$T\sdk"
    .\gradlew.bat assembleRelease --console=plain -q
    if ($LASTEXITCODE) { throw "Falló la compilación" }
    Copy-Item app\build\outputs\apk\release\app-release.apk TasasBCV.apk -Force
}

git add -A
git commit -m ($(if ($Apk) { "v$Version" } else { "Interfaz" }) + $(if ($Notas) { ": $Notas" } else { "" }))
git push

if ($Apk) {
    # Primero el release (el APK ya descargable) y después el aviso, para que nadie baje un enlace roto
    $repo = & $gh repo view --json nameWithOwner -q .nameWithOwner
    & $gh release create "v$Version" TasasBCV.apk --title "v$Version" --notes $Notas
    @{ versionCode = $code; versionName = $Version; notes = $Notas
       apk = "https://github.com/$repo/releases/download/v$Version/TasasBCV.apk" } |
        ConvertTo-Json | Set-Content docs\version.json -Encoding utf8
    git add docs\version.json
    git commit -m "Aviso de v$Version"
    git push
}
Write-Host "Publicado. GitHub Pages tarda ~1 minuto en actualizarse." -ForegroundColor Green
