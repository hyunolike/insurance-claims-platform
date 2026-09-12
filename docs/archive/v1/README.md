# Insurance Claims - Client UI

보험금 청구 자동화 시스템의 클라이언트 PC 인터페이스입니다.

## Design Philosophy

이 UI는 **Editorial/Fashion 디자인 철학**을 기반으로 제작되었습니다:

- **Typography-driven**: 큰 타이포그래피가 주요 디자인 요소
- **Monochrome palette**: 흑백 기반의 차분한 색상 팔레트
- **Generous whitespace**: 의도적인 여백과 공간 활용
- **Minimal & Confident**: 불필요한 요소 제거, 자신감 있는 UI
- **Flat & Unboxed**: 박스나 테두리 없는 플랫한 컴포넌트

전문적인 보험 업무 환경에 프리미엄하고 차분한 경험을 제공합니다.

## Features

### 1. Dashboard (대시보드)
- 모든 청구 내역 조회
- 상태별 필터링 (전체, 제출됨, 검토중, 승인됨, 거절됨, 지급완료)
- 청구 카드 클릭으로 상세 페이지 이동

### 2. Create Claim (청구 생성)
- 새로운 보험금 청구 제출
- 폼 필드:
  - 보험계약번호
  - 청구금액
  - 발생일자
  - 청구사유
- 제출 성공 시 청구번호 표시

### 3. Claim Detail (청구 상세)
- 특정 청구의 상세 정보 조회
- 진행 상태 타임라인
- 청구번호, 계약번호, 금액, 날짜, 사유 등 전체 정보 표시

## Tech Stack

- **HTML5**: Semantic markup
- **CSS3**: Custom properties, Grid, Flexbox
- **Vanilla JavaScript**: No framework dependencies
- **Fetch API**: Backend API communication

## File Structure

```
client-ui/
├── index.html          # Dashboard (청구 목록)
├── create.html         # Create claim form (청구 생성)
├── detail.html         # Claim detail view (청구 상세)
├── css/
│   └── style.css       # Editorial design system
├── js/
│   ├── app.js          # Shared utilities & API client
│   ├── dashboard.js    # Dashboard functionality
│   ├── create.js       # Create form handling
│   └── detail.js       # Detail view rendering
└── assets/             # Images, icons (if needed)
```

## Setup & Usage

### 1. Start Backend Server

먼저 Spring Boot 백엔드 서버를 실행하세요:

```bash
cd /Users/hyuno/dev/toss
./gradlew bootRun
```

백엔드는 `http://localhost:8080`에서 실행됩니다.

### 2. Serve Frontend

정적 파일 서버로 클라이언트 UI를 실행하세요:

**Option A: Python (Python 3)**
```bash
cd client-ui
python3 -m http.server 3000
```

**Option B: Node.js (npx)**
```bash
cd client-ui
npx serve -p 3000
```

**Option C: VS Code Live Server**
- VS Code에서 `index.html` 열기
- 우클릭 → "Open with Live Server"

### 3. Access Application

브라우저에서 `http://localhost:3000` 접속

## API Integration

현재 JavaScript 파일들은 **Mock Data**를 사용합니다. 백엔드 API가 준비되면 다음과 같이 변경하세요:

### app.js
```javascript
const API_BASE_URL = 'http://localhost:8080'; // Already configured
```

### dashboard.js
```javascript
// Replace mock data with actual API call
const claims = await api.get('/claims'); // Uncomment this line
```

### create.js
```javascript
// Already configured to POST to /claims endpoint
const response = await api.post('/claims', claimData);
```

### detail.js
```javascript
// Replace mock data with actual API call
const claim = await api.get(`/claims/${claimId}`); // Uncomment this line
```

## Design Tokens

### Colors
```css
--color-black: #0a0a0a;
--color-charcoal: #2a2a2a;
--color-gray: #6b6b6b;
--color-light-gray: #d4d4d4;
--color-off-white: #f5f5f5;
--color-white: #ffffff;
--color-accent: #3a5a6a;
```

### Typography Scale
```css
--font-size-xs: 0.75rem;    /* 12px */
--font-size-sm: 0.875rem;   /* 14px */
--font-size-base: 1rem;     /* 16px */
--font-size-lg: 1.125rem;   /* 18px */
--font-size-xl: 1.5rem;     /* 24px */
--font-size-2xl: 2rem;      /* 32px */
--font-size-3xl: 3rem;      /* 48px */
--font-size-4xl: 4rem;      /* 64px */
```

### Spacing Scale
```css
--space-xs: 0.5rem;   /* 8px */
--space-sm: 1rem;     /* 16px */
--space-md: 1.5rem;   /* 24px */
--space-lg: 2rem;     /* 32px */
--space-xl: 3rem;     /* 48px */
--space-2xl: 4rem;    /* 64px */
--space-3xl: 6rem;    /* 96px */
```

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)

Modern browsers with ES6+ and CSS Grid support required.

## Future Enhancements

- [ ] Real-time status updates (WebSocket)
- [ ] File upload for claim documents
- [ ] Advanced filtering and search
- [ ] Export to PDF
- [ ] Dark mode support
- [ ] Notification system
- [ ] Pagination for large claim lists

## Notes

- 현재 Mock Data를 사용하여 독립적으로 테스트 가능
- 백엔드 API 연동 시 주석 처리된 API 호출 활성화 필요
- CORS 설정이 백엔드에서 필요할 수 있음 (localhost:3000 허용)

---

**Last Updated**: 2026-01-08
