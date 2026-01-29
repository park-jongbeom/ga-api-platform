# Fine-grained 토큰 권한 설정 가이드

## 현재 문제
Fine-grained 토큰이 발급되었지만 저장소 접근 권한이 설정되지 않았습니다.

## 해결 방법 (GitHub 웹사이트에서 설정)

### 1. 토큰 편집 페이지로 이동
1. GitHub → Settings → Developer settings → Personal access tokens → Fine-grained tokens
2. **UBUNTU_REMOTE_TOKEN** 클릭
3. 또는 직접 URL: `https://github.com/settings/tokens?type=beta`

### 2. Repository access 설정
**Repository access** 섹션에서:
- ✅ **Only select repositories** 선택
- 드롭다운에서 **`ga-api-platform`** 저장소 선택
- 또는 **All repositories** 선택 (모든 저장소 접근)

### 3. Repository permissions 설정
**Repository permissions** 섹션에서:
- ✅ **Contents**: **Read and write** 선택 (필수!)
- ✅ **Metadata**: **Read-only** (자동 선택됨)
- ✅ **Pull requests**: **Read and write** (PR 작업 시)
- ✅ **Issues**: **Read and write** (이슈 작업 시)
- ✅ **Actions**: **Read and write** (CI/CD 사용 시)

### 4. User permissions (선택사항)
일반적으로 비워두면 됩니다. 필요시:
- ✅ **Email addresses**: **Read-only** (이메일 주소 읽기)

### 5. 저장
- 하단 **Save** 또는 **Update token** 버튼 클릭

## 설정 후 테스트

설정 완료 후 로컬에서 테스트:

```bash
cd /media/ubuntu/data120g/ga-api-platform

# 원격 저장소 확인
git remote -v

# Fetch 테스트
git fetch origin

# Push 테스트
git push origin main
```

## 권장 설정 요약

### Repository access
- ✅ Only select repositories → `ga-api-platform` 선택

### Repository permissions
- ✅ Contents: **Read and write** (필수!)
- ✅ Metadata: **Read-only** (자동)
- ✅ Pull requests: **Read and write** (선택)
- ✅ Issues: **Read and write** (선택)
- ✅ Actions: **Read and write** (CI/CD 사용 시)

### User permissions
- (비워둠 또는 필요시 Email addresses: Read-only)

## 설정 완료 후

권한 설정이 완료되면 알려주세요. Push 테스트를 진행하겠습니다.
