# Cursor IDE Git 기능 사용 가이드

## Cursor IDE에서 Git 사용하기

Cursor는 VSCode 기반이므로, SSH로 연결된 원격 서버에서도 Git 기능을 완전히 사용할 수 있습니다.

## 현재 설정 상태

✅ **서버 측 Git 설정 완료**
- Git 사용자 정보 설정됨
- Personal Access Token 저장됨
- 원격 저장소 연결됨

✅ **Cursor 설정 확인**
- `.vscode/settings.json`에 Git 설정 있음
- `git.commitMessageLanguage`: "ko" (한국어 커밋 메시지)

## Cursor에서 Git 사용 방법

### 1. Source Control 패널 사용

1. **Source Control 아이콘 클릭** (좌측 사이드바)
   - 또는 `Ctrl+Shift+G` (Windows/Linux)
   - 또는 `Cmd+Shift+G` (Mac)

2. **변경사항 확인**
   - 수정된 파일이 자동으로 표시됨
   - 파일별 변경사항 확인 가능

3. **스테이징 (Staging)**
   - 파일 옆의 `+` 버튼 클릭
   - 또는 파일 우클릭 → "Stage Changes"

4. **커밋**
   - 상단 커밋 메시지 입력란에 메시지 입력
   - `Ctrl+Enter` 또는 커밋 버튼 클릭

5. **Push**
   - 커밋 후 자동으로 Push 옵션 표시
   - 또는 `...` 메뉴 → "Push"

### 2. Command Palette 사용

1. `Ctrl+Shift+P` (또는 `F1`)로 Command Palette 열기
2. Git 명령어 검색:
   - `Git: Stage All Changes`
   - `Git: Commit`
   - `Git: Push`
   - `Git: Pull`

### 3. 상태 표시줄 사용

- 하단 상태 표시줄에서 Git 상태 확인
- 브랜치 이름, 변경사항 수 등 표시

## Cursor Git 기능이 작동하지 않는 경우

### 문제 1: Git이 인식되지 않음

**증상**: Source Control 패널에 "No source control providers registered" 표시

**해결 방법**:
1. Cursor 재시작
2. Git 확장 프로그램 확인:
   - Extensions (`Ctrl+Shift+X`)
   - "Git" 검색
   - Git 확장이 설치되어 있는지 확인

### 문제 2: 인증 실패

**증상**: Push 시 인증 오류

**해결 방법**:
1. **Credential Helper 확인**:
```bash
git config --global credential.helper
# 출력: store (정상)
```

2. **Credential 파일 확인**:
```bash
cat ~/.git-credentials
# Personal Access Token이 저장되어 있는지 확인
```

3. **원격 저장소 URL 확인**:
```bash
git remote -v
# HTTPS 형식이어야 함
```

### 문제 3: Cursor가 원격 Git 설정을 인식하지 못함

**증상**: Cursor에서 Git 사용자 정보가 표시되지 않음

**해결 방법**:
1. **Git 설정 확인**:
```bash
git config --global user.name
git config --global user.email
```

2. **Cursor 재연결**:
   - Cursor에서 원격 연결 재시작
   - 또는 Cursor 재시작

## Cursor Git 설정 최적화

### `.vscode/settings.json`에 추가할 수 있는 설정:

```json
{
  "git.enabled": true,
  "git.autofetch": true,
  "git.confirmSync": false,
  "git.enableSmartCommit": true,
  "git.suggestSmartCommit": true,
  "git.commitMessageLanguage": "ko",
  "git.useCommitInputAsStashMessage": true
}
```

## 터미널 vs Cursor Git

### 터미널에서 Git 사용
```bash
git add .
git commit -m "메시지"
git push origin main
```

### Cursor에서 Git 사용
- GUI로 시각적으로 작업 가능
- 파일별 변경사항 확인 용이
- 커밋 메시지 히스토리 관리
- 브랜치 시각화

**둘 다 동일한 Git 설정을 사용**하므로, 어느 쪽에서든 작업 가능합니다!

## 권장 워크플로우

1. **코드 수정**: Cursor에서 파일 편집
2. **변경사항 확인**: Source Control 패널에서 확인
3. **스테이징**: Cursor GUI 또는 터미널
4. **커밋**: Cursor GUI (커밋 메시지 히스토리 활용)
5. **Push**: Cursor GUI 또는 터미널

## 현재 상태 확인

Cursor에서 확인할 수 있는 것들:
- ✅ Git 저장소 인식
- ✅ 브랜치 정보 (`main`)
- ✅ 변경사항 추적
- ✅ 원격 저장소 연결 (`origin`)

## 다음 단계

1. Cursor에서 Source Control 패널 열기 (`Ctrl+Shift+G`)
2. 변경사항 확인
3. 커밋 및 Push 테스트
