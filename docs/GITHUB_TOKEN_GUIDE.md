# GitHub Personal Access Token 발급 가이드

## Personal Access Token이란?
HTTPS를 통해 GitHub에 인증할 때 사용하는 비밀번호 대체 토큰입니다.
SSH 키를 사용하지 않는 경우 필요합니다.

## 발급 방법 (단계별)

### 1. GitHub 웹사이트 접속
- 브라우저에서 https://github.com 접속
- 로그인 (park-jongbeom 계정)

### 2. Settings로 이동
- 우측 상단 프로필 아이콘 클릭
- **Settings** 클릭

### 3. Developer settings 접속
- 좌측 메뉴 하단에서 **Developer settings** 클릭
- 또는 직접 접속: https://github.com/settings/developers

### 4. Personal access tokens 메뉴
- 좌측 메뉴에서 **Personal access tokens** 클릭
- **Tokens (classic)** 선택
- 또는 직접 접속: https://github.com/settings/tokens

### 5. 토큰 생성
- **Generate new token** 버튼 클릭
- **Generate new token (classic)** 선택

### 6. 토큰 설정
- **Note**: 토큰 설명 입력 (예: "ga-api-platform local development")
- **Expiration**: 만료 기간 선택
  - 30 days, 60 days, 90 days, 또는 No expiration (권장: 90 days)
- **Select scopes**: 필요한 권한 선택
  - ✅ **repo** (전체 체크)
    - repo:status
    - repo_deployment
    - public_repo
    - repo:invite
    - security_events
  - ✅ **workflow** (GitHub Actions 사용 시)

### 7. 토큰 생성 및 복사
- 하단 **Generate token** 버튼 클릭
- ⚠️ **중요**: 생성된 토큰을 즉시 복사하세요!
- 토큰은 한 번만 표시되며, 페이지를 벗어나면 다시 볼 수 없습니다.
- 형식: `ghp_xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx`

### 8. 토큰 저장
- 안전한 곳에 저장 (비밀번호 관리자 권장)
- 나중에 필요할 수 있으므로 백업

## 사용 방법

### 첫 번째 Push 시
```bash
cd /media/ubuntu/data120g/ga-api-platform
git push origin main
```

인증 정보 입력:
- **Username**: `park-jongbeom`
- **Password**: `<생성한 Personal Access Token>` (비밀번호가 아님!)

### Credential Helper 설정 (이미 완료됨)
```bash
git config --global credential.helper store
```
이 설정으로 한 번 인증하면 자동으로 저장되어 이후에는 토큰을 다시 입력할 필요가 없습니다.

## SSH 키 사용 (대안)

Personal Access Token 대신 SSH 키를 사용할 수도 있습니다.

### SSH 키 생성
```bash
ssh-keygen -t ed25519 -C "qk54r71z@gmail.com"
# 엔터를 눌러 기본 위치 사용
# 비밀번호 설정 (선택사항, 엔터로 건너뛰기 가능)
```

### 공개 키 확인
```bash
cat ~/.ssh/id_ed25519.pub
```

### GitHub에 SSH 키 등록
1. GitHub → Settings → SSH and GPG keys
2. New SSH key 클릭
3. Title: "Ubuntu Server" 등 설명 입력
4. Key: 위에서 복사한 공개 키 붙여넣기
5. Add SSH key 클릭

### 원격 저장소를 SSH로 변경
```bash
cd /media/ubuntu/data120g/ga-api-platform
git remote set-url origin git@github.com:park-jongbeom/ga-api-platform.git
```

### 연결 테스트
```bash
ssh -T git@github.com
# "Hi park-jongbeom! You've successfully authenticated..." 메시지 확인
```

## 보안 주의사항

1. ⚠️ 토큰을 절대 공개 저장소에 커밋하지 마세요
2. ⚠️ 토큰을 코드에 하드코딩하지 마세요
3. ⚠️ 토큰이 유출되면 즉시 GitHub에서 삭제하세요
4. ✅ 토큰은 비밀번호 관리자에 저장하세요
5. ✅ 필요시 토큰을 주기적으로 갱신하세요

## 토큰 삭제/갱신

1. GitHub → Settings → Developer settings → Personal access tokens
2. 삭제할 토큰 옆의 삭제 버튼 클릭
3. 새 토큰 생성 후 사용
