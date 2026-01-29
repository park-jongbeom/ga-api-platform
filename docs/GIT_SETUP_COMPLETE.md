# Git 연동 설정 완료 가이드

## 현재 설정 상태

✅ **Git 사용자 정보**
- Name: `park-jongbeom`
- Email: `qk54r71z@gmail.com`

✅ **원격 저장소**
- URL: `https://github.com/park-jongbeom/ga-api-platform.git`
- 브랜치: `main`

✅ **Credential Helper**
- 설정됨: `store` (자격 증명 저장)

## 403 에러 해결 방법

현재 `git push` 시 403 에러가 발생하는 경우, Personal Access Token의 권한을 확인해야 합니다.

### 1. Personal Access Token 권한 확인

GitHub에서 토큰 권한을 확인하세요:
1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. 발급받은 토큰 클릭
3. **필수 권한 확인**:
   - ✅ `repo` (전체 저장소 접근 권한) - **필수**
   - ✅ `workflow` (GitHub Actions 사용 시)
   - ✅ `write:packages` (패키지 업로드 시)

### 2. 토큰 재발급 (권한 수정)

기존 토큰의 권한을 수정할 수 없으므로, 새 토큰을 발급받으세요:

1. GitHub → Settings → Developer settings → Personal access tokens → Tokens (classic)
2. **Generate new token (classic)**
3. **Note**: 토큰 설명 (예: "ga-api-platform local dev")
4. **Expiration**: 만료 기간 설정
5. **Select scopes**: 
   - ✅ **repo** (전체 체크박스) - 필수!
   - ✅ workflow (CI/CD 사용 시)
6. **Generate token**
7. 새 토큰 복사 (한 번만 표시됨!)

### 3. 새 토큰으로 업데이트

```bash
cd /media/ubuntu/data120g/ga-api-platform

# 새 토큰으로 credential 업데이트
echo "https://park-jongbeom:YOUR_NEW_TOKEN@github.com" > ~/.git-credentials
chmod 600 ~/.git-credentials

# 테스트
git fetch origin
git push origin main
```

### 4. SSH 키 사용 (대안)

Personal Access Token 대신 SSH 키를 사용할 수도 있습니다:

```bash
# SSH 키 생성
ssh-keygen -t ed25519 -C "qk54r71z@gmail.com"

# 공개 키 출력
cat ~/.ssh/id_ed25519.pub

# GitHub에 등록:
# GitHub → Settings → SSH and GPG keys → New SSH key

# 원격 저장소를 SSH로 변경
git remote set-url origin git@github.com:park-jongbeom/ga-api-platform.git

# 테스트
ssh -T git@github.com
git push origin main
```

## 현재 상태

- ✅ Git 사용자 정보 설정 완료
- ✅ Credential helper 설정 완료
- ⚠️ Push 테스트 필요 (토큰 권한 확인 후)

## 다음 단계

1. Personal Access Token에 `repo` 권한이 있는지 확인
2. 필요시 새 토큰 발급 및 업데이트
3. `git push origin main` 테스트
