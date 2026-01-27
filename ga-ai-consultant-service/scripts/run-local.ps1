# 1. Docker 컨테이너 상태 확인 및 시작 (Redis만 로컬 실행)
$containers = docker ps --format "{{.Names}}" | Select-String "ga-redis-local"
if (-not $containers) {
    Write-Host "Starting Docker containers..."
    docker-compose -f ../docker-compose-local.yml up -d
    Start-Sleep -Seconds 5
}

# 2. 환경 변수 로드
if (Test-Path "../.env.local") {
    Get-Content "../.env.local" | ForEach-Object {
        if ($_ -match "^([^#][^=]+)=(.*)$") {
            [System.Environment]::SetEnvironmentVariable($matches[1].Trim(), $matches[2].Trim())
        }
    }
}

# 3. 애플리케이션 실행
Write-Host "Starting AI Consultant Service..."
cd ..
./gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=dev'
