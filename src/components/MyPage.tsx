import { useState } from 'react'
import type { User } from 'firebase/auth'
import { logOut, updateDisplayName } from '../firebase/auth'
import type { TripSpot } from '../types'

/** 마이페이지 — 프로필과 모든 여행을 합친 숫자들, 그리고 로그아웃. */
export function MyPage({ user, allSpots }: { user: User; allSpots: TripSpot[] }) {
  const [name, setName] = useState(user.displayName ?? '')
  const [editing, setEditing] = useState(false)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)

  const displayName = user.displayName || user.email?.split('@')[0] || '여행자'
  const verified = allSpots.filter((s) => s.verifiedColor).length

  async function save() {
    setBusy(true)
    const result = await updateDisplayName(name.trim())
    setBusy(false)
    setMessage(result.ok ? '닉네임을 저장했어요.' : result.message)
    if (result.ok) setEditing(false)
  }

  return (
    <section className="mypage">
      <div className="profile">
        {/* 프로필 사진 업로드는 Storage 가 필요해 아직 없다 — 이름 첫 글자로 대신한다. */}
        <span className="profile__avatar">{displayName.slice(0, 1)}</span>
        <div>
          <p className="t-title">{displayName}</p>
          <p className="t-caption stamp__sub">{user.email}</p>
        </div>
      </div>

      {editing ? (
        <div className="group__row">
          <input
            className="search__input t-body"
            placeholder="닉네임"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <button className="btn-primary t-button" disabled={busy || !name.trim()} onClick={() => void save()}>
            저장
          </button>
        </div>
      ) : (
        <button className="pill t-subtitle" onClick={() => setEditing(true)}>
          닉네임 수정
        </button>
      )}

      {message && <p className="t-caption search__hint">{message}</p>}

      <div className="stats">
        <div className="stat">
          <p className="t-display">{allSpots.length}</p>
          <p className="t-caption stamp__sub">담은 여행지</p>
        </div>
        <div className="stat">
          <p className="t-display">{verified}</p>
          <p className="t-caption stamp__sub">인증 완료</p>
        </div>
        <div className="stat">
          <p className="t-display">{verified}</p>
          <p className="t-caption stamp__sub">모은 조각</p>
        </div>
      </div>

      <button className="pill t-subtitle mypage__logout" onClick={() => void logOut()}>
        로그아웃
      </button>
    </section>
  )
}
