# 컬러링 제주 — 모바일 웹

제주에서 만난 색을 모아 무지개를 채우는 여행 기록.

---

## 시작하기 (5분)

### 1. 설치

```bash
npm install
```

> Node.js 가 없다면 [nodejs.org](https://nodejs.org) 에서 LTS 버전 먼저 설치.

### 2. 키 파일 만들기

```bash
cp .env.example .env.local
```

`.env.local` 을 열어 값 3가지를 채운다.

**① TourAPI 키** — 팀에 물어보기 (안드로이드 `local.properties` 에 있는 값과 같음)

```
TOUR_API_SERVICE_KEY=여기에
```

**② 카카오맵 키** — [카카오 개발자 콘솔](https://developers.kakao.com) → 내 애플리케이션 → 앱 키 → **JavaScript 키**

```
VITE_KAKAO_MAP_KEY=여기에
```

**③ Firebase** — 아래 명령으로 값을 꺼낸다

```bash
git show main:app/google-services.json
```

나온 내용에서 이렇게 옮겨 적는다.

| google-services.json | .env.local |
|---|---|
| `current_key` | `VITE_FIREBASE_API_KEY` |
| `project_id` | `VITE_FIREBASE_PROJECT_ID` |
| `project_id` + `.firebaseapp.com` | `VITE_FIREBASE_AUTH_DOMAIN` |
| `storage_bucket` | `VITE_FIREBASE_STORAGE_BUCKET` |
| `project_number` | `VITE_FIREBASE_MESSAGING_SENDER_ID` |

### 3. 카카오 콘솔에 주소 등록 ⚠️

```
카카오 개발자 콘솔 → 앱 설정 → 플랫폼 → Web → 사이트 도메인
    http://localhost:5173
```

**이걸 빼먹으면 지도 자리가 회색으로만 나온다. 에러도 안 뜬다.**

### 4. 실행

```bash
npm run dev
```

→ http://localhost:5173

---

## 안 될 때

| 증상 | 원인 |
|---|---|
| 지도가 회색 | 3번(도메인 등록) 안 함, 또는 카카오 키 틀림 |
| 장소 검색이 안 됨 | TourAPI 키 없음 |
| 로그인이 안 됨 | Firebase 값 없음 |
| 아무것도 안 뜸 | `.env.local` 을 만들고 **서버를 다시 켜야** 함 |

---

## 폰에서 보려면

카메라는 HTTPS 에서만 열린다. `http://192.168.x.x:5173` 로는 안 된다.

```bash
cloudflared tunnel --url http://localhost:5173
```

나온 `https://...` 주소를 폰에서 열면 된다. (그 주소도 3번처럼 카카오 콘솔에 등록해야 지도가 나온다.)

---

## 명령어

| | |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 빌드 (타입 검사 포함) |

## 폴더

```
src/
  components/   화면
  map/          카카오맵, 마커
  firebase/     로그인, 그룹
  store/        MY 지도, 조각모음 (localStorage)
  api/          TourAPI
  color/        사진에서 색 뽑기
  data/         추천 20곳
  styles/       색·글꼴 토큰
```

## 작업할 때 알아둘 것

- **TourAPI 는 브라우저에서 직접 못 부른다.** `vite.config.ts` 의 프록시를 거친다. 배포할 때는
  같은 역할의 서버리스 함수가 따로 필요하다 (안 만들면 장소 검색만 동작 안 함).
- **안드로이드 앱(`main` 브랜치)과 Firebase 를 공유한다.** 계정과 그룹 데이터가 양쪽에서 같이 보인다.
- **그룹의 `verifiedColor` 형식이 서로 다르다.** 안드로이드는 정수(`-1543350`), 웹은 문자열
  (`"#e8734a"`). 웹은 양쪽 다 읽도록 해뒀지만 언젠가 통일하는 게 좋다.
