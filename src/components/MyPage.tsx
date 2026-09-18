import { useRef, useState } from 'react'
import type { User } from 'firebase/auth'
import { logOut, updateDisplayName, updateProfilePhoto } from '../firebase/auth'
import { ConfirmModal } from './AlbumTab'
import { AddIcon, CheckIcon, PencilIcon } from './Icons'
import type { TripSpot } from '../types'

/** Firebase Auth 의 photoURL 필드는 2048자를 넘으면 "Photo URL too long" 으로 거절된다. */
const MAX_PHOTO_URL_LENGTH = 1900

function loadImage(file: File): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    const url = URL.createObjectURL(file)
    img.onload = () => {
      URL.revokeObjectURL(url)
      resolve(img)
    }
    img.onerror = () => {
      URL.revokeObjectURL(url)
      reject(new Error('image load failed'))
    }
    img.src = url
  })
}

/**
 * 사진을 정사각형으로 가운데 잘라 작은 데이터 URL로 줄인다 — Storage 없이 Auth 프로필의
 * photoURL 에 그대로 저장할 수 있는 크기여야 한다. 사진마다 압축 후 실제 크기가 달라서,
 * 고정 크기 하나로는 사진에 따라 2048자 제한을 넘을 수 있다 — 넘으면 더 작게 다시 시도한다.
 */
async function fileToSmallDataUrl(file: File): Promise<string> {
  const img = await loadImage(file)
  const side = Math.min(img.naturalWidth, img.naturalHeight)
  const sx = (img.naturalWidth - side) / 2
  const sy = (img.naturalHeight - side) / 2
  const canvas = document.createElement('canvas')
  const ctx = canvas.getContext('2d')
  if (!ctx) throw new Error('canvas unsupported')

  for (const size of [96, 64, 48, 32, 24]) {
    canvas.width = size
    canvas.height = size
    for (const quality of [0.7, 0.5, 0.3]) {
      ctx.clearRect(0, 0, size, size)
      ctx.drawImage(img, sx, sy, side, side, 0, 0, size, size)
      const dataUrl = canvas.toDataURL('image/jpeg', quality)
      if (dataUrl.length <= MAX_PHOTO_URL_LENGTH) return dataUrl
    }
  }
  throw new Error('image too large even at lowest quality')
}

/**
 * 마이페이지 — 프로필과 모든 여행을 합친 숫자들, 그리고 로그아웃.
 *
 * 프로필 편집은 이 컴포넌트의 로컬 state 로만 있다 — 어디에도 저장해두지 않기 때문에, 수정
 * 중에 다른 탭으로 옮기면(마이페이지가 언마운트되면) 그 기록은 그냥 사라진다. 다시 마이 탭에
 * 오면 항상 연필 아이콘(수정 전 상태)부터 다시 시작한다.
 */
export function MyPage({
  user,
  allSpots,
  tripCount,
}: {
  user: User
  allSpots: TripSpot[]
  tripCount: number
}) {
  const [name, setName] = useState(user.displayName ?? '')
  const [editing, setEditing] = useState(false)
  const [photoPreview, setPhotoPreview] = useState<string | null>(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState<string | null>(null)
  const [confirmingLogout, setConfirmingLogout] = useState(false)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const displayName = user.displayName || user.email?.split('@')[0] || '여행자'
  const avatarSrc = photoPreview ?? user.photoURL ?? null
  const verified = allSpots.filter((s) => s.verifiedColor).length

  async function save() {
    setBusy(true)
    setMessage(null)
    const nameResult = await updateDisplayName(name.trim())
    if (!nameResult.ok) {
      setBusy(false)
      setMessage(nameResult.message)
      return
    }
    if (photoPreview) {
      const photoResult = await updateProfilePhoto(photoPreview)
      if (!photoResult.ok) {
        setBusy(false)
        setMessage(photoResult.message)
        return
      }
    }
    setBusy(false)
    setEditing(false)
    setPhotoPreview(null)
  }

  function pickPhoto(file: File) {
    fileToSmallDataUrl(file)
      .then(setPhotoPreview)
      .catch(() => setMessage('사진을 불러오지 못했어요.'))
  }

  return (
    <section className="mypage">
      <div className="profile">
        <button
          type="button"
          className="profile__avatar-btn"
          onClick={() => {
            if (editing) fileInputRef.current?.click()
          }}
        >
          {avatarSrc ? (
            <img className="profile__avatar-img" src={avatarSrc} alt="" />
          ) : (
            <span className="profile__avatar">{displayName.slice(0, 1)}</span>
          )}
          {editing && (
            <span className="profile__avatar-add" aria-hidden="true">
              <AddIcon />
            </span>
          )}
        </button>
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          className="sr-only"
          onChange={(e) => {
            const file = e.target.files?.[0]
            e.target.value = ''
            if (file) pickPhoto(file)
          }}
        />

        <div className="profile__info">
          {editing ? (
            <input
              className="search__input t-body profile__name-input"
              placeholder="닉네임"
              value={name}
              onChange={(e) => setName(e.target.value)}
            />
          ) : (
            <p className="t-title">{displayName}</p>
          )}
          <p className="t-caption stamp__sub">{user.email}</p>
        </div>

        <button
          type="button"
          className="profile__edit"
          disabled={busy}
          aria-label={editing ? '수정 완료' : '프로필 수정'}
          onClick={() => (editing ? void save() : setEditing(true))}
        >
          {editing ? <CheckIcon /> : <PencilIcon />}
        </button>
      </div>

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
          <p className="t-display">{tripCount}</p>
          <p className="t-caption stamp__sub">여행</p>
        </div>
      </div>

      <button className="pill t-subtitle mypage__logout" onClick={() => setConfirmingLogout(true)}>
        로그아웃
      </button>

      {confirmingLogout && (
        <ConfirmModal
          title="정말 로그아웃 하시겠습니까?"
          confirmLabel="로그아웃"
          onCancel={() => setConfirmingLogout(false)}
          onConfirm={() => {
            setConfirmingLogout(false)
            void logOut()
          }}
        />
      )}
    </section>
  )
}
