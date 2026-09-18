import { useEffect, useState } from 'react'
import type { User } from 'firebase/auth'
import crownIcon from '../assets/crown.svg'
import { createTrip, joinTripByCode, leaveTrip, tripStatus, watchTripSpots } from '../firebase/trips'
import { AddIcon, CopyIcon } from './Icons'
import { PieceTimeline } from './PieceTimeline'
import type { Trip, TripKind, TripSpot } from '../types'

/** 오늘 날짜('YYYY-MM-DD'). <input type="date"> 값과 그대로 비교할 수 있다. */
function today(): string {
  return new Date().toISOString().slice(0, 10)
}

function sortByStartDateDesc(trips: Trip[]): Trip[] {
  return [...trips].sort((a, b) => b.startDate.localeCompare(a.startDate))
}

/** 여행 카드 배경 — 목록을 위에서 아래로 훑을 때 아주 옅은 파스텔 색이 이어지도록. */
function pastelRainbow(index: number): string {
  return `hsl(${(index * 32) % 360}, 65%, 93%)`
}

/**
 * 여행 탭 — 만든/참여한 여행을 모아보고, 새 여행을 만들거나 코드로 참여하는 곳.
 *
 * 목록은 진행 중·예정·다녀온 여행 세 그룹으로 나눠서 보여준다 — 종료일이 지나면 자동으로
 * 다녀온 여행으로 분류된다(tripStatus).
 *
 * `trips` 는 App.tsx 에서 이미 구독 중인 걸 그대로 받는다 — 홈 화면 드롭다운과 같은 데이터를
 * 두 번 구독할 이유가 없다. 여행을 만들거나 참여하면 `onTripSelect`로 홈·스탬프가 보는
 * "현재 작업 중인 여행"도 그 여행으로 바꿔준다.
 */
export function AlbumTab({
  user,
  trips,
  onTripSelect,
}: {
  user: User
  trips: Trip[]
  onTripSelect: (tripId: string) => void
}) {
  const [openId, setOpenId] = useState<string | null>(null)
  const [error, setError] = useState<string | null>(null)

  const [showCreateTrip, setShowCreateTrip] = useState(false)
  const [showJoinTrip, setShowJoinTrip] = useState(false)
  const [tripActionBusy, setTripActionBusy] = useState(false)
  const [justCreatedTrip, setJustCreatedTrip] = useState<Trip | null>(null)
  const displayName = user.displayName || user.email?.split('@')[0] || '여행자'

  async function run(action: () => Promise<unknown>) {
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(e instanceof Error ? e.message : '여행 처리 중 오류가 발생했어요.')
    }
  }

  async function runTripAction(action: () => Promise<Trip>) {
    setTripActionBusy(true)
    setError(null)
    try {
      const trip = await action()
      onTripSelect(trip.id)
    } catch (e) {
      setError(e instanceof Error ? e.message : '여행 처리 중 오류가 발생했어요.')
    } finally {
      setTripActionBusy(false)
    }
  }

  // 여행 카드를 위에서 아래로 훑을 때 색이 이어지도록, 세 그룹을 하나의 순서로 잇는다.
  const ongoing = sortByStartDateDesc(trips.filter((t) => tripStatus(t) === 'ongoing'))
  const upcoming = sortByStartDateDesc(trips.filter((t) => tripStatus(t) === 'upcoming'))
  const past = sortByStartDateDesc(trips.filter((t) => tripStatus(t) === 'past'))
  const ordered = [...ongoing, ...upcoming, ...past]

  if (openId) {
    const trip = trips.find((t) => t.id === openId)
    if (trip) {
      const colorIndex = ordered.findIndex((t) => t.id === openId)
      return (
        <TripDetail
          trip={trip}
          uid={user.uid}
          background={pastelRainbow(colorIndex)}
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

  return (
    <section className="group">
      <div className="album-actions">
        <button className="btn-primary t-button" onClick={() => setShowCreateTrip(true)}>
          <AddIcon className="icon-inline" /> 여행 추가
        </button>
        <button className="pill t-button album-actions__code" onClick={() => setShowJoinTrip(true)}>
          코드 입력
        </button>
      </div>

      {justCreatedTrip && (
        <div className="modal-scrim" onClick={() => setJustCreatedTrip(null)}>
          <div className="modal invite-card" onClick={(e) => e.stopPropagation()}>
            <button
              className="modal__close"
              aria-label="닫기"
              onClick={() => setJustCreatedTrip(null)}
            >
              ✕
            </button>
            <h2 className="t-title">초대 코드가 발급됐어요</h2>
            <p className="t-caption">친구에게 이 코드를 알려주면 같이 담을 수 있어요.</p>
            <div className="group__row">
              <span className="t-display invite-card__code">{justCreatedTrip.inviteCode}</span>
              <button
                className="pill invite-card__copy"
                aria-label="초대 코드 복사"
                title="초대 코드 복사"
                onClick={() => {
                  void navigator.clipboard?.writeText(justCreatedTrip.inviteCode ?? '')
                }}
              >
                <CopyIcon />
              </button>
            </div>
          </div>
        </div>
      )}

      {error && <p className="t-caption search__error">{error}</p>}

      {trips.length === 0 ? (
        <div className="empty t-body">
          아직 만든 여행이 없어요.
          <br />
          "여행 추가"로 새로 만들거나 친구의 코드로 참여해보세요.
        </div>
      ) : (
        <>
          <TripList title="진행 중인 여행" trips={ongoing} startIndex={0} onOpen={setOpenId} />
          <TripList
            title="예정된 여행"
            trips={upcoming}
            startIndex={ongoing.length}
            onOpen={setOpenId}
          />
          <TripList
            title="다녀온 여행"
            trips={past}
            startIndex={ongoing.length + upcoming.length}
            onOpen={setOpenId}
          />
        </>
      )}

      {showCreateTrip && (
        <CreateTripSheet
          busy={tripActionBusy}
          onClose={() => setShowCreateTrip(false)}
          onCreate={(input) =>
            void runTripAction(async () => {
              const trip = await createTrip({
                ...input,
                ownerUid: user.uid,
                ownerName: displayName,
              })
              setShowCreateTrip(false)
              if (trip.kind === 'group') setJustCreatedTrip(trip)
              return trip
            })
          }
        />
      )}

      {showJoinTrip && (
        <JoinTripModal
          busy={tripActionBusy}
          onClose={() => setShowJoinTrip(false)}
          onJoin={(code) =>
            void runTripAction(async () => {
              const trip = await joinTripByCode(code, user.uid, displayName)
              setShowJoinTrip(false)
              return trip
            })
          }
        />
      )}
    </section>
  )
}

/** "코드 입력" 팝업 — 초대 코드로 그룹 여행에 참여한다. */
export function JoinTripModal({
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
export function CreateTripSheet({
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
            placeholder="여행 이름을 작성해주세요."
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
  startIndex,
  onOpen,
}: {
  title: string
  trips: Trip[]
  startIndex: number
  onOpen: (id: string) => void
}) {
  if (trips.length === 0) return null
  return (
    <div>
      <p className="t-subtitle section-label">{title}</p>
      <div className="group__list">
        {trips.map((t, i) => (
          <button
            key={t.id}
            className="group-card"
            style={{ background: pastelRainbow(startIndex + i) }}
            onClick={() => onOpen(t.id)}
          >
            <div>
              <p className="group-card__name">{t.name}</p>
              {/* 개인/그룹 꼬리표는 날짜 바로 옆에 조금 띄워서 붙인다. */}
              <p className="t-caption group-card__sub group-card__meta">
                {t.startDate} ~ {t.endDate}
                <span className={'kind-tag t-caption' + (t.kind === 'group' ? ' kind-tag--group' : ' kind-tag--personal')}>
                  {t.kind === 'group' ? '그룹' : '개인'}
                </span>
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
  background,
  onBack,
  onLeave,
}: {
  trip: Trip
  uid: string
  background: string
  onBack: () => void
  onLeave: () => void
}) {
  const [spots, setSpots] = useState<TripSpot[]>([])
  useEffect(() => watchTripSpots(trip.id, setSpots), [trip.id])
  const [confirmingLeave, setConfirmingLeave] = useState(false)

  const verified = spots.filter((s) => s.verifiedColor)
  const isOwner = trip.ownerUid === uid

  return (
    <section className="group group--detail" style={{ background }}>
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

      <div className="group__title-row">
        <h2 className="t-display">{trip.name}</h2>
        {trip.kind === 'group' && trip.inviteCode && (
          <button
            className="t-caption group__invite"
            title="눌러서 복사"
            onClick={() => {
              void navigator.clipboard?.writeText(trip.inviteCode ?? '')
            }}
          >
            초대 코드
            <br />
            {trip.inviteCode}
          </button>
        )}
      </div>
      <p className="t-caption group-card__sub">
        {trip.startDate} ~ {trip.endDate}
      </p>

      {/* 개인/그룹 상관없이 멤버 목록을 보여준다 — 개인은 만든 사람 혼자, 그룹은 초대로 들어온 사람까지. */}
      <div className="member-list">
        {Object.entries(trip.members).map(([memberUid, memberName]) => (
          <div key={memberUid} className="member-row">
            <span className="member-row__avatar-wrap">
              <span className="member-row__avatar">{memberName.slice(0, 1)}</span>
              {memberUid === trip.ownerUid && (
                <img className="member-row__crown" src={crownIcon} alt="방장" />
              )}
            </span>
            <span className="t-subtitle">{memberName}</span>
          </div>
        ))}
      </div>

      <PieceTimeline pieces={verified} tripName={trip.name} />

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

/** 되돌릴 수 없는 동작(삭제/나가기/로그아웃) 전에 한 번 더 확인받는 팝업. */
export function ConfirmModal({
  title,
  body,
  confirmLabel,
  onCancel,
  onConfirm,
}: {
  title: string
  /** 없으면 제목만 묻는다(예: 로그아웃). */
  body?: string
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
        {body && <p className="t-body confirm-modal__body">{body}</p>}
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
