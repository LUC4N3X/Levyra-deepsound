$ErrorActionPreference = 'Stop'
$source = "c:\Users\Luca Drogo\Desktop\Levyra-deepsound-claude-ecstatic-heisenberg-qir6ws"
$tempDir = "c:\Users\Luca Drogo\Desktop\Temp-Levyra-PR"

if (Test-Path $tempDir) { Remove-Item $tempDir -Recurse -Force }

git clone https://github.com/LUC4N3X/Levyra-deepsound.git $tempDir
Set-Location $tempDir

git checkout -b feature/update-app-icons

# We want to copy everything except .git (which source doesn't have) and maybe skip things if there are access issues, but source is just files.
Copy-Item "$source\*" -Destination $tempDir -Recurse -Force

git add .
git commit -m 'Update app icons and internal logo'
git push -u origin feature/update-app-icons

gh pr create --title 'Update app icons and internal logo' --body 'Updated app icons (mdpi to xxxhdpi) and internal logo to ensure they look great on any smartphone.'

Write-Output 'PR Created Successfully'
