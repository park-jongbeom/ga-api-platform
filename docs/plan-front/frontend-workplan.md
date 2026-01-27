# GO ALMOND 프론트엔드 작업 일정 및 내용 정리 (전체 계획)

> **⚠️ 참고**: 이 문서는 **전체 기능 계획 (16~17주)**입니다.  
> **MVP 핵심 기능만 필요한 경우** → [`frontend-mvp-plan.md`](./frontend-mvp-plan.md) (6주 계획) 참고

## 📋 문서 개요
- **작성일**: 2026-01-26
- **프로젝트**: GO ALMOND (미국 유학 통합 플랫폼)
- **범위**: 웹 프론트엔드 **전체 기능** (앱은 별도 일정)
- **산정 기준**: 1명 기준, 하루 6시간, 주 4일 (월~목) = 주당 24시간

## 📊 계획 비교

| 구분 | 전체 계획 (이 문서) | MVP 계획 |
|------|------------------|----------|
| 기간 | **13주** | 2.5주 |
| 총 시간 | 약 308시간 | 60시간 |
| 작업 방식 | 하루 6h × 주 4일 | 하루 6h × 주 4일 |
| 페이지 수 | 20개 이상 | 8개 |
| 핵심 기능 | 모든 기능 포함 | AI 매칭 중심 |
| 추가 라이브러리 | 10개 | 2개 |
| 적용 시점 | Phase 2 이후 | **즉시 시작 (MVP)** |

**권장**: 먼저 [`frontend-mvp-plan.md`](./frontend-mvp-plan.md)로 MVP 구축 후, 이 문서로 확장

---

## 🎯 서비스 개요

### 서비스 비전
**Going to America to Learn and Make One's Dream come true!**  
(배우고 꿈을 이루기 위해 미국으로 간다!)

### 핵심 가치
- 미국 유학 전 과정(상담-검색-신청-입학-비자-이주-정착-진로)을 하나의 플랫폼에서 통합 제공
- 직업학교/커뮤니티 칼리지/초등 참관 프로그램 등 새로운 유학 시장 개척
- 공적 문서 표준화 제공 (무료 서비스)
- AI 기반 맞춤 추천으로 진로 불확실성 해소
- 이주 후 정착 생활 서비스 지원

### 서비스 대상
- 아시아권 미국 유학 준비자 (한국, 일본, 대만, 베트남, 중국, 인도 등)
- 일반 대학/대학원 진학 희망자
- 커뮤니티 컬리지 편입 전략 활용자
- 직업학교 진학 희망자 (고등학생, 대학생, 일반인)
- 초등학교 단기 참관 프로그램 참여 가족

---

## 🎨 디자인 시스템

### 컬러 팔레트
- **Primary**: 밝은 녹색 (브랜드 메인)
- **Neutral**: 밝은 회색, 흰색 (배경/텍스트)
- **Accent**: 오렌지색 (강조/CTA)

### UI 컴포넌트 (기구축 완료)
- Typography: `BaseHeading`, `BaseText`
- Layout: `BaseSection`, `BaseContainer`, `BaseGrid`
- Input: `BaseInput`, `BaseTextarea`
- Button: `BaseButton`, `BaseLinkButton`
- Card: `BaseCard`
- Badge: `BaseBadge`
- Navigation: `BaseNavLink`

---

## 📐 화면 구조 및 라우팅

### 1. 인증 전 (Public)
```
/                         → 랜딩 페이지 (서비스 소개)
/signup                   → 회원가입 (약관 동의 → 정보 입력)
/login                    → 로그인
```

### 2. 인증 후 (Protected)
```
/dashboard                → 메인 대시보드 (맞춤형)
/programs/vocational      → 직업학교 프로그램
/programs/community       → 커뮤니티 컬리지 프로그램
/programs/university      → 일반 대학/대학원 프로그램
/programs/elementary      → 초등학교 참관 프로그램
/school/:id               → 학교 상세 (보관/지원/문의)
/recommendations          → AI 맞춤 추천 리스트
/recommendations/:id      → 추천 상세 + 리포트 (PDF/공유)
/documents/application    → 지원 서류 표준화
/documents/visa           → 비자 서류 표준화
/search                   → 통합 검색 (학교/전공/직업/지역)
/us-life                  → 미국 생활 정보 허브
/us-life/housing          → 숙소 검색/신청 가이드
/us-life/license          → 운전면허 취득 안내
/us-life/vehicle          → 차량 구매/렌트 정보
/us-life/insurance        → 자동차/건강 보험 가입
/us-life/opt              → OPT 및 취업 정보
/my                       → 마이페이지 (정보 수정)
/my/applications          → 내 지원 현황
/my/saved                 → 보관한 학교/프로그램
/contact                  → 문의하기 (이메일/챗봇)
```

---

## 📅 전체 일정 요약

| 단계 | 작업 내용 | 예상 시간 | 기간 | 누적 |
|------|-----------|----------|------|------|
| Phase 0 | 설계/기반 구축 | 20h | 1주 | 1주 |
| Phase 1 | 인증 전 페이지 (랜딩/가입/로그인) | 40h | 1.7주 | 2.7주 |
| Phase 2 | 필수 정보 입력 시스템 | 40h | 1.7주 | 4.4주 |
| Phase 3 | AI 추천/맞춤 리포트 | 40h | 1.7주 | 6.1주 |
| Phase 4 | 4개 핵심 프로그램 페이지 | 36h | 1.5주 | 7.6주 |
| Phase 5 | 검색 및 학교 상세 | 28h | 1.2주 | 8.8주 |
| Phase 6 | 문서 표준화 (지원/비자) | 28h | 1.2주 | 10주 |
| Phase 7 | 미국 생활 정보 허브 | 20h | 1주 | 11주 |
| Phase 8 | 로그인 후 메인/마이페이지 | 40h | 1.7주 | 12.7주 |
| Phase 9 | QA/성능/접근성 | 16h | 0.7주 | 13.4주 |

**총 예상 시간: 약 308시간**  
**총 예상 기간: 13주 (하루 6시간 × 주 4일 = 주당 24시간)**  
**기존 계획 대비: 16~17주 → 13주 (작업 시간 증가로 기간 단축)**

---

## 🔧 단계별 상세 작업 내용

### Phase 0. 설계/기반 구축 (1주)

#### 작업 내용
1. **정보 아키텍처 설계**
   - 라우팅 구조 확정 (인증 전/후 분리)
   - 페이지 간 데이터 흐름 정의
   - 상태 관리 전략 수립 (Context API vs Zustand)

2. **디자인 토큰 적용**
   - Tailwind Config 확장 (브랜드 컬러)
   - Typography 스케일 정의
   - Spacing/Shadow/Radius 규칙

3. **공통 레이아웃 구축**
   - `Layout.tsx` (Header/Footer/Navigation)
   - `ProtectedRoute.tsx` (인증 체크)
   - `ErrorBoundary.tsx` (에러 처리)

4. **API 클라이언트 설정**
   - Axios 인스턴스 설정 (이미 구축됨)
   - API 엔드포인트 정의
   - 에러 핸들링 표준화

#### 산출물
- 라우팅 맵 문서
- 디자인 토큰 가이드
- 공통 레이아웃 컴포넌트

---

### Phase 1. 인증 전 페이지 (2주)

#### 1.1 랜딩 페이지 (`/`) - 3일
- **섹션 구성**
  - Hero: 서비스 비전 + CTA (회원가입/로그인)
  - 서비스 소개 (4개 핵심 프로그램 카드)
  - 서비스 대상 (타겟 사용자별 혜택)
  - 성공 사례 (인기 학교/전공/프로그램)
  - Footer (Contact Us)

- **주요 컴포넌트**
  - `HeroSection.tsx`
  - `ProgramCard.tsx` (4개 프로그램)
  - `TargetAudienceSection.tsx`
  - `SuccessStoryCarousel.tsx`

#### 1.2 회원가입 (`/signup`) - 4일
- **단계별 플로우**
  1. 서비스 소개 (슬라이드)
  2. 약관 동의 (미국 정부 지침 준수)
  3. 기본 정보 입력 (이메일/비밀번호)
  4. 가입 완료 → 필수 정보 입력 안내

- **주요 컴포넌트**
  - `SignupWizard.tsx` (단계 관리)
  - `TermsAgreement.tsx` (약관 동의)
  - `BasicInfoForm.tsx` (기본 정보)

#### 1.3 로그인 (`/login`) - 2일
- 이메일/비밀번호 로그인
- OAuth (선택: Google/Naver/Kakao)
- 비밀번호 찾기 링크
- 회원가입 유도

#### 1.4 통합 테스트 - 1일
- 인증 플로우 검증
- 에러 처리 확인
- 반응형 테스트

---

### Phase 2. 필수 정보 입력 시스템 (2주)

#### 2.1 데이터 모델 설계 - 1일
- **학교 정보**
  - 출신학교 유형 (검정고시/초등/중학/고등/대학/대학원)
  - 학교명, 지역, 내신 성적, 영어 평가
  - 생기부 일부 내용 (선택)

- **개인 정보**
  - 생년월일, MBTI, 성향
  - 가족관계, 생활 환경
  - 자기소개 (선택)

- **희망 유학 케이스**
  - 지원 프로그램 (Vocational/Community/University/Elementary)
  - 희망 학교/전공/직업
  - 연간 예산 범위 (학비 + 생활비)
  - 희망 지역 (주/도시)
  - 학업 기간, 졸업 후 체류 의사

#### 2.2 입력 폼 구현 - 6일
- **Form Wizard 구조**
  - Step 1: 학교 정보 (2일)
  - Step 2: 개인 정보 (1.5일)
  - Step 3: 희망 유학 케이스 (2일)
  - Progress Bar + 단계별 저장

- **주요 컴포넌트**
  - `UserProfileWizard.tsx`
  - `SchoolInfoForm.tsx`
  - `PersonalInfoForm.tsx`
  - `StudyPreferenceForm.tsx`
  - `AutocompleteSchool.tsx` (학교 검색)
  - `LocationSelector.tsx` (지역 선택)

#### 2.3 유효성 검사 및 저장 - 2일
- Zod 스키마 정의
- 실시간 유효성 검사
- 임시 저장 기능 (LocalStorage)
- 서버 저장 + 에러 처리

#### 2.4 통합 테스트 - 1일

---

### Phase 3. AI 추천/맞춤 리포트 (2주)

#### 3.1 매칭 알고리즘 UI - 3일
- **매칭 지표 시각화**
  - 전체 매칭률 (퍼센트 게이지)
  - 6개 세부 지표 (학교/전공/예산/기간/진로/지역)
  - Radar Chart 또는 Bar Chart

- **주요 컴포넌트**
  - `MatchingScoreCard.tsx`
  - `MatchingRadarChart.tsx` (Chart.js/Recharts)

#### 3.2 추천 리스트 페이지 (`/recommendations`) - 3일
- 추천 프로그램 카드 리스트
- 필터링 (프로그램 유형/예산/지역)
- 정렬 (매칭률/학비/인기도)
- 페이지네이션

- **주요 컴포넌트**
  - `RecommendationList.tsx`
  - `RecommendationCard.tsx`
  - `RecommendationFilter.tsx`

#### 3.3 추천 상세 + 리포트 (`/recommendations/:id`) - 4일
- **상세 정보**
  - 학교 소개 (역사/랭킹/졸업 후 진로)
  - 전공/직업 교육 소개
  - 학비, 생활비, 기숙사, 식당
  - 주변 환경 (지도 연동)

- **리포트 출력**
  - 페이지 내 뷰
  - PDF 다운로드 (react-pdf 또는 서버 생성)
  - 공유 (이메일/메신저/SNS) 모달

- **주요 컴포넌트**
  - `RecommendationDetail.tsx`
  - `ReportViewer.tsx`
  - `ReportExport.tsx` (PDF)
  - `ShareModal.tsx`

#### 3.4 통합 테스트 - 2일

---

### Phase 4. 4개 핵심 프로그램 페이지 (2주)

#### 4.1 프로그램 공통 레이아웃 - 1일
- `ProgramLayout.tsx` (공통 구조)
- 프로그램 소개 섹션
- 학교 리스트/필터링/정렬

#### 4.2 직업학교 (`/programs/vocational`) - 2일
- 직업학교 소개
- 준학사 과정 (A.A/A.S) 설명
- OPT 취업 연계 정보
- 학교 리스트 (필터: 직업 분야)

#### 4.3 커뮤니티 컬리지 (`/programs/community`) - 2일
- 편입 전략 설명
- 편입 가능 대학 매칭
- 학점 인정 클라스 정보
- 학교 리스트 (필터: 편입 대학)

#### 4.4 일반 대학/대학원 (`/programs/university`) - 2일
- 4년제 대학/대학원 소개
- 입학 요건 분석
- 학교 리스트 (필터: 전공/랭킹)

#### 4.5 초등학교 참관 (`/programs/elementary`) - 2일
- 프로그램 소개 (단기 참관)
- 학교/교사/시설 현황
- 학부모 가이드
- 신청 절차

#### 4.6 통합 테스트 - 1일

---

### Phase 5. 검색 및 학교 상세 (1.5주)

#### 5.1 통합 검색 (`/search`) - 3일
- **검색 기능**
  - 학교/전공/직업/지역 통합 검색
  - 자동완성 (Autocomplete)
  - 최근 검색어 저장

- **결과 표시**
  - 카테고리별 결과 탭 (학교/전공/직업/지역)
  - 결과 리스트 (카드 형식)
  - 페이지네이션

- **주요 컴포넌트**
  - `SearchBar.tsx`
  - `SearchResults.tsx`
  - `SearchResultCard.tsx`

#### 5.2 학교 상세 (`/school/:id`) - 4일
- **상세 정보**
  - 학교 기본 정보 (역사/랭킹/전공)
  - 학비, 기숙사, 식당, 시설
  - 교수진, 클라스 정보
  - 지역 환경 (지도)
  - 졸업 후 진로 데이터

- **사용자 액션**
  - 보관하기 (Bookmark)
  - 지원하기 (Apply)
  - 문의하기 (Contact)

- **주요 컴포넌트**
  - `SchoolDetail.tsx`
  - `SchoolInfo.tsx`
  - `SchoolActionBar.tsx` (보관/지원/문의)

#### 5.3 통합 테스트 - 1일

---

### Phase 6. 문서 표준화 (1.5주)

#### 6.1 지원 서류 표준화 (`/documents/application`) - 3일
- **기능**
  - 사용자 정보 기반 자동 작성
  - 학교별 요구 서류 체크리스트
  - 서류 업로드 (파일 관리)
  - 페이지 내 뷰 + PDF 다운로드

- **주요 컴포넌트**
  - `ApplicationDocuments.tsx`
  - `DocumentViewer.tsx`
  - `DocumentUploader.tsx`

#### 6.2 비자 서류 표준화 (`/documents/visa`) - 3일
- **기능**
  - F1 비자 인터뷰 서류 목록
  - 재정 증명서, 가족 관계 증명서 템플릿
  - 서류 업로드/다운로드
  - 페이지 내 뷰 + PDF 다운로드

- **주요 컴포넌트**
  - `VisaDocuments.tsx`
  - `VisaDocumentChecklist.tsx`

#### 6.3 통합 테스트 - 1일

---

### Phase 7. 미국 생활 정보 허브 (1주)

#### 7.1 생활 정보 메인 (`/us-life`) - 1일
- 6개 카테고리 카드
- 각 카테고리 요약 정보

#### 7.2 각 카테고리 상세 페이지 - 3일
- `/us-life/housing`: 숙소 검색/신청 가이드
- `/us-life/license`: 운전면허 취득 안내
- `/us-life/vehicle`: 차량 구매/렌트 정보
- `/us-life/insurance`: 자동차/건강 보험
- `/us-life/opt`: OPT 및 취업 정보

- **공통 구조**
  - 가이드 콘텐츠 (마크다운 렌더링)
  - 외부 링크 (파트너사)
  - 문의/신청 버튼

#### 7.3 통합 테스트 - 1일

---

### Phase 8. 로그인 후 메인/마이페이지 (2주)

#### 8.1 메인 대시보드 (`/dashboard`) - 5일
- **섹션 구성**
  1. Welcome + 사용자 이름
  2. 4개 핵심 프로그램 요약 카드
  3. 진행 중 신청 현황 (있을 경우)
     - 진행률 표시 (Progress Bar)
     - 간략한 개요
  4. AI 맞춤 추천 프로그램 (최대 6개)
  5. 인기 학교/전공/프로그램 (실시간)
  6. 미국 생활 정보 퀵링크

- **주요 컴포넌트**
  - `Dashboard.tsx`
  - `WelcomeSection.tsx`
  - `ProgramSummaryCards.tsx`
  - `ApplicationStatusCard.tsx`
  - `RecommendationCarousel.tsx`
  - `TrendingSection.tsx`

#### 8.2 마이페이지 (`/my`) - 2일
- **정보 수정**
  - 필수 정보 재입력 (Phase 2 폼 재사용)
  - 프로필 사진 업로드
  - 비밀번호 변경

- **주요 컴포넌트**
  - `MyProfile.tsx`
  - `ProfileEdit.tsx`

#### 8.3 지원 현황 (`/my/applications`) - 2일
- 지원한 학교/프로그램 리스트
- 상태별 필터 (진행중/완료/거절)
- 상세 보기 → 학교 상세 페이지 연결

#### 8.4 보관함 (`/my/saved`) - 1일
- 보관한 학교/프로그램 리스트
- 삭제 기능

#### 8.5 문의하기 (`/contact`) - 1일
- 이메일 문의 폼
- 챗봇 연동 (선택)

#### 8.6 통합 테스트 - 1일

---

### Phase 9. QA/성능/접근성 (1~2주)

#### 9.1 반응형 테스트 - 2일
- 모바일/태블릿/데스크톱 레이아웃 점검
- 터치 인터랙션 확인

#### 9.2 접근성 (a11y) - 2일
- ARIA 레이블 추가
- 키보드 내비게이션 테스트
- 색상 대비 확인
- 스크린 리더 호환성

#### 9.3 성능 최적화 - 2일
- 이미지 최적화 (WebP, Lazy Loading)
- 코드 스플리팅 (React.lazy)
- 번들 크기 분석
- Lighthouse 점수 개선

#### 9.4 에러 처리 강화 - 1일
- ErrorBoundary 적용
- API 에러 핸들링
- 사용자 피드백 개선

#### 9.5 보안 점검 - 1일
- XSS/CSRF 방어 확인
- 입력 검증 강화
- 의존성 취약점 점검

#### 9.6 최종 통합 테스트 - 2일

---

## 📊 페이지별 작업 시간 요약

| 페이지/기능 | 예상 시간 | 우선순위 |
|------------|----------|---------|
| 랜딩 페이지 | 3일 | 높음 |
| 회원가입/로그인 | 6일 | 높음 |
| 필수 정보 입력 | 10일 | 높음 |
| AI 추천/리포트 | 10일 | 높음 |
| 4개 프로그램 페이지 | 9일 | 중간 |
| 검색/학교 상세 | 7일 | 중간 |
| 문서 표준화 | 7일 | 중간 |
| 미국 생활 정보 | 5일 | 낮음 |
| 대시보드/마이페이지 | 10일 | 높음 |
| QA/성능/접근성 | 10일 | 높음 |

**총 작업일: 77일 (약 15.4주)**

---

## 🛠️ 기술 스택 (현재 프로젝트 기준)

### 프론트엔드
- **Framework**: React 18 + TypeScript
- **Build**: Vite
- **Styling**: Tailwind CSS
- **Routing**: React Router v6
- **State**: Context API + Hooks
- **HTTP**: Axios (인터셉터 설정 완료)
- **Validation**: Zod
- **Testing**: Vitest + React Testing Library

### 추가 필요 라이브러리
- **Chart**: Recharts 또는 Chart.js (매칭 지표 시각화)
- **PDF**: react-pdf 또는 서버 생성
- **Map**: Google Maps API 또는 Mapbox (지역 환경)
- **Form**: React Hook Form (복잡한 폼 관리)
- **File Upload**: react-dropzone
- **Carousel**: Swiper 또는 React Slick
- **Markdown**: react-markdown (생활 정보 가이드)

---

## ⚠️ 리스크 및 의존성

### 백엔드 의존성
- **필수 API 스펙**
  - 사용자 정보 저장/조회
  - AI 추천 알고리즘 (매칭 로직)
  - 학교/전공/직업 데이터베이스
  - 문서 템플릿 생성
  - 검색 엔진 (Elasticsearch 등)

- **지연 리스크**
  - API 스펙 미확정 시 프론트엔드 작업 블로킹
  - Mock 데이터로 선행 개발 가능하지만 최종 연동 시 수정 필요

### 기능 구현 복잡도
- **PDF 생성**: 서버 생성 vs 클라이언트 생성 결정 필요
- **공유 기능**: 이메일/메신저/SNS API 연동 (백엔드 협의)
- **챗봇**: 외부 서비스 연동 (선택 기능)
- **지도 연동**: Google Maps API 키 발급 필요

### 디자인 확정
- **디자인 시스템**: 상세 디자인 가이드 필요
- **컴포넌트 갤러리**: Storybook 도입 고려
- **이미지/아이콘**: 에셋 준비 (학교 로고, 아이콘 등)

---

## 🎯 다음 단계 (Action Items)

### 즉시 진행
1. **백엔드 API 스펙 정의 회의**
   - 필수 엔드포인트 목록 작성
   - 데이터 스키마 확정
   - Mock API 서버 구축 (선택)

2. **디자인 시스템 확정**
   - 컬러 팔레트 상세화 (Hex 코드)
   - Typography 스케일
   - 컴포넌트 스타일 가이드

3. **외부 서비스 계약**
   - Google Maps API 키
   - PDF 생성 서비스 (선택)
   - 챗봇 서비스 (선택)

### Phase 0 시작 전 준비
- 라우팅 맵 최종 확정
- 백엔드 API 스펙 1차 버전 완료
- 디자인 토큰 Tailwind Config 적용

---

## 📝 변경 이력
- **2026-01-26**: 초기 작성 (상세 일정 및 내용 정리)
