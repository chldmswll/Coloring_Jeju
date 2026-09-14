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
  components/   화면 (AlbumTab — 여행 만들기/참여 포함)
  map/          카카오맵, 마커
  firebase/     로그인, 여행(trips) 데이터
  api/          TourAPI
  color/        사진에서 색 뽑기
  data/         추천 20곳
  styles/       색·글꼴 토큰
```

---

# 헤매지 않으려면 알아둘 것

## 장소 검색(TourAPI)은 왜 이렇게 돼 있나

브라우저에서 TourAPI 를 **직접 부르면 무조건 실패한다.** 공공데이터포털이 브라우저에서 오는
요청을 막아놨기 때문이다 (403 을 돌려준다). 안드로이드 앱은 이 제한이 없어서 그냥 됐다.

그래서 웹은 **중간에 누가 대신 불러줘야 한다.**

```
브라우저  →  대신 불러주는 서버  →  TourAPI
```

**개발 중에는 이미 해결돼 있다.** `npm run dev` 로 띄운 개발 서버가 그 역할을 한다
(`vite.config.ts` 에 설정돼 있음). 코드에서는 그냥 이렇게 부르면 된다.

```ts
fetch('/api/tour/searchKeyword2?keyword=우도')
```

**배포하면 안 된다.** 개발 서버가 없으니 대신 불러줄 사람이 사라진다. 그래서 배포 전에
**Firebase Functions** 로 같은 역할을 하는 걸 하나 만들어야 한다.

```
아직 안 만들었음 → 지금 배포하면 장소 검색만 동작 안 함 (나머지는 다 됨)
```

> 참고: Firebase Functions 를 쓰려면 요금제를 **Blaze(종량제)** 로 바꿔야 한다. 카드 등록이
> 필요하지만 무료 한도(월 200만 호출) 안이라 실제 요금은 0원이다. 카드가 부담되면
> Cloudflare Workers 나 Vercel 로 대신해도 된다.

**TourAPI 키는 절대 프론트 코드에 넣지 말 것.** 웹은 소스가 다 보인다. 키는 대신 불러주는
쪽(개발 서버 / Functions)에만 둔다.

## 카메라는 어떻게 쓰나

앱에서의 흐름:

```
스탬프 탭  →  장소 선택  →  인증하기  →  카메라 열기  →  촬영
   →  사진에서 색 3개가 자동으로 뽑힘  →  하나 고르고 "미션 완료"
```

코드는 특별한 라이브러리 없이 `<input>` 하나다 (`src/components/VerifySheet.tsx`).

```html
<input type="file" accept="image/*" capture="environment" />
```

`capture="environment"` 덕에 **폰에서는 후면 카메라가 바로 열리고, PC 에서는 파일 선택창이
뜬다.** 별도 분기가 필요 없다.

**⚠️ 카메라는 HTTPS 에서만 열린다.**

| 주소 | 카메라 |
|---|---|
| `http://localhost:5173` (PC) | ✅ 열림 (localhost 는 예외) |
| `http://192.168.0.5:5173` (폰) | ❌ **안 열림** |
| `https://...` (터널/배포) | ✅ 열림 |

폰에서 테스트하려면 위 "폰에서 보려면" 항목을 볼 것.

## 데이터는 어디에 저장되나

| 무엇 | 어디에 | 뜻 |
|---|---|---|
| 여행(앨범)·담은 장소·조각모음 | Firebase (`trips` 컬렉션, 웹 전용) | 로그인한 기기 어디서나 같은 여행이 보인다. 그룹 여행은 멤버끼리 실시간 반영 |
| 로그인 계정 | Firebase | 안드로이드 앱과 **공유** |

**개인 여행도 서버에 저장된다** (기기에만 남지 않음) — 다른 기기에서 같은 계정으로 로그인하면
그대로 보인다. 여행은 만들 때 개인/그룹을 고르고 시작일·종료일을 넣는다. 그룹으로 만들면
초대 코드가 나오고, 그 코드로 참여한 사람은 같은 여행의 지도·스탬프·조각모음을 같이 채운다.
종료일이 지나면 자동으로 "다녀옴"으로 분류되어 조각모음에 그 여행 이름의 앨범으로 묶여 보인다.

## 안드로이드 앱과의 관계

같은 Firebase 프로젝트를 쓰지만 **로그인 계정만 공유**한다. 그래서 **앱에서 가입한 계정으로
웹에서 그대로 로그인**은 되지만, 여행·장소·조각모음은 웹이 따로 쓰는 `trips` 컬렉션에 저장되므로
**안드로이드 앱의 "그룹" 데이터와는 서로 안 보인다.** 안드로이드 앱 코드/데이터를 건드리지 않기
위해 일부러 별도 컬렉션으로 뒀다.

## Firestore 보안 규칙 (배포 전 꼭 확인)

이 저장소에는 `firestore.rules` 가 없다 — 지금 규칙은 Firebase 콘솔에서 직접 관리되고 있을
가능성이 높다. `trips` 컬렉션을 실제로 쓰기 전에 콘솔(Firestore Database → 규칙)에 아래 내용이
반영돼 있는지 확인할 것 (기존 규칙이 따로 있다면 지우지 말고 병합해서 추가):

```
match /trips/{tripId} {
  allow read, update, delete: if request.auth != null &&
    request.auth.uid in resource.data.memberUids;
  allow create: if request.auth != null &&
    request.auth.uid == request.resource.data.ownerUid;

  match /spots/{spotId} {
    allow read, write: if request.auth != null &&
      request.auth.uid in get(/databases/$(database)/documents/trips/$(tripId)).data.memberUids;
  }
}
```

이게 없으면 로그인만 했으면 아무나 남의 여행을 읽거나 고칠 수 있다(또는 반대로 너무 막혀서
자기 것도 못 씀).

## 지도가 회색일 때

99% 는 **카카오 콘솔에 주소를 등록하지 않아서**다. 에러가 안 뜨기 때문에 원인을 찾기 어렵다.

```
카카오 개발자 콘솔 → 앱 설정 → 플랫폼 → Web → 사이트 도메인
```

지금 접속 중인 주소(`http://localhost:5173` 또는 터널 주소)가 **정확히 그대로** 들어 있어야
한다. 포트까지 같아야 한다.

그래도 안 되면 `.env.local` 의 `VITE_KAKAO_MAP_KEY` 가 **JavaScript 키**가 맞는지 확인할 것
(REST API 키나 네이티브 앱 키를 넣으면 안 된다).
