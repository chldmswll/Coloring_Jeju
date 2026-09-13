import { initializeApp } from 'firebase/app'
import { getAuth } from 'firebase/auth'
import { getFirestore } from 'firebase/firestore'

/**
 * 안드로이드 앱과 **같은 Firebase 프로젝트**에 붙는다. 계정과 그룹 데이터가 그대로 공유되므로
 * 앱에서 가입한 사람이 웹에서 같은 이메일로 로그인되고, 한쪽에서 만든 그룹이 다른 쪽에도 보인다.
 *
 * 설정값은 google-services.json 에서 가져온 것이라 콘솔 작업이 따로 필요 없었다. appId 는
 * 안드로이드용밖에 없지만 Analytics 에만 쓰이는 값이라 Auth/Firestore 동작에는 지장이 없다.
 * (나중에 콘솔에서 웹 앱을 정식 등록하면 웹 appId 가 생긴다.)
 *
 * 이 값들이 프론트 번들에 들어가는 건 정상이다 — Firebase 는 키를 숨겨서가 아니라 Firestore
 * 보안 규칙과 Auth 승인 도메인으로 보호한다.
 */
const firebaseApp = initializeApp({
  apiKey: import.meta.env.VITE_FIREBASE_API_KEY,
  authDomain: import.meta.env.VITE_FIREBASE_AUTH_DOMAIN,
  projectId: import.meta.env.VITE_FIREBASE_PROJECT_ID,
  storageBucket: import.meta.env.VITE_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: import.meta.env.VITE_FIREBASE_MESSAGING_SENDER_ID,
})

export const auth = getAuth(firebaseApp)
export const db = getFirestore(firebaseApp)
