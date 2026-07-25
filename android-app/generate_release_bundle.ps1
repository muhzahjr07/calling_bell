# Automated Release Build Script for Calling Bell
# Generates release.keystore and builds signed app-release.aab for Google Play Console

$ErrorActionPreference = "Stop"

Write-Host "==========================================================" -ForegroundColor Cy
Write-Host " 🔔 Calling Bell - Production App Bundle (.aab) Builder" -ForegroundColor Yellow
Write-Host "==========================================================" -ForegroundColor Cy

$KeystorePath = "app/release.keystore"

if (-not (Test-Path $KeystorePath)) {
    Write-Host "🔑 Creating new release keystore ($KeystorePath)..." -ForegroundColor Green
    keytool -genkey -v `
        -keystore $KeystorePath `
        -alias callingbell `
        -keyalg RSA `
        -keysize 2048 `
        -validity 10000 `
        -storepass callingbell123 `
        -keypass callingbell123 `
        -dname "CN=Calling Bell, OU=Risk Technologies, O=Risk Tech, L=Colombo, ST=Western, C=LK"
    Write-Host "✅ Release keystore created successfully." -ForegroundColor Green
} else {
    Write-Host "🔑 Found existing release keystore at $KeystorePath" -ForegroundColor Cyan
}

Write-Host "📦 Building signed Release Android App Bundle (.aab)..." -ForegroundColor Yellow
.\gradlew bundleRelease

$BundlePath = "app/build/outputs/bundle/release/app-release.aab"

if (Test-Path $BundlePath) {
    Write-Host ""
    Write-Host "==========================================================" -ForegroundColor Green
    Write-Host " 🎉 BUILD SUCCESSFUL!" -ForegroundColor Green
    Write-Host " 📦 App Bundle Ready for Play Console Upload:" -ForegroundColor Yellow
    Write-Host "    👉 $BundlePath" -ForegroundColor Cyan
    Write-Host "==========================================================" -ForegroundColor Green
} else {
    Write-Host "❌ Bundle generation failed." -ForegroundColor Red
}
