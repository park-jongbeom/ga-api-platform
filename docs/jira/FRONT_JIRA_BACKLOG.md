# Go Almond AI 매칭 MVP - 프론트엔드 JIRA Backlog

**프로젝트**: Go Almond AI Matching Frontend  
**버전**: MVP 1.0  
**예상 기간**: 2.5주 (하루 6시간 × 주 4일 = 주당 24시간)  
**작성일**: 2026-01-26 (업데이트: 백엔드 협업 반영)

---

## Story Points 산정 기준

| Story Points | 예상 시간 | 복잡도 |
|-------------|---------|--------|
| 1 SP | 1시간 | 매우 낮음 |
| 2 SP | 2시간 | 낮음 |
| 3 SP | 3시간 | 보통 |
| 4 SP | 4시간 | 높음 |
| 5 SP | 5시간 | 높음 |
| 6 SP | 6시간 | 매우 높음 |

**주당 가용 SP**: 24 SP (월~목, 하루 6시간)  
**전체 기간**: 2.5주 (60 SP)  
**백엔드 의존성**: Mock API (Week 1) → 실제 API (Week 2~)

---

## Epic 1: 인증 & 프로필 입력 (Week 1)

**Epic ID**: GAMF-1  
**Epic Name**: 사용자 인증 및 AI 매칭 필수 정보 수집  
**Business Value**: 사용자가 AI 매칭을 받기 위한 기본 정보 입력  
**Target Sprint**: Week 1  
**Total Story Points**: 24 SP  
**백엔드 의존성**: Mock API (GAM-11), User Profile API (GAM-22)

---

### Story GAMF-11: 회원가입/로그인

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 4 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `authentication`, `week1`, `core-feature`

**Description**:
사용자 인증 기능 구현. 이메일/비밀번호 기반 회원가입 및 로그인, JWT 토큰 관리.

**User Story**:
```
AS A 학생 사용자
I WANT 이메일과 비밀번호로 회원가입하고 로그인하며
SO THAT AI 매칭 서비스를 사용할 수 있다
```

**Acceptance Criteria**:
- [ ] 회원가입 폼 렌더링 (이메일/비밀번호/비밀번호 확인)
- [ ] 이메일 형식 검증 (Zod)
- [ ] 비밀번호 강도 검증 (최소 8자, 영문+숫자)
- [ ] 회원가입 후 자동 로그인
- [ ] JWT 토큰 localStorage에 저장
- [ ] 로그인 폼 렌더링
- [ ] 로그인 실패 시 에러 메시지 표시
- [ ] "비밀번호 찾기" 링크 표시
- [ ] ProtectedRoute로 인증된 사용자만 대시보드 접근

**Tasks**:
- [ ] GAMF-11-1: SignupForm.tsx 구현 (BaseInput 활용) - 1.5h
- [ ] GAMF-11-2: LoginForm.tsx 구현 - 1h
- [ ] GAMF-11-3: AuthService API 연동 (회원가입/로그인) - 1h
- [ ] GAMF-11-4: JWT 토큰 localStorage 저장 로직 - 0.5h

**Technical Notes**:
```typescript
// 파일 위치
src/pages/Signup.tsx
src/pages/Login.tsx
src/context/AuthContext.tsx (이미 구축 ✅)
src/api/AuthService.ts (이미 구축 ✅)

// Zod 스키마
const SignupSchema = z.object({
  email: z.string().email('유효한 이메일을 입력하세요'),
  password: z.string().min(8, '비밀번호는 최소 8자 이상'),
  passwordConfirm: z.string()
}).refine(data => data.password === data.passwordConfirm, {
  message: '비밀번호가 일치하지 않습니다',
  path: ['passwordConfirm']
});
```

**Definition of Done**:
- 회원가입 및 로그인 플로우 정상 동작
- JWT 토큰 저장 및 인증 체크 확인
- Vitest 단위 테스트 작성 완료

---

### Story GAMF-12: 프로필 입력 Step 1 - 학교 정보

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 4 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `profile`, `wizard`, `week1`, `core-feature`

**Description**:
AI 매칭을 위한 학교 정보 수집 (출신학교, 내신 성적, 영어 점수)

**User Story**:
```
AS A 학생 사용자
I WANT 내 학력 정보를 입력하고
SO THAT AI가 내 학업 수준을 평가할 수 있다
```

**Acceptance Criteria**:
- [ ] ProfileWizard 3단계 구조 생성 (Progress Bar 포함)
- [ ] 출신학교 유형 선택 가능 (고등학교/대학교)
- [ ] 학교명 입력 (텍스트)
- [ ] 지역 입력 (텍스트 or 드롭다운)
- [ ] 내신 성적 입력 (GPA 0.0~4.0 범위 검증)
- [ ] 영어 점수 입력 (TOEFL 0~120, IELTS 0~9 범위 검증)
- [ ] 실시간 유효성 검사 (Zod)
- [ ] 임시 저장 (localStorage)
- [ ] "다음" 버튼 클릭 시 Step 2로 이동

**Tasks**:
- [ ] GAMF-12-1: ProfileWizard.tsx 구조 생성 (Progress Bar) - 1h
- [ ] GAMF-12-2: Step1SchoolInfo.tsx 폼 구현 - 2h
- [ ] GAMF-12-3: Zod 스키마 정의 및 Validation - 0.5h
- [ ] GAMF-12-4: localStorage 임시 저장 로직 - 0.5h

**Technical Notes**:
```typescript
// 파일 위치
src/components/ProfileWizard.tsx
src/components/Step1SchoolInfo.tsx
src/components/WizardProgress.tsx

// Zod 스키마
const Step1Schema = z.object({
  schoolType: z.enum(['high_school', 'university']),
  schoolName: z.string().min(2, '학교명을 입력하세요'),
  schoolLocation: z.string().min(2, '지역을 입력하세요'),
  gpa: z.number().min(0).max(4.0, 'GPA는 0.0~4.0 범위'),
  englishTestType: z.enum(['TOEFL', 'IELTS']),
  englishScore: z.number().min(0)
});
```

**Definition of Done**:
- Step 1 폼 정상 렌더링
- 유효성 검사 동작
- 임시 저장 후 새로고침해도 데이터 유지

---

### Story GAMF-13: 프로필 입력 Step 2 - 개인 정보

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 3 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `profile`, `wizard`, `week1`

**Description**:
AI 매칭 정확도 향상을 위한 개인 정보 수집

**User Story**:
```
AS A 학생 사용자
I WANT 내 개인 정보를 입력하고
SO THAT 더 정확한 매칭을 받을 수 있다
```

**Acceptance Criteria**:
- [ ] 생년월일 입력 (DatePicker 또는 input type="date")
- [ ] 생년월일 유효성 검사 (만 18세 이상)
- [ ] MBTI 선택 (16가지 드롭다운)
- [ ] 성향 입력 (BaseTextarea, 선택 사항)
- [ ] 자기소개 입력 (BaseTextarea, 선택 사항)
- [ ] "이전" 버튼 클릭 시 Step 1로 데이터 유지
- [ ] "다음" 버튼 클릭 시 Step 3 이동

**Tasks**:
- [ ] GAMF-13-1: Step2PersonalInfo.tsx 폼 구현 - 2h
- [ ] GAMF-13-2: "이전"/"다음" 버튼 로직 - 0.5h
- [ ] GAMF-13-3: MBTI 선택 UI - 0.5h

**Definition of Done**:
- Step 2 폼 정상 렌더링
- 단계 이동 정상 동작
- 선택 필드 미입력해도 다음 진행 가능

---

### Story GAMF-14: 프로필 입력 Step 3 - 유학 목표

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 4 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `profile`, `wizard`, `week1`, `core-feature`

**Description**:
AI 매칭 핵심 입력값 수집 (프로그램 유형, 예산, 지역, 진로)

**User Story**:
```
AS A 학생 사용자
I WANT 내 유학 목표를 입력하고
SO THAT 내 목표에 맞는 학교를 추천받을 수 있다
```

**Acceptance Criteria**:
- [ ] 프로그램 유형 선택 (Vocational/Community/University 라디오 버튼)
- [ ] 희망 전공/직업 입력 (텍스트 또는 자동완성)
- [ ] 연간 예산 범위 슬라이더 ($20k~$80k)
- [ ] 예산 슬라이더 실시간 값 표시
- [ ] 희망 지역 다중 선택 (체크박스)
- [ ] 학업 기간 선택 (1년/2년/4년)
- [ ] 졸업 후 체류 의사 (OPT) 선택
- [ ] "완료" 버튼 클릭 시 전체 데이터 검증
- [ ] 검증 실패 시 해당 Step으로 이동 및 에러 표시

**Tasks**:
- [ ] GAMF-14-1: Step3StudyPreference.tsx 폼 구현 - 1.5h
- [ ] GAMF-14-2: BudgetSlider.tsx 구현 - 1h
- [ ] GAMF-14-3: LocationSelector.tsx 다중 선택 구현 - 1h
- [ ] GAMF-14-4: 전체 데이터 검증 로직 - 0.5h

**Technical Notes**:
```typescript
// BudgetSlider
<input
  type="range"
  min={20000}
  max={80000}
  step={1000}
  value={budget}
  onChange={(e) => setBudget(Number(e.target.value))}
/>
<span>${budget.toLocaleString()}</span>
```

**Definition of Done**:
- Step 3 폼 정상 렌더링
- 예산 슬라이더 동작
- 전체 검증 정상 동작

---

### Story GAMF-15: 프로필 저장 API 연동

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 3 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `api`, `profile`, `week1`

**Description**:
입력된 프로필 데이터를 백엔드 API로 저장

**User Story**:
```
AS A 학생 사용자
I WANT 입력한 프로필이 저장되고
SO THAT 다음에 접속해도 다시 입력하지 않아도 된다
```

**Acceptance Criteria**:
- [ ] UserProfileService API 호출
  - `PUT /api/v1/user/profile`
  - `POST /api/v1/user/education`
  - `POST /api/v1/user/preference`
- [ ] DTO 타입 정의 (TypeScript Interface)
- [ ] Zod 스키마로 런타임 검증
- [ ] API 실패 시 에러 메시지 표시
- [ ] 재시도 로직 (최대 3회)
- [ ] 저장 중 로딩 스피너 표시
- [ ] 저장 성공 시 /dashboard 리다이렉트
- [ ] 성공 토스트 메시지

**Tasks**:
- [ ] GAMF-15-1: UserProfileService.ts API 클라이언트 - 1h
- [ ] GAMF-15-2: DTO 타입 정의 - 0.5h
- [ ] GAMF-15-3: 에러 핸들링 및 재시도 로직 - 1h
- [ ] GAMF-15-4: 로딩 스피너 및 토스트 메시지 - 0.5h

**Definition of Done**:
- 프로필 저장 API 정상 호출
- 에러 처리 정상 동작
- 저장 후 대시보드 이동 확인

---

### Story GAMF-16: Mock API 연동 테스트

**Story Type**: Task  
**Priority**: High  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `testing`, `mock-api`, `week1`

**Description**:
백엔드 Mock API와 연동 테스트 및 응답 구조 확인

**User Story**:
```
AS A 프론트엔드 개발자
I WANT Mock API 응답 구조를 확인하고
SO THAT 실제 API 연동 전에 UI를 완성할 수 있다
```

**Acceptance Criteria**:
- [ ] Swagger UI 접속 확인 (http://localhost:8084/swagger-ui.html)
- [ ] 4개 Mock API 호출 성공
  - `POST /api/v1/matching/run`
  - `GET /api/v1/matching/result`
  - `GET /api/v1/programs?type=cc`
  - `GET /api/v1/schools/{id}`
- [ ] 응답 데이터 구조 확인 및 타입 정의
- [ ] Axios 인스턴스 Base URL 설정 확인
- [ ] 인터셉터 동작 확인 (JWT 토큰 자동 첨부)

**Tasks**:
- [ ] GAMF-16-1: Swagger UI에서 Mock API 테스트 - 0.5h
- [ ] GAMF-16-2: Axios 인스턴스 설정 확인 - 0.5h
- [ ] GAMF-16-3: Mock API 응답 타입 정의 - 0.5h
- [ ] GAMF-16-4: API 호출 테스트 코드 작성 - 0.5h

**Definition of Done**:
- 모든 Mock API 정상 호출
- 응답 타입 정의 완료
- Axios 인터셉터 동작 확인

---

### Story GAMF-17: Form Validation & Error Handling

**Story Type**: Task  
**Priority**: High  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `validation`, `error-handling`, `week1`

**Description**:
전체 프로필 입력 폼의 유효성 검사 및 에러 핸들링 강화

**User Story**:
```
AS A 학생 사용자
I WANT 입력 오류를 즉시 확인하고
SO THAT 올바른 정보를 입력할 수 있다
```

**Acceptance Criteria**:
- [ ] 3단계 전체 Zod 스키마 통합
- [ ] 필수/선택 필드 구분
- [ ] 실시간 유효성 검사 동작
- [ ] 필드별 에러 메시지 표시 (BaseInput errorText prop 활용)
- [ ] 전체 에러 요약 표시
- [ ] 전체 검증 실패 시 첫 번째 에러 필드로 스크롤
- [ ] 한글 에러 메시지

**Tasks**:
- [ ] GAMF-17-1: 전체 Zod 스키마 통합 - 1h
- [ ] GAMF-17-2: 에러 메시지 UI 구현 - 0.5h
- [ ] GAMF-17-3: 에러 필드 스크롤 로직 - 0.5h

**Definition of Done**:
- 실시간 유효성 검사 동작
- 에러 메시지 정상 표시
- 사용자 친화적 에러 핸들링

---

### Story GAMF-18: 단위 테스트 (Week 1)

**Story Type**: Task  
**Priority**: Medium  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 1  
**Labels**: `testing`, `week1`

**Description**:
Week 1 완성 컴포넌트 단위 테스트 작성

**Acceptance Criteria**:
- [ ] 회원가입 폼 렌더링 테스트
- [ ] 로그인 폼 렌더링 테스트
- [ ] Step 1~3 렌더링 테스트
- [ ] Wizard 단계 이동 테스트
- [ ] Zod 스키마 테스트
- [ ] 에러 메시지 표시 테스트
- [ ] npm test 통과
- [ ] 테스트 커버리지 80% 이상

**Tasks**:
- [ ] GAMF-18-1: 인증 컴포넌트 테스트 - 0.5h
- [ ] GAMF-18-2: 프로필 입력 컴포넌트 테스트 - 1h
- [ ] GAMF-18-3: Validation 테스트 - 0.5h

**Definition of Done**:
- 모든 테스트 통과
- 주요 컴포넌트 테스트 작성 완료

---

## Epic 2: 매칭 결과 & API 연동 (Week 2)

**Epic ID**: GAMF-2  
**Epic Name**: AI 매칭 실행 및 결과 시각화  
**Business Value**: AI 매칭 핵심 기능 제공  
**Target Sprint**: Week 2  
**Total Story Points**: 24 SP  
**백엔드 의존성**: AI 매칭 엔진 (GAM-31~33), 매칭 API (GAM-42)

---

### Story GAMF-21: 매칭 실행 UI

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `matching`, `week2`, `core-feature`

**Description**:
대시보드에서 AI 매칭을 실행하는 UI 구현

**User Story**:
```
AS A 학생 사용자
I WANT AI 매칭을 실행하고
SO THAT 나에게 맞는 학교 추천을 받을 수 있다
```

**Acceptance Criteria**:
- [ ] "AI 매칭 실행" BaseButton 렌더링
- [ ] 프로필 미입력 시 버튼 비활성화
- [ ] 버튼 클릭 시 Mock 매칭 API 호출
- [ ] 로딩 중 스피너 표시
- [ ] 예상 시간 표시 (약 3초)
- [ ] 로딩 중 버튼 비활성화
- [ ] 매칭 완료 후 결과 페이지 이동
- [ ] 에러 시 재시도 버튼 표시

**Tasks**:
- [ ] GAMF-21-1: MatchingTrigger.tsx 구현 - 1h
- [ ] GAMF-21-2: LoadingSpinner.tsx 구현 - 0.5h
- [ ] GAMF-21-3: Mock 매칭 API 호출 로직 - 0.5h

**Definition of Done**:
- 매칭 실행 버튼 정상 동작
- 로딩 스피너 표시
- 결과 페이지 이동 확인

---

### Story GAMF-22: 6대 지표 시각화 (Recharts)

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 4 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `matching`, `visualization`, `week2`, `core-feature`

**Description**:
AI 매칭 결과의 6대 지표를 Radar Chart로 시각화

**User Story**:
```
AS A 학생 사용자
I WANT 매칭 결과를 시각적으로 확인하고
SO THAT 어떤 부분이 강점/약점인지 파악할 수 있다
```

**Acceptance Criteria**:
- [ ] Recharts 라이브러리 설치 (`npm install recharts`)
- [ ] 전체 매칭률 퍼센트 게이지 표시
- [ ] 6대 지표 Radar Chart 렌더링
  - 학업 적합도 (20%)
  - 영어 적합도 (15%)
  - 예산 적합도 (15%)
  - 지역 선호 (10%)
  - 기간 적합도 (10%)
  - 진로 연계성 (30%)
- [ ] 각 지표별 점수 0~100 범위
- [ ] 차트 마우스 오버 시 상세 값 표시
- [ ] 반응형 Chart (모바일 대응)
- [ ] 6대 지표 목록 표시

**Tasks**:
- [ ] GAMF-22-1: Recharts 설치 및 기본 설정 - 0.5h
- [ ] GAMF-22-2: MatchingResultCard.tsx 구현 - 1.5h
- [ ] GAMF-22-3: MatchingRadarChart.tsx 구현 - 2h

**Technical Notes**:
```typescript
import { Radar, RadarChart, PolarGrid, PolarAngleAxis, PolarRadiusAxis } from 'recharts';

const data = [
  { subject: '학업', A: 85, fullMark: 100 },
  { subject: '영어', A: 90, fullMark: 100 },
  { subject: '예산', A: 75, fullMark: 100 },
  { subject: '지역', A: 80, fullMark: 100 },
  { subject: '기간', A: 70, fullMark: 100 },
  { subject: '진로', A: 95, fullMark: 100 }
];
```

**Definition of Done**:
- Radar Chart 정상 렌더링
- 반응형 동작 확인
- 데이터 바인딩 정상

---

### Story GAMF-23: Top 5 추천 학교 리스트

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 4 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `matching`, `school-list`, `week2`, `core-feature`

**Description**:
AI 매칭 결과 Top 5 학교를 카드 리스트로 표시

**User Story**:
```
AS A 학생 사용자
I WANT 추천 학교 리스트를 확인하고
SO THAT 관심 학교를 선택할 수 있다
```

**Acceptance Criteria**:
- [ ] BaseGrid를 활용한 반응형 리스트 레이아웃
- [ ] Top 5 학교 카드 표시
- [ ] 학교 카드 정보
  - 학교명
  - 프로그램 유형 (BaseBadge)
  - 학비
  - 매칭 점수 (퍼센트)
  - "상세 보기" BaseButton
- [ ] 프로그램 유형 필터 (Vocational/Community/University)
- [ ] 매칭률 순 정렬
- [ ] 학비 순 정렬
- [ ] 카드 클릭 시 학교 상세 페이지 이동
- [ ] 추천 학교 없을 시 안내 메시지

**Tasks**:
- [ ] GAMF-23-1: RecommendationList.tsx 구현 - 1h
- [ ] GAMF-23-2: SchoolCard.tsx 구현 - 2h
- [ ] GAMF-23-3: ProgramFilter.tsx 구현 - 1h

**Definition of Done**:
- 학교 리스트 정상 표시
- 필터링 및 정렬 동작
- 카드 클릭 이동 확인

---

### Story GAMF-24: 실제 매칭 API 전환

**Story Type**: Task  
**Priority**: Critical  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `api`, `integration`, `week2`

**Description**:
Mock API에서 실제 백엔드 매칭 API로 전환

**User Story**:
```
AS A 프론트엔드 개발자
I WANT Mock API에서 실제 API로 전환하고
SO THAT 실제 매칭 결과를 사용자에게 제공할 수 있다
```

**Acceptance Criteria**:
- [ ] API 엔드포인트 Mock → 실제 URL 변경
- [ ] 환경변수로 API URL 분리 (VITE_API_BASE_URL)
- [ ] DTO 타입 업데이트 (백엔드 응답 구조 반영)
- [ ] 필드 매핑 조정
- [ ] API 실패 시 재시도 로직 (최대 3회)
- [ ] 타임아웃 처리 (5초)
- [ ] 6대 지표 데이터 정상 파싱 확인
- [ ] Explainable AI 설명 포함 확인
- [ ] 에러 시 사용자 친화적 메시지

**Tasks**:
- [ ] GAMF-24-1: API 엔드포인트 변경 및 환경변수 분리 - 0.5h
- [ ] GAMF-24-2: DTO 타입 업데이트 - 1h
- [ ] GAMF-24-3: 에러 핸들링 강화 - 0.5h

**Definition of Done**:
- 실제 매칭 API 호출 성공
- 응답 데이터 정상 파싱
- 에러 처리 정상 동작

---

### Story GAMF-25: 학교 상세 페이지

**Story Type**: Story  
**Priority**: High  
**Story Points**: 6 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `school-detail`, `week2`

**Description**:
추천 학교의 상세 정보 및 매칭 이유 표시

**User Story**:
```
AS A 학생 사용자
I WANT 학교 상세 정보와 추천 이유를 확인하고
SO THAT 지원 여부를 결정할 수 있다
```

**Acceptance Criteria**:
- [ ] 학교 기본 정보 표시
  - 학교명
  - 프로그램 유형
  - 학비
  - 기숙사 정보
  - 식당 정보
  - 주소 (지도 제외)
- [ ] "이 학교를 추천하는 이유" 섹션
  - Explainable AI 설명 텍스트 렌더링
  - 6대 지표별 설명 표시
- [ ] 사용자 액션 버튼
  - "보관하기" BaseButton
  - "지원하기" BaseButton
- [ ] 버튼 상태 관리 (이미 보관/지원한 경우)
- [ ] "뒤로가기" 버튼
- [ ] BaseSection, BaseContainer 활용 반응형 레이아웃

**Tasks**:
- [ ] GAMF-25-1: SchoolDetail.tsx 레이아웃 - 1h
- [ ] GAMF-25-2: SchoolInfo.tsx 기본 정보 섹션 - 2h
- [ ] GAMF-25-3: MatchingExplanation.tsx 설명 섹션 - 2h
- [ ] GAMF-25-4: SchoolActionBar.tsx 액션 버튼 - 1h

**Definition of Done**:
- 학교 상세 정보 정상 표시
- 매칭 설명 정상 렌더링
- 액션 버튼 동작 확인

---

### Story GAMF-26: Explainable AI 설명 표시

**Story Type**: Story  
**Priority**: High  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `explainable-ai`, `week2`

**Description**:
백엔드에서 생성된 매칭 설명을 사용자 친화적으로 표시

**User Story**:
```
AS A 학생 사용자
I WANT 매칭 설명을 쉽게 이해하고
SO THAT 추천 이유를 신뢰할 수 있다
```

**Acceptance Criteria**:
- [ ] 백엔드 템플릿 기반 설명 텍스트 파싱
- [ ] 6대 지표별 설명 분리
- [ ] 아코디언 또는 탭 UI
- [ ] 아이콘 + 설명 텍스트
- [ ] 긍정 요소 강조 (초록색 아이콘)
- [ ] 부정 요소 경고 (노란색/빨간색 아이콘)
- [ ] 설명이 없을 경우 기본 메시지

**Tasks**:
- [ ] GAMF-26-1: 설명 텍스트 파싱 로직 - 1h
- [ ] GAMF-26-2: MatchingExplanation.tsx UI 구현 - 1h

**Definition of Done**:
- 설명 정상 표시
- 사용자 친화적 UI
- 긍정/부정 구분 명확

---

### Story GAMF-27: 보관하기 기능

**Story Type**: Story  
**Priority**: Medium  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `bookmark`, `week2`

**Description**:
관심 학교를 보관하여 나중에 확인 가능

**User Story**:
```
AS A 학생 사용자
I WANT 관심 학교를 보관하고
SO THAT 나중에 다시 확인할 수 있다
```

**Acceptance Criteria**:
- [ ] "보관하기" BaseButton 렌더링
- [ ] 버튼 클릭 시 Bookmark API 호출 (`POST /api/v1/bookmarks`)
- [ ] 보관 성공 시 아이콘 변경 (빈 하트 → 채운 하트)
- [ ] 보관 취소 시 API 호출 (`DELETE /api/v1/bookmarks/{id}`)
- [ ] 중복 보관 방지
- [ ] 보관 성공 토스트 메시지

**Tasks**:
- [ ] GAMF-27-1: BookmarkButton.tsx 구현 - 1h
- [ ] GAMF-27-2: Bookmark API 연동 - 1h

**Definition of Done**:
- 보관 기능 정상 동작
- 아이콘 변경 확인
- 토스트 메시지 표시

---

### Story GAMF-28: 단위 테스트 (Week 2)

**Story Type**: Task  
**Priority**: Medium  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 2  
**Labels**: `testing`, `week2`

**Description**:
Week 2 완성 컴포넌트 단위 테스트 작성

**Acceptance Criteria**:
- [ ] 매칭 실행 버튼 테스트
- [ ] Radar Chart 렌더링 테스트
- [ ] 추천 리스트 렌더링 테스트
- [ ] 학교 상세 정보 표시 테스트
- [ ] 보관 버튼 동작 테스트
- [ ] npm test 통과

**Tasks**:
- [ ] GAMF-28-1: 매칭 결과 컴포넌트 테스트 - 1h
- [ ] GAMF-28-2: 학교 상세 컴포넌트 테스트 - 1h

**Definition of Done**:
- 주요 컴포넌트 테스트 작성
- 모든 테스트 통과

---

## Epic 3: 지원 관리 & 완료 (Week 3)

**Epic ID**: GAMF-3  
**Epic Name**: 지원 관리 기능 및 대시보드 완성  
**Business Value**: 사용자가 지원 과정을 추적 가능  
**Target Sprint**: Week 3  
**Total Story Points**: 12 SP  
**백엔드 의존성**: Application API (GAM-51), Dashboard API (GAM-53)

---

### Story GAMF-31: 지원하기 기능

**Story Type**: Story  
**Priority**: High  
**Story Points**: 3 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 3  
**Labels**: `application`, `week3`

**Description**:
학교 상세 페이지에서 지원 신청 및 상태 관리

**User Story**:
```
AS A 학생 사용자
I WANT 관심 학교에 지원하고
SO THAT 지원 현황을 관리할 수 있다
```

**Acceptance Criteria**:
- [ ] "지원하기" BaseButton 렌더링
- [ ] 버튼 클릭 시 지원 확인 모달 표시
- [ ] 모달에 학교 정보 확인 표시
- [ ] 모달 "확인" 버튼 클릭 시 Application API 호출
- [ ] 지원 상태 저장 (준비중/진행중/완료)
- [ ] 중복 지원 방지 (이미 지원한 학교 체크)
- [ ] 중복 지원 시 안내 메시지
- [ ] 지원 성공 시 상태 변경 (버튼 비활성화)
- [ ] 지원 후 대시보드에서 확인 가능

**Tasks**:
- [ ] GAMF-31-1: ApplicationModal.tsx 구현 - 1.5h
- [ ] GAMF-31-2: ApplicationService.ts API 연동 - 1h
- [ ] GAMF-31-3: 중복 지원 방지 로직 - 0.5h

**Definition of Done**:
- 지원 기능 정상 동작
- 중복 지원 방지 확인
- 모달 UI 정상 표시

---

### Story GAMF-32: 대시보드 구현

**Story Type**: Story  
**Priority**: Critical  
**Story Points**: 5 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 3  
**Labels**: `dashboard`, `week3`, `core-feature`

**Description**:
사용자 맞춤 대시보드 - 매칭 현황, 지원 현황, 보관한 학교 표시

**User Story**:
```
AS A 학생 사용자
I WANT 대시보드에서 한눈에 현황을 파악하고
SO THAT 다음 단계를 계획할 수 있다
```

**Acceptance Criteria**:
- [ ] Welcome 섹션 (사용자 이름 + 환영 메시지)
- [ ] 매칭 현황 카드
  - 마지막 매칭 날짜
  - Top 1 추천 학교 표시
  - "다시 매칭하기" BaseButton
- [ ] 지원 현황 카드
  - 지원한 학교 수
  - 상태별 카운트 (준비중/진행중/완료)
  - 최근 지원 학교 3개
- [ ] 보관한 학교 카드 (최대 5개)
  - "더 보기" 링크 → /my/saved
- [ ] Quick Action 버튼
  - "프로필 수정"
  - "매칭 실행"
  - "보관함 보기"
- [ ] Dashboard API 호출 (`GET /api/v1/dashboard`)
- [ ] BaseSection, BaseContainer, BaseGrid 활용

**Tasks**:
- [ ] GAMF-32-1: Dashboard.tsx 리팩토링 - 1h
- [ ] GAMF-32-2: WelcomeSection.tsx - 0.5h
- [ ] GAMF-32-3: MatchingStatusCard.tsx - 1.5h
- [ ] GAMF-32-4: ApplicationStatusCard.tsx - 1.5h
- [ ] GAMF-32-5: SavedSchoolsCard.tsx - 0.5h

**Definition of Done**:
- 대시보드 정상 렌더링
- 모든 카드 데이터 바인딩 확인
- Quick Action 동작 확인

---

### Story GAMF-33: 통합 테스트 (E2E)

**Story Type**: Task  
**Priority**: High  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 3  
**Labels**: `testing`, `e2e`, `week3`

**Description**:
전체 플로우 E2E 테스트 작성 및 실행

**User Story**:
```
AS A QA 엔지니어
I WANT 전체 사용자 플로우가 정상 동작하는지 확인하고
SO THAT 프로덕션 배포 전 품질을 보증할 수 있다
```

**Acceptance Criteria**:
- [ ] E2E 테스트 시나리오 작성
  1. 회원가입 → 로그인
  2. 프로필 입력 (3단계)
  3. 매칭 실행
  4. 결과 확인 (6대 지표)
  5. 학교 상세 보기
  6. 보관/지원
  7. 대시보드 확인
- [ ] 에러 시나리오 테스트
  - API 실패 시 에러 핸들링
  - 네트워크 에러 시 재시도
- [ ] 전체 플로우 E2E 테스트 통과
- [ ] 주요 사용자 플로우 정상 동작

**Tasks**:
- [ ] GAMF-33-1: E2E 테스트 시나리오 작성 - 1.5h
- [ ] GAMF-33-2: 에러 시나리오 테스트 - 0.5h

**Definition of Done**:
- E2E 테스트 통과
- 에러 시나리오 테스트 통과

---

### Story GAMF-34: 반응형 & 최적화

**Story Type**: Task  
**Priority**: Medium  
**Story Points**: 2 SP  
**Assignee**: Frontend Developer  
**Sprint**: Week 3  
**Labels**: `optimization`, `responsive`, `week3`

**Description**:
반응형 디자인 적용 및 성능 최적화

**User Story**:
```
AS A 모바일 사용자
I WANT 모바일에서도 편리하게 사용하고
SO THAT 언제 어디서나 서비스를 이용할 수 있다
```

**Acceptance Criteria**:
- [ ] 모바일 레이아웃 점검 (320px~768px)
- [ ] 태블릿 레이아웃 점검 (768px~1024px)
- [ ] 데스크톱 레이아웃 점검 (1024px~)
- [ ] Tailwind 반응형 클래스 적용 (sm:, md:, lg:)
- [ ] React.lazy로 코드 스플리팅
- [ ] 이미지 Lazy Loading
- [ ] Lighthouse 점검
  - Performance 점수 80+
  - Accessibility 점수 90+
- [ ] 번들 크기 < 500KB

**Tasks**:
- [ ] GAMF-34-1: 반응형 테스트 및 수정 - 1h
- [ ] GAMF-34-2: 성능 최적화 (코드 스플리팅, Lazy Loading) - 0.5h
- [ ] GAMF-34-3: Lighthouse 점검 및 개선 - 0.5h

**Definition of Done**:
- 모든 디바이스에서 정상 동작
- Lighthouse 점수 요구사항 충족
- 번들 크기 확인

---

## Sprint 요약

### Sprint 1 (Week 1): 인증 & 프로필 입력
- **Goal**: 사용자 인증 및 AI 매칭 필수 정보 수집
- **Stories**: GAMF-11 ~ GAMF-18
- **Total SP**: 24 SP
- **Deliverables**: 회원가입/로그인, 프로필 입력 (3단계), Mock API 연동

### Sprint 2 (Week 2): 매칭 결과 & API 연동
- **Goal**: AI 매칭 실행 및 결과 시각화
- **Stories**: GAMF-21 ~ GAMF-28
- **Total SP**: 24 SP
- **Deliverables**: 매칭 실행 UI, 6대 지표 Radar Chart, Top 5 학교 리스트, 학교 상세

### Sprint 3 (Week 3): 지원 관리 & 완료
- **Goal**: 지원 관리 기능 및 대시보드 완성, 최종 통합 테스트
- **Stories**: GAMF-31 ~ GAMF-34
- **Total SP**: 12 SP
- **Deliverables**: 지원하기 기능, 대시보드, E2E 테스트, 반응형 최적화

**총 예상 기간**: 2.5주 (60 SP, 하루 6시간 × 주 4일)

---

## 백엔드 협업 타임라인

| 시점 | 백엔드 상태 | 프론트엔드 작업 가능 범위 |
|------|-----------|---------------------|
| **Week 1 시작** | Mock API 제공 (GAM-11) | ✅ 전체 UI 개발 시작 (Mock 데이터) |
| **Week 1 종료** | User Profile API 완성 (GAM-22) | ✅ 프로필 입력 화면 실제 API 연동 |
| **Week 2 시작** | 매칭 엔진 완성 (GAM-31~33) | 매칭 로직 백엔드 완성 대기 |
| **Week 2 중간** | 매칭 API 완성 (GAM-42) | ✅ 실제 매칭 결과 연동 |
| **Week 2 종료** | 학교 데이터 20개 제공 (GAM-41) | ✅ 학교 상세 페이지 완성 |
| **Week 3 시작** | Application API 완성 (GAM-51) | ✅ 지원 관리 화면 연동 |
| **Week 3 종료** | Dashboard API 완성 (GAM-53) | ✅ 대시보드 완성 및 통합 테스트 |

---

## 백엔드 의존성 체크리스트

### Week 1 시작 전
- [ ] Mock API 제공 (GAM-11)
- [ ] Swagger UI 접근 가능 (http://localhost:8084/swagger-ui.html)
- [ ] CORS 설정 완료
- [ ] 04_FRONTEND_COOPERATION.md 문서 제공 (GAM-12)

### Week 1 종료 시점
- [ ] User Profile API 제공 (GAM-22)
  - `PUT /api/v1/user/profile`
  - `POST /api/v1/user/education`
  - `POST /api/v1/user/preference`
- [ ] JWT 인증 동작
- [ ] Validation 에러 응답 확인

### Week 2 중간
- [ ] 실제 매칭 API 제공 (GAM-42)
  - `POST /api/v1/matching/run`
  - `GET /api/v1/matching/result`
- [ ] 6대 지표 데이터 포함
- [ ] Explainable AI 설명 포함 (GAM-43)
- [ ] 학교 데이터 20개 제공 (GAM-41)

### Week 3 시작 전
- [ ] Application API 제공 (GAM-51)
  - `POST /api/v1/applications`
  - `GET /api/v1/applications`
  - `PATCH /api/v1/applications/{id}/status`
- [ ] Bookmark API 제공
  - `POST /api/v1/bookmarks`
  - `DELETE /api/v1/bookmarks/{id}`
- [ ] Dashboard API 제공 (GAM-53)
  - `GET /api/v1/dashboard`

---

## JIRA 등록 가이드

### Epic 생성
1. Project → Create Issue → Epic
2. Epic Name 입력 (예: "인증 & 프로필 입력")
3. Business Value 명시
4. Target Sprint 설정 (Week 1/2/3)
5. Total Story Points 입력

### Story 생성
1. Epic 하위에 Story 생성
2. Story Points 설정 (1, 2, 3, 4, 5, 6 SP)
3. User Story 형식 작성 ("AS A ... I WANT ... SO THAT ...")
4. Acceptance Criteria 체크박스 형식으로 명시
5. Tasks로 상세 작업 쪼개기 (GAMF-XX-1, GAMF-XX-2, ...)
6. Definition of Done 명시

### Labels 활용
- `week1`, `week2`, `week3`: 주차별 구분
- `authentication`, `profile`, `matching`, `school-detail`: 기능별 구분
- `core-feature`: MVP 핵심 기능
- `api`, `ui`, `testing`, `optimization`: 작업 유형
- `critical`, `high`, `medium`, `low`: 우선순위

### Workflow
1. **To Do**: 계획됨
2. **In Progress**: 진행 중
3. **Code Review**: 리뷰 요청
4. **Testing**: 테스트 중
5. **Done**: 완료

---

## 다음 단계 (Phase 2)

### 프론트엔드 확장 기능 백로그
- **GAMF-4**: 4개 프로그램 별 상세 페이지
- **GAMF-5**: 문서 표준화 (지원/비자 서류 뷰어)
- **GAMF-6**: PDF 생성 및 공유 기능
- **GAMF-7**: 미국 생활 정보 허브 (6개 카테고리)
- **GAMF-8**: 고급 검색 및 필터링
- **GAMF-9**: 실시간 챗봇 문의
- **GAMF-10**: 다국어 지원 (일본어, 중국어)
- **GAMF-11**: PWA (Progressive Web App)
- **GAMF-12**: 소셜 로그인 (Google/Naver/Kakao)

---

**문서 버전**: 1.0  
**마지막 업데이트**: 2026-01-26  
**작성자**: Go Almond Frontend Team  
**참고**: 백엔드 JIRA Backlog와 협업
