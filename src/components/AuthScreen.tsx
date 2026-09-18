import { useState } from 'react'
import { signIn, signUp } from '../firebase/auth'
import { Brand } from './Brand'
import { EyeIcon } from './Icons'
import './AuthScreen.css'

type Mode = 'LOGIN' | 'SIGN_UP'

/**
 * 로그인 / 회원가입 — 안드로이드 AuthScreen 을 그대로 옮긴 것. 화면 하나에서 모드만 바꾼다.
 *
 * 서버에 보내기 전에 먼저 걸러내는 검사 순서도 같다: 빈 칸 → 이메일 형식 → 비밀번호 길이 →
 * (회원가입일 때) 확인 일치. 여기서 막으면 네트워크를 안 타고 바로 알려줄 수 있다.
 */
export function AuthScreen({ onAuthenticated }: { onAuthenticated: () => void }) {
  const [mode, setMode] = useState<Mode>('LOGIN')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [passwordConfirm, setPasswordConfirm] = useState('')
  const [autoLogin, setAutoLogin] = useState(true)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)

  /**
   * 붙여넣기 사고로 이메일에 줄바꿈이나 본문이 딸려 들어오는 일이 잦다. trim() 은 앞뒤 공백만
   * 없애므로 중간에 낀 공백·줄바꿈까지 걷어낸다.
   */
  const cleanEmail = email.replace(/\s/g, '')

  async function submit(e: React.FormEvent) {
    e.preventDefault()
    const validation =
      !cleanEmail || !password
        ? '이메일과 비밀번호를 입력해주세요.'
        : !/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(cleanEmail)
          ? '이메일 형식이 올바르지 않아요.'
          : password.length < 6
            ? '비밀번호는 6자 이상으로 입력해주세요.'
            : mode === 'SIGN_UP' && password !== passwordConfirm
              ? '비밀번호가 서로 일치하지 않아요.'
              : null

    if (validation) return setError(validation)

    setLoading(true)
    setError(null)
    const result =
      mode === 'LOGIN'
        ? await signIn(cleanEmail, password, autoLogin)
        : await signUp(cleanEmail, password)
    setLoading(false)

    if (result.ok) onAuthenticated()
    else setError(result.message)
  }

  return (
    <form className="auth" onSubmit={submit}>
      {/* 앱 헤더와 똑같은 로고 + 제목을 가운데에. */}
      <Brand className="auth__brand" />

      <label className="field">
        <span className="t-subtitle">이메일</span>
        <input
          type="email"
          className="t-body"
          placeholder="you@example.com"
          autoComplete="email"
          value={email}
          onChange={(e) => {
            setEmail(e.target.value)
            setError(null)
          }}
        />
      </label>

      <label className="field">
        <span className="t-subtitle">비밀번호</span>
        <PasswordInput
          placeholder="6자 이상 입력해주세요"
          autoComplete={mode === 'LOGIN' ? 'current-password' : 'new-password'}
          value={password}
          onChange={(value) => {
            setPassword(value)
            setError(null)
          }}
        />
      </label>

      {mode === 'SIGN_UP' ? (
        <label className="field">
          <span className="t-subtitle">비밀번호 확인</span>
          <PasswordInput
            placeholder="비밀번호를 다시 입력해주세요"
            autoComplete="new-password"
            value={passwordConfirm}
            onChange={(value) => {
              setPasswordConfirm(value)
              setError(null)
            }}
          />
        </label>
      ) : (
        <label className="auto-login t-caption">
          <input
            type="checkbox"
            checked={autoLogin}
            disabled={loading}
            onChange={(e) => setAutoLogin(e.target.checked)}
          />
          자동 로그인
        </label>
      )}

      {error && <p className="t-caption auth__error">{error}</p>}

      <button className="btn-primary t-button auth__submit" type="submit" disabled={loading}>
        {loading ? '처리 중…' : mode === 'LOGIN' ? '로그인' : '회원가입'}
      </button>

      <button
        type="button"
        className="t-caption auth__switch"
        disabled={loading}
        onClick={() => {
          setMode(mode === 'LOGIN' ? 'SIGN_UP' : 'LOGIN')
          setError(null)
        }}
      >
        {mode === 'LOGIN' ? '아직 계정이 없으신가요? 회원가입' : '이미 계정이 있으신가요? 로그인'}
      </button>
    </form>
  )
}

/**
 * 비밀번호 칸 + 오른쪽 눈 아이콘. 눈을 누르고 있는 동안만 적은 글자가 보이고, 손을 떼면(또는
 * 손가락이 버튼 밖으로 나가면) 바로 다시 가려진다. 누르는 순간 기본 동작을 막아서 입력칸의
 * 포커스와 키보드가 그대로 남는다.
 */
function PasswordInput({
  value,
  onChange,
  placeholder,
  autoComplete,
}: {
  value: string
  onChange: (value: string) => void
  placeholder: string
  autoComplete: string
}) {
  const [shown, setShown] = useState(false)
  const hide = () => setShown(false)

  return (
    <span className="field__control">
      <input
        type={shown ? 'text' : 'password'}
        className="t-body"
        placeholder={placeholder}
        autoComplete={autoComplete}
        value={value}
        onChange={(e) => onChange(e.target.value)}
      />
      <button
        type="button"
        className={'field__eye' + (shown ? ' is-active' : '')}
        aria-label="누르고 있는 동안 비밀번호 보기"
        onPointerDown={(e) => {
          e.preventDefault()
          setShown(true)
        }}
        onPointerUp={hide}
        onPointerLeave={hide}
        onPointerCancel={hide}
        onKeyDown={(e) => {
          if (e.key === ' ' || e.key === 'Enter') setShown(true)
        }}
        onKeyUp={hide}
        onBlur={hide}
        onContextMenu={(e) => e.preventDefault()}
      >
        <EyeIcon />
      </button>
    </span>
  )
}
