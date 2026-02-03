# GO ALMOND 프론트엔드 MVP 개발 계획

## 📋 문서 개요
- **작성일**: 2026-01-26
- **프로젝트**: GO ALMOND AI 매칭 MVP
- **범위**: 웹 프론트엔드 핵심 기능만
- **산정 기준**: 1명 기준, 하루 6시간, 주 4일 (월~목) = 주당 24시간
- **전략**: 백엔드 Mock API(Week 1) 완료 후 즉시 시작 → 병렬 개발

---

## 🎯 MVP 핵심 목표

### 최소 기능 정의 (Minimum Viable Product)
**"AI 매칭 엔진을 체험할 수 있는 최소 기능"**

1. ✅ **회원가입/로그인** - 사용자 인증
2. ✅ **필수 정보 입력** - AI 매칭을 위한 프로필 수집
3. ✅ **AI 매칭 실행** - 매칭 결과 및 6대 지표 시각화
4. ✅ **추천 학교 리스트** - Top 5 학교 표시
5. ✅ **학교 상세 보기** - 학교 정보 + 설명 가능한 추천 이유
6. ✅ **지원 관리** - 보관/지원 상태 관리
7. ✅ **간단한 대시보드** - 매칭 현황 + 지원 현황

### MVP 제외 기능 (Phase 2 이후)
- ❌ 4개 프로그램 별 상세 페이지 (간소화된 필터링으로 대체)
- ❌ 문서 표준화 (지원/비자 서류)
- ❌ 미국 생활 정보 허브
- ❌ 복잡한 검색 (간단한 필터링만)
- ❌ PDF 생성/공유 기능
- ❌ 챗봇 문의

---

## 📅 전체 일정 요약

### 백엔드 연동 타임라인
```
백엔드 Week 1 (10h) → Mock API 제공
                     ↓
프론트 Week 1 (24h) → Mock API로 UI 개발 시작
                     ↓
백엔드 Week 2 (10h) → User Profile API 완성
                     ↓
프론트 Week 2 (24h) → 실제 매칭 API 연동
                     ↓
백엔드 Week 3~4 → 매칭 엔진 + Application API
프론트 Week 3 (12h) → 통합 테스트 + 최적화
```

### 프론트엔드 일정 (총 2.5주, 60시간)

| 주차 | 시간 | Epic | 핵심 산출물 | 백엔드 상태 |
|------|------|------|-----------|-----------|
| **Week 1** | 24h | Epic 1-2 | 인증 + 프로필 입력 완성 + Mock 연동 | Mock API 제공 |
| **Week 2** | 24h | Epic 3-4 | 매칭 결과 UI + 실제 API 연동 + 학교 상세 | **✅ 실제 매칭 API** |
| **Week 3** | 12h | Epic 5-6 | 지원 관리 + 대시보드 + 통합 테스트 | Application API |

**총 예상 기간: 2.5주 (60시간, 하루 6시간 × 주 4일)**

**상세 일정**:
- Week 1: 4일 × 6h = 24h
- Week 2: 4일 × 6h = 24h
- Week 3: 2일 × 6h = 12h

---

## 🔧 Epic별 상세 작업 내용

### Week 1: 인증 + 프로필 입력 완성 (24h)

#### 백엔드 상태
- ✅ Mock API 제공 (`POST /api/v1/matching/run`)
- ✅ Swagger 문서 제공
- ✅ User Profile API (`PUT /api/v1/user/profile`)

#### Story 1.1: 회원가입/로그인 (4h)
**작업 내용**
- 회원가입 폼 (이메일/비밀번호)
- 로그인 폼
- JWT 토큰 저장 (localStorage)
- ProtectedRoute 설정

**컴포넌트**
- `SignupForm.tsx`
- `LoginForm.tsx`
- `AuthContext.tsx` (이미 구축됨 ✅)

**검증**
- [ ] 회원가입 후 자동 로그인
- [ ] JWT 토큰 저장 및 인증 확인
- [ ] 로그인 실패 시 에러 메시지

#### Story 1.2: 프로필 입력 (Step 1 - 학교 정보) (4h)
**작업 내용**
- Form Wizard 구조 생성
- Step 1: 학교 정보 입력
  - 출신학교 유형 선택 (고등학교/대학교)
  - 학교명, 지역
  - 내신 성적 (GPA 4.0 스케일)
  - 영어 점수 (TOEFL/IELTS)

**컴포넌트**
- `ProfileWizard.tsx`
- `Step1SchoolInfo.tsx`
- `WizardProgress.tsx` (단계 표시)

**검증**
- [ ] 단계별 진행 표시
- [ ] 유효성 검사 (Zod)
- [ ] 임시 저장 (localStorage)

#### Story 1.3: Mock API 연동 테스트 (2h)
**작업 내용**
- Swagger UI에서 Mock API 테스트
- Axios 클라이언트 설정 확인
- API 응답 구조 확인

**검증**
- [ ] Mock API 호출 성공
- [ ] 응답 데이터 구조 확인

#### Story 1.2: 프로필 입력 Step 2 - 개인 정보 (3h)
**작업 내용**
- 생년월일, MBTI, 성향, 자기소개 입력 폼

**컴포넌트**: `Step2PersonalInfo.tsx`

#### Story 1.3: 프로필 입력 Step 3 - 유학 목표 (4h)
**작업 내용**
- Step 3: 희망 유학 케이스 입력
  - 프로그램 유형 (Vocational/Community/University)
  - 희망 전공/직업
  - 연간 예산 범위 ($20k~$80k)
  - 희망 지역 (주/도시)
  - 학업 기간
  - 졸업 후 체류 의사 (OPT)

**컴포넌트**
- `Step3StudyPreference.tsx`
- `BudgetSlider.tsx`
- `LocationSelector.tsx`

**검증**
- [ ] 예산 슬라이더 동작
- [ ] 지역 다중 선택
- [ ] 전체 입력 완료 → 서버 저장

#### Story 2.3: 실제 User Profile API 연동 (3h)
**작업 내용**
- 프로필 저장 API 호출
- 에러 핸들링
- 완료 후 대시보드 이동

**검증**
- [ ] API 호출 성공
- [ ] 저장 후 대시보드 리다이렉트

---

### Epic 3: 매칭 결과 UI + 학교 리스트 (Week 3, 10h)

#### 백엔드 상태
- ✅ AI 매칭 엔진 완성 (Hard Filter + Base Score)

#### Story 3.1: 매칭 실행 버튼 + 로딩 (2h)
**작업 내용**
- 대시보드에 "AI 매칭 실행" 버튼
- 로딩 스피너 (3초 예상)
- Mock 매칭 API 호출

**컴포넌트**
- `MatchingTrigger.tsx`
- `LoadingSpinner.tsx`

**검증**
- [ ] 버튼 클릭 시 API 호출
- [ ] 로딩 상태 표시

#### Story 3.2: 매칭 결과 - 6대 지표 시각화 (4h)
**작업 내용**
- 매칭 점수 표시 (퍼센트 게이지)
- 6대 지표 Radar Chart
  - 학업 적합도
  - 영어 여유도
  - 예산 여유도
  - 지역 일치도
  - 기간 적합도
  - 진로 연계성

**컴포넌트**
- `MatchingResultCard.tsx`
- `MatchingRadarChart.tsx` (Recharts)

**기술 스택**
- Recharts 설치: `npm install recharts`

**검증**
- [ ] Radar Chart 정상 렌더링
- [ ] 각 지표별 점수 표시

#### Story 3.3: Top 5 추천 학교 리스트 (4h)
**작업 내용**
- 추천 학교 카드 리스트
- 학교명, 프로그램 유형, 학비, 매칭 점수
- 간단한 필터링 (프로그램 유형)
- 정렬 (매칭률 높은 순)

**컴포넌트**
- `RecommendationList.tsx`
- `SchoolCard.tsx`
- `ProgramFilter.tsx`

**검증**
- [ ] Top 5 학교 표시
- [ ] 필터링 동작
- [ ] 카드 클릭 → 학교 상세 이동

---

### Epic 4: 실제 API 연동 + 학교 상세 (Week 4, 10h)

#### 백엔드 상태
- ✅ **실제 매칭 API 완성** (`POST /api/v1/matching/run`)
- ✅ 학교 데이터 20개 제공

#### Story 4.1: Mock → 실제 API 전환 (2h)
**작업 내용**
- Mock API 엔드포인트 → 실제 API로 변경
- API 응답 구조 변경 대응
- 에러 핸들링 강화

**검증**
- [ ] 실제 매칭 결과 반환
- [ ] 에러 시 재시도 로직

#### Story 4.2: 학교 상세 페이지 (6h)
**작업 내용**
- 학교 기본 정보
  - 학교명, 프로그램 유형, 학비
  - 기숙사, 식당, 시설
  - 지역 (지도 간소화 - 주소만 표시)
- **Explainable AI 설명**
  - "이 학교를 추천하는 이유"
  - 6대 지표별 설명 표시
- 사용자 액션
  - 보관하기 (Bookmark)
  - 지원하기 (Apply)

**컴포넌트**
- `SchoolDetail.tsx`
- `SchoolInfo.tsx`
- `MatchingExplanation.tsx` (설명 표시)
- `SchoolActionBar.tsx`

**검증**
- [ ] 학교 상세 정보 표시
- [ ] 매칭 설명 표시
- [ ] 보관하기 버튼 동작

#### Story 4.3: 통합 테스트 (2h)
**작업 내용**
- 프로필 입력 → 매칭 실행 → 결과 확인 → 학교 상세 전체 플로우 테스트

**검증**
- [ ] E2E 플로우 정상 동작

---

### Epic 5: 지원 관리 + 대시보드 (Week 5, 10h)

#### 백엔드 상태
- ✅ Application API (`POST /api/v1/applications`)
- ✅ Document 업로드 (선택 기능)

#### Story 5.1: 지원하기 기능 (3h)
**작업 내용**
- 학교 상세에서 "지원하기" 버튼 클릭
- 지원 확인 모달
- Application API 호출
- 지원 상태 저장 (준비중/진행중/완료)

**컴포넌트**
- `ApplicationModal.tsx`
- `ApplicationService.ts`

**검증**
- [ ] 지원 후 상태 변경
- [ ] 중복 지원 방지

#### Story 5.2: 보관함 (2h)
**작업 내용**
- 보관한 학교 리스트 표시
- 보관 취소 기능

**컴포넌트**
- `SavedSchools.tsx`

**검증**
- [ ] 보관 목록 표시
- [ ] 보관 취소 동작

#### Story 5.3: 대시보드 (5h)
**작업 내용**
- Welcome 섹션 (사용자 이름)
- 매칭 현황 카드
  - 마지막 매칭 날짜
  - Top 1 추천 학교 표시
  - "다시 매칭하기" 버튼
- 지원 현황 카드
  - 지원한 학교 수
  - 상태별 카운트 (준비중/진행중/완료)
  - 최근 지원 학교 3개
- 보관한 학교 (최대 5개)
- Quick Action
  - "프로필 수정"
  - "매칭 실행"
  - "보관함 보기"

**컴포넌트**
- `Dashboard.tsx` (이미 구축됨, 리팩토링 ✅)
- `MatchingStatusCard.tsx`
- `ApplicationStatusCard.tsx`
- `SavedSchoolsCard.tsx`

**검증**
- [ ] 대시보드 요약 데이터 표시
- [ ] Quick Action 동작

---

### Epic 6: 통합 테스트 + 최적화 (Week 6, 10h)

#### 백엔드 상태
- ✅ 보안 강화 (JWT, Rate Limiting)
- ✅ 모니터링 (Actuator)

#### Story 6.1: E2E 통합 테스트 (3h)
**작업 내용**
- 전체 플로우 테스트
  1. 회원가입 → 로그인
  2. 프로필 입력 (3단계)
  3. 매칭 실행
  4. 결과 확인 (6대 지표)
  5. 학교 상세 보기
  6. 보관/지원
  7. 대시보드 확인
- 에러 시나리오 테스트

**검증**
- [ ] 전체 플로우 정상 동작
- [ ] 에러 핸들링 확인

#### Story 6.2: 반응형 테스트 (2h)
**작업 내용**
- 모바일/태블릿/데스크톱 레이아웃 점검
- Tailwind 반응형 클래스 적용

**검증**
- [ ] 모바일에서 정상 동작
- [ ] 태블릿에서 정상 동작

#### Story 6.3: 성능 최적화 (3h)
**작업 내용**
- 이미지 Lazy Loading
- React.lazy로 코드 스플리팅
- Recharts 번들 크기 최적화
- Lighthouse 점검

**검증**
- [ ] Lighthouse 점수 80+ (Performance)
- [ ] 번들 크기 < 500KB

#### Story 6.4: 보안 점검 (2h)
**작업 내용**
- XSS 방어 확인 (입력값 검증)
- CSRF 토큰 확인
- JWT 토큰 만료 처리
- Rate Limiting 동작 확인

**검증**
- [ ] XSS 공격 방어
- [ ] JWT 만료 시 재로그인

---

## 📊 페이지별 작업 시간 요약 (MVP)

| 페이지/기능 | 예상 시간 | 우선순위 |
|------------|----------|---------|
| 회원가입/로그인 | 4h | 높음 |
| 프로필 입력 (3단계) | 11h | 높음 |
| 매칭 실행 + 결과 표시 | 6h | 높음 |
| 학교 리스트 | 4h | 높음 |
| 학교 상세 | 6h | 높음 |
| 지원 관리 | 3h | 중간 |
| 보관함 | 2h | 중간 |
| 대시보드 | 5h | 높음 |
| 통합 테스트 | 3h | 높음 |
| 반응형 테스트 | 2h | 중간 |
| 성능 최적화 | 3h | 중간 |
| 보안 점검 | 2h | 높음 |
| Mock API 연동 | 5h | 높음 |
| 실제 API 전환 | 2h | 높음 |
| 기타 (버퍼) | 2h | - |

**총 작업 시간: 60시간**  
**총 기간: 2.5주 (하루 6시간 × 주 4일 = 주당 24시간)**  
**일정: Week 1 (24h) + Week 2 (24h) + Week 3 (12h)**

---

## 🛠️ 기술 스택 (MVP)

### 프론트엔드 (현재 프로젝트 기준)
- **Framework**: React 18 + TypeScript
- **Build**: Vite
- **Styling**: Tailwind CSS
- **Routing**: React Router v6
- **State**: Context API + Hooks
- **HTTP**: Axios (인터셉터 설정 완료 ✅)
- **Validation**: Zod
- **Testing**: Vitest + React Testing Library

### 추가 필요 라이브러리 (MVP 최소)
- **Chart**: Recharts (매칭 지표 시각화)
  ```bash
  npm install recharts
  ```
- **Form**: React Hook Form (복잡한 폼 관리)
  ```bash
  npm install react-hook-form @hookform/resolvers
  ```

### MVP 제외 라이브러리 (Phase 2)
- ❌ react-pdf (PDF 생성)
- ❌ Google Maps API (지도)
- ❌ react-dropzone (파일 업로드)
- ❌ Swiper (캐러셀)
- ❌ react-markdown (가이드 렌더링)

---

## 📐 화면 구조 및 라우팅 (MVP)

### 인증 전 (Public)
```
/                         → 랜딩 페이지 (간소화)
/signup                   → 회원가입
/login                    → 로그인
```

### 인증 후 (Protected)
```
/dashboard                → 메인 대시보드
/profile/edit             → 프로필 입력/수정 (3단계 Wizard)
/matching/result          → 매칭 결과 (6대 지표 + Top 5)
/schools/:id              → 학교 상세 (보관/지원)
/my/applications          → 내 지원 현황
/my/saved                 → 보관한 학교
```

**총 페이지: 8개 (기존 20개 → 60% 축소)**

---

## ⚠️ 리스크 및 완화 계획

### 주요 리스크

1. **백엔드 API 지연**
   - 완화: Mock API로 선행 개발 (Week 1~3)
   - 완화: API 스펙 변경 시 Adapter 패턴 적용

2. **Recharts 학습 곡선**
   - 완화: 간단한 Radar Chart만 사용
   - 완화: 공식 문서 예제 활용

3. **시간 제약 (주당 10시간)**
   - 완화: MVP 기능만 집중
   - 완화: 기구축된 컴포넌트 재사용 (Base UI ✅)

4. **API 응답 구조 변경**
   - 완화: DTO 타입 정의 (TypeScript)
   - 완화: Zod 스키마로 런타임 검증

---

## 🎯 백엔드 협업 체크리스트

### Week 1 종료 시점 확인 사항
- [ ] Swagger UI 접근 가능 (`http://localhost:8084/swagger-ui.html`)
- [ ] Mock API 엔드포인트 4개 동작
  - `POST /api/v1/matching/run`
  - `GET /api/v1/matching/result`
  - `GET /api/v1/programs?type=cc`
  - `GET /api/v1/schools/{id}`
- [ ] Mock 데이터 응답 구조 확인
- [ ] CORS 설정 확인

### Week 2 종료 시점 확인 사항
- [ ] User Profile API 동작
  - `PUT /api/v1/user/profile`
  - `POST /api/v1/user/education`
  - `POST /api/v1/user/preference`
- [ ] JWT 인증 확인
- [ ] Validation 에러 응답 확인

### Week 4 종료 시점 확인 사항
- [ ] 실제 매칭 API 동작
- [ ] 6대 지표 데이터 포함 확인
- [ ] Explainable AI 설명 포함 확인
- [ ] 학교 데이터 20개 확인

### Week 5 종료 시점 확인 사항
- [ ] Application API 동작
- [ ] Bookmark API 동작
- [ ] Dashboard API 동작

---

## 📝 컴포넌트 재사용 전략 (기구축 활용)

### 이미 구축된 컴포넌트 (✅)
- `BaseButton` → 매칭 실행, 지원하기, 보관하기 버튼에 사용
- `BaseInput` → 프로필 입력 폼에 사용
- `BaseCard` → 학교 카드, 매칭 결과 카드에 사용
- `BaseHeading`, `BaseText` → 전체 페이지 Typography
- `BaseSection`, `BaseContainer` → 레이아웃
- `AuthContext` → 인증 관리 (이미 완성 ✅)
- `ProtectedRoute` → 인증 체크 (이미 완성 ✅)
- `ErrorBoundary` → 에러 처리 (이미 완성 ✅)

### 신규 생성 컴포넌트
- `ProfileWizard.tsx` (3단계 폼)
- `MatchingRadarChart.tsx` (Recharts)
- `SchoolCard.tsx`
- `MatchingExplanation.tsx`
- `ApplicationModal.tsx`

**재사용률: 약 60% (기구축 컴포넌트 활용)**

---

## 🚀 성공 지표

### 기술적 지표
- [ ] 프로필 입력 완료율 100% (필수 필드)
- [ ] 매칭 실행 시간 < 3초 (프론트 렌더링 포함)
- [ ] 페이지 로드 시간 < 2초
- [ ] Lighthouse 점수 80+ (Performance)
- [ ] 모바일 반응형 100% 지원

### 비즈니스 지표
- [ ] 회원가입 → 프로필 입력 완료 플로우
- [ ] AI 매칭 결과 6대 지표 시각화
- [ ] Top 5 학교 추천 표시
- [ ] 학교 상세 + 설명 가능한 추천 이유
- [ ] 보관/지원 기능 동작

---

## 📈 다음 단계 (Phase 2)

### 프론트엔드 확장 기능
- 4개 프로그램 별 상세 페이지
- 문서 표준화 (지원/비자 서류 뷰어)
- PDF 생성 및 공유 기능
- 미국 생활 정보 허브
- 고급 검색 및 필터링
- 실시간 챗봇 문의
- 다국어 지원 (일본어, 중국어)

### 기술 개선
- PWA (Progressive Web App)
- 오프라인 모드
- 푸시 알림 (지원 상태 변경)
- 소셜 로그인 (Google/Naver/Kakao)
- A/B 테스트 (매칭 UI 최적화)

---

## 📋 변경 이력
- **2026-01-26 (v2)**: 작업 시간 변경 (하루 6시간 기준, 2.5주 60시간)
- **2026-01-26 (v1)**: 초기 작성 (MVP 핵심 기능 중심, 6주 60시간)
- **참고**: 백엔드 AI 매칭 MVP 계획 (6주 57시간)과 병렬 진행
