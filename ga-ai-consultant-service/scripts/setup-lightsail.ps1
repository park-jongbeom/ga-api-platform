# ========================================
# Lightsail PostgreSQL 설정 자동화 스크립트 (PowerShell)
# ========================================
# 실행 방법: .\scripts\setup-lightsail.ps1

$ErrorActionPreference = "Stop"

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "Lightsail PostgreSQL 설정 시작" -ForegroundColor Cyan
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""

# 1단계: 환경변수 파일 확인
Write-Host "1. 환경변수 파일 확인 중..." -ForegroundColor Yellow
$envFile = ".env.lightsail"

if (-Not (Test-Path $envFile)) {
    Write-Host "   [경고] $envFile 파일이 없습니다." -ForegroundColor Red
    Write-Host "   .env.lightsail.example을 복사하여 $envFile을 생성하고 값을 설정하세요." -ForegroundColor Red
    Write-Host ""
    Write-Host "실행 명령:" -ForegroundColor Yellow
    Write-Host "   Copy-Item .env.lightsail.example $envFile" -ForegroundColor White
    exit 1
}

Write-Host "   [완료] $envFile 파일 발견" -ForegroundColor Green
Write-Host ""

# 2단계: 환경변수 로드
Write-Host "2. 환경변수 로드 중..." -ForegroundColor Yellow
Get-Content $envFile | ForEach-Object {
    if ($_ -match '^\s*([^#][^=]+)=(.*)$') {
        $name = $matches[1].Trim()
        $value = $matches[2].Trim()
        [System.Environment]::SetEnvironmentVariable($name, $value, "Process")
        Write-Host "   - $name 설정 완료" -ForegroundColor Gray
    }
}
Write-Host "   [완료] 환경변수 로드 완료" -ForegroundColor Green
Write-Host ""

# 3단계: 필수 환경변수 확인
Write-Host "3. 필수 환경변수 확인 중..." -ForegroundColor Yellow
$required = @("DATABASE_URL", "DATABASE_USERNAME", "DATABASE_PASSWORD", "OPENAI_API_KEY", "JWT_SECRET")
$missing = @()

foreach ($var in $required) {
    $value = [System.Environment]::GetEnvironmentVariable($var, "Process")
    if ([string]::IsNullOrEmpty($value)) {
        $missing += $var
        Write-Host "   [누락] $var" -ForegroundColor Red
    } else {
        Write-Host "   [확인] $var" -ForegroundColor Green
    }
}

if ($missing.Count -gt 0) {
    Write-Host ""
    Write-Host "   [오류] 다음 필수 환경변수가 설정되지 않았습니다:" -ForegroundColor Red
    $missing | ForEach-Object { Write-Host "      - $_" -ForegroundColor Red }
    exit 1
}
Write-Host ""

# 4단계: 데이터베이스 연결 테스트 안내
Write-Host "4. 데이터베이스 연결 테스트 안내" -ForegroundColor Yellow
Write-Host ""
Write-Host "   다음 명령어로 Lightsail PostgreSQL에 연결할 수 있습니다:" -ForegroundColor White
Write-Host ""

$dbUrl = [System.Environment]::GetEnvironmentVariable("DATABASE_URL", "Process")
if ($dbUrl -match "jdbc:postgresql://([^:]+):(\d+)/(.+)") {
    $host = $matches[1]
    $port = $matches[2]
    $database = $matches[3]
    $username = [System.Environment]::GetEnvironmentVariable("DATABASE_USERNAME", "Process")
    
    Write-Host "   psql -h $host -U $username -d $database -p $port" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "   또는 검증 스크립트 실행:" -ForegroundColor White
    Write-Host "   psql -h $host -U $username -d $database -p $port -f scripts\verify-lightsail-db.sql" -ForegroundColor Cyan
}
Write-Host ""

# 5단계: Flyway 마이그레이션 안내
Write-Host "5. Flyway 마이그레이션 실행 안내" -ForegroundColor Yellow
Write-Host ""
Write-Host "   환경변수가 설정된 상태에서 다음 명령어를 실행하세요:" -ForegroundColor White
Write-Host ""
Write-Host "   cd .." -ForegroundColor Cyan
Write-Host "   .\gradlew.bat :ga-ai-consultant-service:flywayInfo" -ForegroundColor Cyan
Write-Host "   .\gradlew.bat :ga-ai-consultant-service:flywayMigrate" -ForegroundColor Cyan
Write-Host ""

# 6단계: 애플리케이션 실행 안내
Write-Host "6. 애플리케이션 실행 안내" -ForegroundColor Yellow
Write-Host ""
Write-Host "   Lightsail 프로파일로 애플리케이션 실행:" -ForegroundColor White
Write-Host "   .\gradlew.bat :ga-ai-consultant-service:bootRun --args='--spring.profiles.active=lightsail'" -ForegroundColor Cyan
Write-Host ""

Write-Host "========================================" -ForegroundColor Cyan
Write-Host "설정 완료!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "다음 단계:" -ForegroundColor Yellow
Write-Host "1. 데이터베이스 연결 테스트" -ForegroundColor White
Write-Host "2. pgvector 확장 확인/설치" -ForegroundColor White
Write-Host "3. Flyway 마이그레이션 실행" -ForegroundColor White
Write-Host "4. 애플리케이션 실행" -ForegroundColor White
Write-Host ""
