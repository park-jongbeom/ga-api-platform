# Go Almond - 프론트엔드 협업 요약

**작성일**: 2026-01-26  
**업데이트**: 실제 작업 시간 반영  
**미팅 안건**: AI 매칭 MVP 개발 협업 방안 논의

---

## 📌 핵심 제안 (TL;DR)

### 1. Mock API를 1주차에 제공 → 프론트는 즉시 작업 시작
```
Week 1: 백엔드가 Mock API 제공
Week 1~3: 프론트는 Mock으로 전체 UI 개발
Week 4: 실제 API로 전환 (설정 파일만 변경)
```

### 2. 병렬 개발로 6주 만에 완성
```
기존: 백엔드 완성(6주) → 프론트 시작(4주) = 총 10주
제안: 백엔드 + 프론트 병렬(6주) = 총 6주
```

### 3. API 명세는 처음부터 확정 → 중간 변경 최소화

### 4. CI/CD 자동 배포 활용 → 배포 시간 절약

---

## 🎯 백엔드 일정

| 주차 | 제공 내용 | 프론트 작업 가능 |
|-----|---------|----------------|
| **Week 1** | Mock API + Swagger | ✅ **전체 UI 개발 시작** |
| Week 2 | User Profile API | ✅ 프로필 화면 연동 |
| Week 3 | 매칭 엔진 (내부 로직) | Mock으로 계속 개발 |
| Week 4 | 매칭 API 완성 | ✅ **실제 매칭 연동** |
| Week 5 | Application + Document | ✅ 지원 관리 연동 |
| Week 6 | 보안 + 모니터링 | ✅ 통합 테스트 (CI/CD 자동 배포) |

**백엔드 작업 환경**:
- **가용 시간**: 월~목, 19:00~21:30 (일 2.5시간, 주 10시간)
- **실제 실적**: 31시간에 AI 컨설턴트 서비스 완성 ✅
- **배포**: Push 시 GitHub Actions 자동 배포

---

## 📡 주요 API (MVP 범위)

### 1. 인증 API (이미 완성)
```
POST /api/v1/auth/register  - 회원가입
POST /api/v1/auth/login     - 로그인
```

### 2. User Profile API (Week 2)
```
PUT  /api/v1/user/profile       - 기본 정보
POST /api/v1/user/education     - 학력 정보
POST /api/v1/user/preference    - 유학 목표
```

### 3. AI 매칭 API (Week 4) ⭐ 핵심
```
POST /api/v1/matching/run       - 매칭 실행
GET  /api/v1/matching/result    - 매칭 결과 조회
```

**매칭 결과 구조**:
```json
{
  "results": [
    {
      "rank": 1,
      "school": { "name": "Irvine Valley College", "..." },
      "total_score": 87.5,
      "score_breakdown": {
        "academic": 18,   // 학업 (20%)
        "english": 14,    // 영어 (15%)
        "budget": 15,     // 예산 (15%)
        "location": 10,   // 지역 (10%)
        "duration": 9,    // 기간 (10%)
        "career": 28      // 진로 (30%)
      },
      "recommendation_type": "safe",  // 안정권/도전권/전략
      "explanation": "이 학교는...",
      "pros": ["예산 여유", "OPT 가능"],
      "cons": ["경쟁률 높음"]
    }
    // ... Top 5
  ]
}
```

### 4. 학교/프로그램 API (Week 4)
```
GET /api/v1/programs?type={type}  - 프로그램 목록
GET /api/v1/schools/{id}          - 학교 상세
```

### 5. Application API (Week 5)
```
POST /api/v1/applications         - 지원 생성
GET  /api/v1/applications         - 지원 현황
```

### 6. Document API (Week 5)
```
POST /api/v1/documents/upload     - 파일 업로드 (multipart)
GET  /api/v1/documents            - 문서 리스트
```

### 7. Dashboard API (Week 5)
```
GET /api/v1/dashboard             - 대시보드 요약
```

---

## 🎨 프론트엔드 화면 구성 (기획서 기반)

### 1. 회원가입 / 정보 입력 (3단계)
- Step 1: 기본 정보 (이메일, 비밀번호)
- Step 2: 학력 정보 (학교, 성적, 영어 점수)
- Step 3: 목표 설정 (프로그램, 예산, 지역)

### 2. 메인 페이지 (로그인 후)
- 추천 요약 카드
- 프로그램 바로가기 (4개)
- 진행 중 지원 현황
- 인기 학교

### 3. AI 매칭 결과 페이지 ⭐ 핵심
- **Radar Chart**: 6대 지표 시각화
- **추천 카드**: Top 5 학교
- **설명**: 추천 사유, 장단점

### 4. 학교 상세 페이지
- 학교 소개, 학비, 프로그램
- 보관하기, 지원하기 버튼

### 5. 지원 관리 페이지
- 지원 현황 리스트
- 상태 (준비/제출/합격/거절)
- Progress Bar

### 6. 문서 페이지
- 문서 리스트
- 파일 업로드 (Drag & Drop)

### 7. My Page
- 내 정보, 지원 현황, 문서 관리

---

## 🔧 기술 스택 제안

### 백엔드 (확정)
```
- Kotlin + Spring Boot 3.4+
- PostgreSQL 17
- JWT 인증
- Swagger UI
- AWS Lightsail
```

### 프론트엔드 (권장)
```javascript
{
  "framework": "React 18 / Next.js 14",
  "language": "TypeScript",
  "state": "TanStack Query (React Query)",
  "styling": "Tailwind CSS",
  "charts": "Recharts (Radar Chart)",
  "forms": "React Hook Form + Zod",
  "http": "Fetch API / Axios"
}
```

### Cursor 협업 프롬프트 예시
```
나는 Go Almond 프론트엔드 개발자야.

@FRONTEND_COOPERATION_PROPOSAL.md 를 참고해서
AI 매칭 결과를 Radar Chart로 보여주는 React 컴포넌트를 만들어줘.

- 6대 지표 (학업/영어/예산/지역/기간/진로)
- Recharts 라이브러리 사용
- Tailwind CSS 스타일링
- TypeScript 타입 정의 포함
```

---

## 📦 Week 1 제공 사항 (Mock API)

### 1. Swagger UI
```
http://localhost:8084/swagger-ui.html
```
- 모든 API "Try it out" 가능
- 인증 토큰 설정 가능

### 2. Mock 데이터 (3가지 시나리오)
- **시나리오 A**: 고GPA + 충분한 예산 → University 추천
- **시나리오 B**: 중간GPA + 제한 예산 → CC 추천
- **시나리오 C**: 저GPA + 취업 목표 → Vocational 추천

### 3. TypeScript 타입 정의
```typescript
export interface MatchingResult {
  matching_id: string;
  total_matches: number;
  results: SchoolRecommendation[];
}

export interface SchoolRecommendation {
  rank: number;
  school: School;
  program: Program;
  total_score: number;
  score_breakdown: ScoreBreakdown;
  recommendation_type: 'safe' | 'challenge' | 'strategic';
  explanation: string;
  pros: string[];
  cons: string[];
}
```

### 4. React Query Hook 예시
```typescript
// hooks/useMatching.ts
import { useMutation } from '@tanstack/react-query';

export const useRunMatching = () => {
  return useMutation({
    mutationFn: async (userId: string) => {
      const response = await fetch(
        `${process.env.NEXT_PUBLIC_API_URL}/matching/run`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({ user_id: userId })
        }
      );
      return response.json();
    }
  });
};
```

---

## 🔄 Mock → 실제 API 전환 방법

### 환경변수만 변경
```bash
# .env.development (Week 1~3)
NEXT_PUBLIC_API_URL=http://localhost:8084/api/v1
NEXT_PUBLIC_USE_MOCK=true

# .env.production (Week 4~)
NEXT_PUBLIC_API_URL=https://api.goalmond.com/api/v1
NEXT_PUBLIC_USE_MOCK=false
```

### 코드 수정 불필요
API 엔드포인트와 응답 구조가 동일하므로, **설정 파일만 변경**하면 됩니다.

---

## 🎯 마일스톤

### Milestone 1: Mock API (Week 1 종료 - 2/2)
- ✅ Mock API 4개
- ✅ Swagger UI
- ✅ 협업 문서

**프론트 시작 가능**: 전체 UI 개발

---

### Milestone 2: User Profile (Week 2 종료 - 2/9)
- ✅ User Profile API
- ✅ DB 연동

**프론트 통합**: 프로필 입력 화면

---

### Milestone 3: AI 매칭 (Week 4 종료 - 2/23)
- ✅ 매칭 엔진 완성
- ✅ 실제 학교 데이터

**프론트 통합**: 실제 매칭 결과 연동

---

### Milestone 4: Application (Week 5 종료 - 3/2)
- ✅ Application + Document API

**프론트 통합**: 지원 관리 화면

---

### Milestone 5: 배포 (Week 6 종료 - 3/9)
- ✅ Lightsail 배포
- ✅ HTTPS 적용

**최종 테스트**: E2E, 성능, 모바일

---

## ❓ 논의 필요 사항

### 1. API 명세 검토
- [ ] 필요한 API가 모두 포함되었나요?
- [ ] 응답 구조가 프론트에서 사용하기 편한가요?
- [ ] 추가로 필요한 필드가 있나요?

### 2. 데이터 시각화
- [ ] Radar Chart 외 다른 차트가 필요한가요?
- [ ] 6대 지표 외 추가 지표가 필요한가요?

### 3. 일정
- [ ] Week 1 Mock API 제공 시점이 적절한가요?
- [ ] 프론트 개발 기간은 얼마나 예상하시나요?

### 4. 협업 방식
- [ ] Cursor AI를 사용하시나요?
- [ ] TypeScript를 사용하시나요?
- [ ] 선호하는 상태 관리 라이브러리가 있나요?

### 5. 디자인
- [ ] 디자인 시스템(Figma)이 준비되었나요?
- [ ] UI 컴포넌트 라이브러리를 사용하시나요? (예: shadcn/ui)

---

## 📞 다음 단계

### 미팅 후 진행 사항

#### 백엔드
1. 피드백 반영하여 API 명세 확정
2. Week 1 Mock API 개발 시작
3. Swagger UI 구축

#### 프론트엔드
1. 기술 스택 확정
2. 프로젝트 초기 설정
3. 컴포넌트 구조 설계

#### 협업
1. GitHub Repository 권한 설정
2. Slack 채널 생성 (#goalmond-dev)
3. 주간 싱크 미팅 일정 확정

---

## 📚 참고 문서

### 상세 문서
- **[FRONTEND_COOPERATION_PROPOSAL.md](FRONTEND_COOPERATION_PROPOSAL.md)**: API 명세, 데이터 구조 상세
- **[JIRA_BACKLOG.md](JIRA_BACKLOG.md)**: Epic & Story 목록
- **[04_FRONTEND_COOPERATION.md](04_FRONTEND_COOPERATION.md)**: 기존 협업 가이드

### 기획 문서
- **[docs/plan/Go Almond Api 명세서 (mvp).pdf](plan/Go Almond Api 명세서 (mvp).pdf)**: 원본 API 명세
- **[docs/plan/Go Almond – Ai Matching Logic Specification.pdf](plan/Go Almond – Ai Matching Logic Specification.pdf)**: AI 로직 설계
- **[docs/plan/Go Almond Db 인풋 데이터 & 프론트엔드 개발 기획서.pdf](plan/Go Almond Db 인풋 데이터 & 프론트엔드 개발 기획서.pdf)**: DB & 화면 기획

---

## 🚀 기대 효과

### 1. 개발 기간 단축
```
병렬 개발: 10주 → 6주 (40% 단축)
```

### 2. 리스크 감소
```
Week 1부터 프론트 개발 시작 → 늦은 통합 리스크 제거
```

### 3. 품질 향상
```
Mock API로 충분히 테스트 → 실제 통합 시 이슈 최소화
```

### 4. 효율적 협업
```
명확한 API 명세 → 백엔드-프론트 의존성 최소화
```

---

**이 요약서를 검토하신 후, 상세 문서([FRONTEND_COOPERATION_PROPOSAL.md](FRONTEND_COOPERATION_PROPOSAL.md))를 확인해주세요.**

**질문이나 피드백은 언제든지 환영합니다!** 📧

---

**작성**: Go Almond Backend Team  
**연락처**: backend@goalmond.com  
**버전**: 1.0 (2026-01-26)
