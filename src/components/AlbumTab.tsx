import { useEffect, useState } from 'react'
import type { User } from 'firebase/auth'
import {
  createTrip,
  joinTripByCode,
  leaveTrip,
  tripStatus,
  watchTripSpots,
} from '../firebase/trips'
import { PieceTimeline } from './PieceTimeline'
import type { Trip, TripKind, TripSpot } from '../types'

/** 오늘 날짜('YYYY-MM-DD'). <input type="date"> 값과 그대로 비교할 수 있다. */
function today(): string {
  return new Date().toISOString().slice(0, 10)
}

/**
 * 앨범 — 여행을 만들고 참여하는 곳. (예전 "그룹" 탭 자리)
 *
 * 여행은 개인(혼자) 또는 그룹(초대코드로 친구와 같이 담기)으로 만든다. "새 여행 만들기" 버튼을
 * 누르면 팝업(바텀시트)이 뜨고, 거기서 이름·개인/그룹·기간을 입력해 "여행 시작하기"로 만든다.
 * 목록은 진행중·예정과 다녀옴으로 나눠서 보여준다 — 종료일이 지나면 자동으로 다녀옴으로
 * 분류된다(tripStatus).
 *
 * `trips` 는 App.tsx 에서 이미 구독 중인 걸 그대로 받는다 — 홈 화면 드롭다운과 같은 데이터를
 * 두 번 구독할 이유가 없다.
 */
export function AlbumTab({ user, trips }: { user: User; trips: Trip[] }) {
  const [openId, setOpenId] = useState<string | null>(null)
  const [showCreate, setShowCreate] = useState(false)
  const [showJoin, setShowJoin] = useState(false)
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [justCreated, setJustCreated] = useState<Trip | null>(null)

  const displayName = user.displayName || user.email?.split('@')[0] || '여행자'

  async function run(action: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(e instanceof Error ? e.message : '여행 처리 중 오류가 발생했어요.')
    } finally {
      setBusy(false)
    }
  }

  if (openId) {
    const trip = trips.find((t) => t.id === openId)
    if (trip) {
      return (
        <TripDetail
          trip={trip}
          uid={user.uid}
          onBack={() => setOpenId(null)}
          onLeave={() =>
            void run(async () => {
              await leaveTrip(trip.id, user.uid)
              setOpenId(null)
            })
          }
        />
      )
    }
  }

  const upcoming = trips.filter((t) => tripStatus(t) !== 'past')
  const past = trips.filter((t) => tripStatus(t) === 'past')

  return (
    <section className="group">
      <div className="album-actions">
        <button className="btn-primary t-button" onClick={() => setShowCreate(true)}>
          + 여행 추가
        </button>
        <button className="pill t-button album-actions__code" onClick={() => setShowJoin(true)}>
          코드 입력
        </button>
      </div>

      {justCreated && (
        <div className="group__form invite-card">
          <h2 className="t-title">초대 코드가 발급됐어요</h2>
          <p className="t-caption">친구에게 이 코드를 알려주면 같이 담을 수 있어요.</p>
          <div className="group__row">
            <span className="t-display invite-card__code">{justCreated.inviteCode}</span>
            <button
              className="pill t-subtitle"
              onClick={() => {
                void navigator.clipboard?.writeText(justCreated.inviteCode ?? '')
              }}
            >
              복사
            </button>
          </div>
          <button className="t-caption group__back" onClick={() => setJustCreated(null)}>
            닫기
          </button>
        </div>
      )}

      {error && <p className="t-caption search__error">{error}</p>}

      {trips.length === 0 ? (
        <div className="empty t-body">
          아직 만든 여행이 없어요.
          <br />
          여행을 만들거나 친구의 코드로 참여해보세요.
        </div>
      ) : (
        <>
          <TripList title="진행중·예정" trips={upcoming} onOpen={setOpenId} />
          <TripList title="다녀옴" trips={past} onOpen={setOpenId} />
        </>
      )}

      {showCreate && (
        <CreateTripSheet
          busy={busy}
          onClose={() => setShowCreate(false)}
          onCreate={(input) =>
            void run(async () => {
              const trip = await createTrip({
                ...input,
                ownerUid: user.uid,
                ownerName: displayName,
              })
              setShowCreate(false)
              if (trip.kind === 'group') setJustCreated(trip)
            })
          }
        />
      )}

      {showJoin && (
        <JoinTripModal
          busy={busy}
          onClose={() => setShowJoin(false)}
          onJoin={(code) =>
            void run(async () => {
              await joinTripByCode(code, user.uid, displayName)
              setShowJoin(false)
            })
          }
        />
      )}
    </section>
  )
}

/** "코드 입력" 팝업 — 초대 코드로 그룹 여행에 참여한다. */
function JoinTripModal({
  busy,
  onClose,
  onJoin,
}: {
  busy: boolean
  onClose: () => void
  onJoin: (code: string) => void
}) {
  const [code, setCode] = useState('')

  return (
    <div className="modal-scrim" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal__close" aria-label="닫기" onClick={onClose}>
          ✕
        </button>
        <h2 className="t-title">코드로 참여하기</h2>

        <div className="trip-form">
          <input
            className="search__input t-body group__code-input"
            placeholder="초대 코드 6자리"
            maxLength={6}
            value={code}
            onChange={(e) => setCode(e.target.value.toUpperCase())}
          />
          <div className="verify__actions">
            <button className="pill t-subtitle" onClick={onClose}>
              취소
            </button>
            <button
              className="btn-primary t-button"
              disabled={busy || code.trim().length < 6}
              onClick={() => onJoin(code)}
            >
              참여
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

/**
 * "새 여행 만들기" 팝업. 시작일은 오늘 이후(오늘 포함)만 허용한다 — 이미 지난 날짜로 여행을
 * 시작할 수는 없으니까. `min` 속성으로 달력에서부터 막고, 그래도 잘못 들어오면(직접 타이핑 등)
 * 제출 시 다시 한번 검사해 오류 메시지를 보여준다.
 */
function CreateTripSheet({
  busy,
  onClose,
  onCreate,
}: {
  busy: boolean
  onClose: () => void
  onCreate: (input: { name: string; kind: TripKind; startDate: string; endDate: string }) => void
}) {
  const [name, setName] = useState('')
  const [kind, setKind] = useState<TripKind>('personal')
  const [startDate, setStartDate] = useState('')
  const [endDate, setEndDate] = useState('')
  const [validationError, setValidationError] = useState<string | null>(null)

  const canSubmit = name.trim() !== '' && startDate !== '' && endDate !== ''

  function submit() {
    if (!canSubmit) return
    if (startDate < today()) {
      setValidationError('여행 시작일은 오늘 이후로 골라주세요. 지난 날짜로는 여행을 만들 수 없어요.')
      return
    }
    if (endDate < startDate) {
      setValidationError('종료일은 시작일보다 같거나 늦어야 해요.')
      return
    }
    setValidationError(null)
    onCreate({ name, kind, startDate, endDate })
  }

  return (
    <div className="modal-scrim" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal__close" aria-label="닫기" onClick={onClose}>
          ✕
        </button>
        <h2 className="t-title">새 여행 만들기</h2>

        <div className="trip-form">
          <div className="segmented">
            {(['personal', 'group'] as TripKind[]).map((k) => (
              <button
                key={k}
                className={'segmented__item t-subtitle' + (kind === k ? ' is-active' : '')}
                onClick={() => setKind(k)}
              >
                {k === 'personal' ? '개인' : '그룹'}
              </button>
            ))}
          </div>

          <input
            className="search__input t-body"
            placeholder="여행 이름 (예: 제주 3박4일)"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />

          <div className="group__row">
            <input
              className="search__input t-body"
              type="date"
              aria-label="시작일"
              min={today()}
              value={startDate}
              onChange={(e) => {
                setStartDate(e.target.value)
                setValidationError(null)
              }}
            />
            <input
              className="search__input t-body"
              type="date"
              aria-label="종료일"
              min={startDate || today()}
              value={endDate}
              onChange={(e) => {
                setEndDate(e.target.value)
                setValidationError(null)
              }}
            />
          </div>

          {validationError && <p className="t-caption search__error">{validationError}</p>}

          <div className="verify__actions">
            <button className="pill t-subtitle" onClick={onClose}>
              취소
            </button>
            <button className="btn-primary t-button" disabled={busy || !canSubmit} onClick={submit}>
              여행 시작하기
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}

function TripList({
  title,
  trips,
  onOpen,
}: {
  title: string
  trips: Trip[]
  onOpen: (id: string) => void
}) {
  if (trips.length === 0) return null
  return (
    <div>
      <p className="t-subtitle rainbow__label">{title}</p>
      <div className="group__list">
        {[...trips]
          .sort((a, b) => b.startDate.localeCompare(a.startDate))
          .map((t) => (
            <button key={t.id} className="group-card" onClick={() => onOpen(t.id)}>
              <div>
                <p className="t-subtitle">
                  {t.name} <span className="t-caption">{t.kind === 'group' ? '· 그룹' : '· 개인'}</span>
                </p>
                <p className="t-caption group-card__sub">
                  {t.startDate} ~ {t.endDate}
                  {t.kind === 'group' ? ` · 멤버 ${t.memberUids.length}명 · 코드 ${t.inviteCode}` : ''}
                </p>
              </div>
              <span className="t-caption group-card__go">›</span>
            </button>
          ))}
      </div>
    </div>
  )
}

function TripDetail({
  trip,
  uid,
  onBack,
  onLeave,
}: {
  trip: Trip
  uid: string
  onBack: () => void
  onLeave: () => void
}) {
  const [spots, setSpots] = useState<TripSpot[]>([])
  useEffect(() => watchTripSpots(trip.id, setSpots), [trip.id])
  const [confirmingLeave, setConfirmingLeave] = useState(false)

  const verified = spots.filter((s) => s.verifiedColor)
  const isOwner = trip.ownerUid === uid

  return (
    <section className="group">
      <div className="group__head">
        <button className="t-subtitle group__back" onClick={onBack}>
          ‹ 여행 목록
        </button>
        {/* 이름 그대로: 방장은 여행을 지우고, 방장이 아니면 여행에서 나간다. 둘 다 되돌릴 수
            없어서 팝업으로 한 번 더 확인받는다. */}
        <button className="t-caption group__leave" onClick={() => setConfirmingLeave(true)}>
          {isOwner ? '여행 삭제' : '여행 나가기'}
        </button>
      </div>

      <h2 className="t-title">{trip.name}</h2>
      <p className="t-caption group-card__sub">
        {trip.startDate} ~ {trip.endDate}
        {trip.kind === 'group' ? ` · 초대 코드 ${trip.inviteCode}` : ''}
      </p>

      {/* 개인/그룹 상관없이 멤버 목록을 보여준다 — 개인은 만든 사람 혼자, 그룹은 초대로 들어온 사람까지. */}
      <div className="member-list">
        {Object.entries(trip.members).map(([memberUid, memberName]) => (
          <div key={memberUid} className="member-row">
            <span className="member-row__avatar">{memberName.slice(0, 1)}</span>
            <span className="t-subtitle">{memberName}</span>
            {memberUid === trip.ownerUid && <span className="t-caption member-row__badge">방장</span>}
          </div>
        ))}
      </div>

      <PieceTimeline pieces={verified} />

      {confirmingLeave && (
        <ConfirmModal
          title={isOwner ? '여행을 삭제할까요?' : '여행에서 나갈까요?'}
          body={
            isOwner
              ? '삭제하면 이 여행에 담긴 장소와 인증 사진이 전부 함께 사라져요. 되돌릴 수 없어요.'
              : '나가면 이 여행 목록에서 빠지고, 다시 보려면 초대 코드로 재참여해야 해요.'
          }
          confirmLabel={isOwner ? '삭제하기' : '나가기'}
          onCancel={() => setConfirmingLeave(false)}
          onConfirm={() => {
            setConfirmingLeave(false)
            onLeave()
          }}
        />
      )}
    </section>
  )
}

/** 되돌릴 수 없는 동작(삭제/나가기) 전에 한 번 더 확인받는 팝업. */
function ConfirmModal({
  title,
  body,
  confirmLabel,
  onCancel,
  onConfirm,
}: {
  title: string
  body: string
  confirmLabel: string
  onCancel: () => void
  onConfirm: () => void
}) {
  return (
    <div className="modal-scrim" onClick={onCancel}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal__close" aria-label="닫기" onClick={onCancel}>
          ✕
        </button>
        <h2 className="t-title">{title}</h2>
        <p className="t-body confirm-modal__body">{body}</p>
        <div className="verify__actions">
          <button className="pill t-subtitle" onClick={onCancel}>
            취소
          </button>
          <button className="btn-primary t-button" onClick={onConfirm}>
            {confirmLabel}
          </button>
        </div>
      </div>
    </div>
  )
}
