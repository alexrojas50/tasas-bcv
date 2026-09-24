# Crea tu clave de firma permanente (una sola vez).
# Pide una contraseña sin mostrarla y guarda la configuración en keystore.properties (no se sube a GitHub).
# HAZ UNA COPIA de %USERPROFILE%\tasasbcv.jks y de la contraseña: sin ellas no podrás publicar más actualizaciones.
$ErrorActionPreference = 'Stop'
Set-Location $PSScriptRoot
$jks = "$env:USERPROFILE\tasasbcv.jks"
if (Test-Path $jks) { Write-Host "Ya existe $jks. No se sobrescribe." -ForegroundColor Yellow; exit 1 }

$p1 = Read-Host "Contraseña para la clave (mín. 6 caracteres)" -AsSecureString
$p2 = Read-Host "Repite la contraseña" -AsSecureString
$plain = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($p1))
$check = [Runtime.InteropServices.Marshal]::PtrToStringAuto([Runtime.InteropServices.Marshal]::SecureStringToBSTR($p2))
if ($plain -ne $check) { Write-Host "Las contraseñas no coinciden." -ForegroundColor Red; exit 1 }
if ($plain.Length -lt 6) { Write-Host "Mínimo 6 caracteres." -ForegroundColor Red; exit 1 }

$env:KS_PASS = $plain
& "$env:USERPROFILE\android-tools\jdk-17.0.20.1+1\bin\keytool.exe" -genkeypair -keystore $jks -alias tasasbcv `
    -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=Tasas BCV" -storepass:env KS_PASS -keypass:env KS_PASS
Remove-Item Env:KS_PASS

@"
storeFile=$($jks -replace '\\','/')
storePassword=$plain
keyAlias=tasasbcv
keyPassword=$plain
"@ | Set-Content -Encoding ascii keystore.properties

Write-Host "`nClave creada: $jks" -ForegroundColor Green
Write-Host "Haz una copia de ese archivo y de la contraseña en un lugar seguro." -ForegroundColor Green
