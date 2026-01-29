# Run from external terminal (outside Cursor) to avoid .git lock.
Set-Location $PSScriptRoot
git add -A
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
git commit -m "refactor: 단일 프로젝트로 경량화 및 서비스 통합"
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
git push origin main
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }
Write-Host "Push completed. Next: delete folder and clone from GitHub."
