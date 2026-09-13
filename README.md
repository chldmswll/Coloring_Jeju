# 컬러링 제주 — 모바일 웹

제주에서 만난 색을 모아 무지개를 채우는 여행 기록. 안드로이드 앱(이 저장소 `main` 브랜치)과 **같은 Firebase
프로젝트**를 쓰므로 계정과 그룹 데이터가 양쪽에서 공유된다.

## 처음 받았다면

### 1. Node.js 설치 확인

```bash
node --version   # v20 이상
```

없으면 https://nodejs.org 에서 LTS 버전 설치.

### 2. 패키지 설치

```bash
npm install
```

### 3. `.env.local` 만들기 ⚠️ 이걸 안 하면 아무것도 안 된다

키는 저장소에 올라가지 않는다. `.env.example` 을 복사해서 값을 채운다.

```bash
cp .env.example .env.local
```

| 항목 | 어디서 구하나 | 없으면 |
|---|---|---|
| `TOUR_API_SERVICE_KEY` | 안드로이드 `local.properties` 의 `tour.api.service.key` 와 같은 값. 원본은 [공공데이터포털](https://www.data.go.kr) | 장소 검색이 안 됨 |
| `VITE_KAKAO_MAP_KEY` | [카카오 개발자 콘솔](https://developers.kakao.com) → 앱 키 → **JavaScript 키** | **지도가 회색 화면** |
| `VITE_FIREBASE_*` | 이 저장소 `main` 브랜치의 `app/google-services.json` (아래 대응표 참고) | 로그인·그룹이 안 됨 |

`google-services.json` 은 안드로이드 앱 쪽 파일이라 이 브랜치에는 없다. `main` 브랜치에서 열어보거나
(`git show main:app/google-services.json`) 팀에 요청할 것. 대응은 다음과 같다:

```
project_id      →  VITE_FIREBASE_PROJECT_ID
                   VITE_FIREBASE_AUTH_DOMAIN = <project_id>.firebaseapp.com
project_number  →  VITE_FIREBASE_MESSAGING_SENDER_ID
storage_bucket  →  VITE_FIREBASE_STORAGE_BUCKET
current_key     →  VITE_FIREBASE_API_KEY
```

> `appId` 는 `google-services.json` 에 안드로이드용밖에 없지만, Analytics 에만 쓰이는 값이라
> Auth/Firestore 동작에는 필요 없다.

### 4. 카카오 콘솔에 도메인 등록 ⚠️ 사람마다 한 번씩

```
카카오 개발자 콘솔 → 앱 설정 → 플랫폼 → Web → 사이트 도메인
  http://localhost:5173
```

**등록이 안 되어 있으면 에러 메시지 없이 지도 자리가 회색으로만 나온다.** 지도가 안 보이면
여기부터 의심할 것. (한 번 등록해 두면 팀원 전체가 쓸 수 있다. 안드로이드처럼 PC 마다
키 해시를 따로 등록할 필요는 없다.)

### 5. 실행

```bash
npm run dev
```

→ http://localhost:5173

## 폰에서 테스트하려면

**카메라는 HTTPS 에서만 열린다.** PC 의 `localhost` 는 예외라 되지만, 폰에서
`http://192.168.x.x:5173` 로 접속하면 카메라가 차단된다. 임시 HTTPS 주소를 만들어 쓴다:

```bash
cloudflared tunnel --url http://localhost:5173
```

출력된 `https://xxx.trycloudflare.com` 을 폰에서 열면 카메라까지 정상 동작한다.
(그 주소도 카카오 콘솔 사이트 도메인에 추가해야 지도가 뜬다.)

## 명령어

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 타입 검사 + 프로덕션 빌드 |
| `npm run preview` | 빌드 결과 미리보기 |

## 구조

```
src/
  api/tourApi.ts          TourAPI 호출 (프록시 경유)
  color/extractColors.ts  사진에서 대표 색 추출
  components/             화면 단위 컴포넌트
  data/                   추천 20곳 (안드로이드 assets 와 동일)
  firebase/               Auth · Firestore(그룹)
  map/                    카카오맵 · 마커 클러스터링
  store/                  MY 지도 · 조각모음 (localStorage)
  styles/tokens.css       디자인 토큰 (Compose ui/theme 와 동일)
```

## 알아두면 좋은 것

**TourAPI 는 브라우저에서 직접 못 부른다.** Origin 헤더가 붙은 요청에 403 을 주고 CORS 헤더도
없다. 그래서 모든 호출이 `/api/tour/*` 를 거치며, 개발 중에는 `vite.config.ts` 의 프록시가
대신 호출한다. **배포할 때는 같은 역할을 하는 서버리스 함수가 따로 필요하다** — 지금 그대로
배포하면 장소 검색만 동작하지 않는다.

**그룹의 `verifiedColor` 형식이 두 클라이언트에서 다르다.** 안드로이드는 ARGB 정수
(`-1543350`), 웹은 문자열(`"#e8734a"`). 같은 Firestore 를 보므로 읽을 때는 양쪽을 모두
받아 문자열로 맞추고 있다. 언젠가 한쪽으로 통일하는 게 좋다.
