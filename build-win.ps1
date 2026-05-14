$projectDir = Split-Path -Parent $MyInvocation.MyCommand.Definition
Set-Location $projectDir

Write-Host "=== MIRV Sim Build Start ==="

if (Test-Path "dist") {
  Remove-Item -Recurse -Force "dist"
}

# Step 1
Write-Host ">>> Step 1: electron-packager"
$packagerExe = "node_modules\.bin\electron-packager.cmd"
$args = @(".", "MIRV-Sim", "--platform=win32", "--arch=x64", "--out=dist", "--overwrite", "--prune=true", "--ignore=NukemapApp", "--ignore=.git", "--ignore=dist", "--ignore=installer.iss", "--ignore=build-win.ps1")
& $packagerExe $args
if ($LASTEXITCODE -ne 0) { Write-Host "electron-packager failed"; exit 1 }
Write-Host "electron-packager OK"

# Step 2
Write-Host ">>> Step 2: Inno Setup"
$iscc = "$env:USERPROFILE\.innosetup\ISCC.exe"
if (!(Test-Path $iscc)) { Write-Host "ISCC not found"; exit 1 }
& $iscc "installer.iss"
if ($LASTEXITCODE -ne 0) { Write-Host "Inno Setup failed"; exit 1 }
Write-Host "Inno Setup OK"

$output = Get-Item "dist\MIRV-Sim-Setup-*.exe" | Select-Object -First 1
$sizeMB = [math]::Round($output.Length / 1MB, 1)
Write-Host "=== Build Complete ==="
Write-Host "Installer: $($output.FullName)"
Write-Host "Size: $sizeMB MB"
