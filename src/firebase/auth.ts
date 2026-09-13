import {
  browserLocalPersistence,
  browserSessionPersistence,
  createUserWithEmailAndPassword,
  onAuthStateChanged,
  setPersistence,
  signInWithEmailAndPassword,
  signOut,
  updateProfile,
  type User,
} from 'firebase/auth'
import { useEffect, useState } from 'react'
import { auth } from './app'

export type AuthResult = { ok: true; user: User } | { ok: false; message: string }

/**
 * Firebase 오류를 그대로 보여주면 영어인 데다 원인도 안 읽혀서, 화면에 쓸 문구로 바꾼다.
 *
 * `auth/invalid-credential` 은 안드로이드 쪽에서 "이메일 형식이 올바르지 않거나 비밀번호가
 * 틀렸어요."로 안내하고 있는데, 최신 Firebase 는 **비밀번호만 틀려도** 이 코드를 준다.
 * 그래서 형식 얘기를 빼고 둘 다 다시 보라고만 말한다 — 실제로 이 문구 때문에 멀쩡한 이메일을
 * 두고 형식을 의심한 적이 있다.
 */
function messageOf(code: string): string {
  switch (code) {
    case 'auth/invalid-credential':
    case 'auth/wrong-password':
      return '이메일 또는 비밀번호를 다시 확인해주세요.'
    case 'auth/user-not-found':
      return '가입되지 않은 이메일이에요.'
    case 'auth/email-already-in-use':
      return '이미 가입된 이메일이에요.'
    case 'auth/weak-password':
      return '비밀번호는 6자 이상으로 입력해주세요.'
    case 'auth/invalid-email':
      return '이메일 형식이 올바르지 않아요.'
    case 'auth/network-request-failed':
      return '네트워크 연결을 확인해주세요.'
    case 'auth/too-many-requests':
      return '시도가 너무 잦아요. 잠시 후 다시 해주세요.'
    default:
      return '알 수 없는 오류가 발생했어요.'
  }
}

function toResult(e: unknown): AuthResult {
  const code = typeof e === 'object' && e && 'code' in e ? String(e.code) : ''
  return { ok: false, message: messageOf(code) }
}

/**
 * 자동 로그인 여부를 저장 방식으로 표현한다. local 은 브라우저를 닫아도 세션이 남고,
 * session 은 탭을 닫으면 사라진다 — 안드로이드의 AutoLoginPreferences 와 같은 역할이다.
 */
async function applyPersistence(autoLogin: boolean) {
  await setPersistence(auth, autoLogin ? browserLocalPersistence : browserSessionPersistence)
}

export async function signIn(
  email: string,
  password: string,
  autoLogin: boolean,
): Promise<AuthResult> {
  try {
    await applyPersistence(autoLogin)
    const cred = await signInWithEmailAndPassword(auth, email, password)
    return { ok: true, user: cred.user }
  } catch (e) {
    return toResult(e)
  }
}

export async function signUp(email: string, password: string): Promise<AuthResult> {
  try {
    // 회원가입 직후는 늘 로그인 상태를 유지한다 (안드로이드와 같은 규칙).
    await applyPersistence(true)
    const cred = await createUserWithEmailAndPassword(auth, email, password)
    return { ok: true, user: cred.user }
  } catch (e) {
    return toResult(e)
  }
}

export async function updateDisplayName(name: string): Promise<AuthResult> {
  const user = auth.currentUser
  if (!user) return { ok: false, message: '로그인이 필요해요.' }
  try {
    await updateProfile(user, { displayName: name })
    return { ok: true, user }
  } catch (e) {
    return toResult(e)
  }
}

export function logOut() {
  return signOut(auth)
}

/**
 * 현재 로그인한 사용자. `loading` 이 true 인 동안에는 로그인 화면을 띄우지 않는다 —
 * Firebase 가 저장된 세션을 복원하기 전에 판단하면, 이미 로그인한 사람에게 로그인 화면이
 * 한 번 깜빡이고 사라진다.
 */
export function useAuthUser(): { user: User | null; loading: boolean } {
  const [state, setState] = useState<{ user: User | null; loading: boolean }>({
    user: null,
    loading: true,
  })
  useEffect(() => onAuthStateChanged(auth, (user) => setState({ user, loading: false })), [])
  return state
}
