# Personal Access Token 403 에러 해결 가이드

## 현재 상황
- ✅ Git 사용자 정보 설정 완료
- ✅ Credential helper 설정 완료  
- ⚠️ `git push` 시 403 에러 발생

## 원인 분석

### 1. 토큰 권한 부족 (가장 가능성 높음)
현재 토큰에 `repo` 권한이 없을 수 있습니다.

**해결 방법:**
1. GitHub → Settings → Developer settings → Personal access tokens
2. 발급받은 토큰 확인
3. **Classic 토큰**인 경우:
   - 토큰 클릭하여 권한 확인
   - `repo` 권한이 체크되어 있는지 확인
   - 없으면 새 토큰 발급 (기존 토큰은 권한 수정 불가)
4. **Fine-grained 토큰**인 경우:
   - Repository access에서 `ga-api-platform` 저장소 선택
   - Permissions에서 `Contents: Read and write` 권한 확인

### 2. 토큰 형식 확인

**Classic 토큰**: `ghp_`로 시작
```bash
# 예: ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

**Fine-grained 토큰**: `github_pat_`로 시작 (현재 사용 중)
```bash
# 예: github_pat_11AZ4H6AQ079PTrJ7LvF2l_...
```

Fine-grained 토큰의 경우, 저장소별 권한 설정이 필요합니다.

## 해결 방법

### 방법 1: Classic 토큰 재발급 (권장)

1. **GitHub에서 Classic 토큰 발급**:
   - GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
   - Generate new token (classic)
   - Note: "ga-api-platform local dev"
   - Expiration: 90 days (또는 원하는 기간)
   - **Select scopes**: 
     - ✅ **repo** (전체 체크박스) - 필수!
     - ✅ workflow (CI/CD 사용 시)
   - Generate token
   - 토큰 복사 (예: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`)

2. **새 토큰으로 업데이트**:
```bash
cd /media/ubuntu/data120g/ga-api-platform

# 새 토큰으로 credential 업데이트
echo "https://park-jongbeom:YOUR_NEW_CLASSIC_TOKEN@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials

# 테스트
git fetch origin
git push origin main
```

### 방법 2: Fine-grained 토큰 권한 확인

현재 토큰이 Fine-grained인 경우:

1. GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
2. 토큰 클릭
3. **Repository access** 확인:
   - ✅ `ga-api-platform` 저장소가 선택되어 있는지 확인
4. **Permissions** 확인:
   - Repository permissions → Contents: **Read and write**
   - Repository permissions → Metadata: **Read-only** (자동)
5. Save changes

### 방법 3: SSH 키 사용 (가장 안전)

Personal Access Token 대신 SSH 키 사용:

```bash
# SSH 키 생성
ssh-keygen -t ed25519 -C "qk54r71z@gmail.com"
# 엔터를 눌러 기본 위치 사용
# 비밀번호 설정 (선택사항)

# 공개 키 출력
cat ~/.ssh/id_ed25519.pub

# GitHub에 등록:
# GitHub → Settings → SSH and GPG keys → New SSH key
# 위에서 복사한 공개 키 붙여넣기

# 원격 저장소를 SSH로 변경
cd /media/ubuntu/data120g/ga-api-platform
git remote set-url origin git@github.com:park-jongbeom/ga-api-platform.git

# 테스트
ssh -T git@github.com
# "Hi park-jongbeom! You've successfully authenticated..." 메시지 확인

# Push 테스트
git push origin main
```

## 현재 설정 확인

```bash
# Git 설정 확인
git config --list | grep -E "(user|credential)"

# 원격 저장소 확인
git remote -v

# Credential 파일 확인 (보안상 내용은 숨김)
ls -la ~/.git-credentials
```

## 다음 단계

1. ✅ 토큰 권한 확인 (특히 `repo` 권한)
2. ✅ 필요시 Classic 토큰 재발급
3. ✅ 새 토큰으로 credential 업데이트
4. ✅ `git push origin main` 테스트
