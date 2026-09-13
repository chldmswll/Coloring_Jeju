import { useEffect, useState } from 'react'
import type { User } from 'firebase/auth'
import {
  createGroup,
  joinGroup,
  leaveGroup,
  watchGroupSpots,
  watchMyGroups,
  type GroupSpot,
  type TravelGroup,
} from '../firebase/groups'

/**
 * 그룹 — 같은 코드를 가진 사람들이 하나의 지도를 같이 채운다.
 *
 * 목록도 장소도 onSnapshot 으로 구독하므로, 다른 사람이 담거나 인증하면 새로고침 없이 바로
 * 반영된다. 안드로이드 앱과 같은 Firestore 를 보기 때문에 앱에서 만든 그룹도 여기 나온다.
 */
export function GroupTab({ user }: { user: User }) {
  const [groups, setGroups] = useState<TravelGroup[]>([])
  const [openCode, setOpenCode] = useState<string | null>(null)
  const [name, setName] = useState('')
  const [joinCode, setJoinCode] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => watchMyGroups(user.uid, setGroups), [user.uid])

  const displayName = user.displayName || user.email?.split('@')[0] || '여행자'

  async function run(action: () => Promise<unknown>) {
    setBusy(true)
    setError(null)
    try {
      await action()
    } catch (e) {
      setError(e instanceof Error ? e.message : '그룹 처리 중 오류가 발생했어요.')
    } finally {
      setBusy(false)
    }
  }

  if (openCode) {
    const group = groups.find((g) => g.code === openCode)
    if (group) {
      return (
        <GroupDetail
          group={group}
          uid={user.uid}
          onBack={() => setOpenCode(null)}
          onLeave={() =>
            void run(async () => {
              await leaveGroup(group.code, user.uid)
              setOpenCode(null)
            })
          }
        />
      )
    }
  }

  return (
    <section className="group">
      <div className="group__form">
        <h2 className="t-title">그룹 만들기</h2>
        <div className="group__row">
          <input
            className="search__input t-body"
            placeholder="그룹 이름 (예: 제주 3박4일)"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
          <button
            className="btn-primary t-button"
            disabled={busy || !name.trim()}
            onClick={() =>
              void run(async () => {
                await createGroup(name, user.uid, displayName)
                setName('')
              })
            }
          >
            만들기
          </button>
        </div>
      </div>

      <div className="group__form">
        <h2 className="t-title">코드로 참여하기</h2>
        <div className="group__row">
          <input
            className="search__input t-body group__code-input"
            placeholder="초대 코드 6자리"
            maxLength={6}
            value={joinCode}
            onChange={(e) => setJoinCode(e.target.value.toUpperCase())}
          />
          <button
            className="btn-primary t-button"
            disabled={busy || joinCode.trim().length < 6}
            onClick={() =>
              void run(async () => {
                await joinGroup(joinCode, user.uid, displayName)
                setJoinCode('')
              })
            }
          >
            참여
          </button>
        </div>
      </div>

      {error && <p className="t-caption search__error">{error}</p>}

      {groups.length === 0 ? (
        <div className="empty t-body">
          아직 참여한 그룹이 없어요.
          <br />
          그룹을 만들거나 친구의 코드로 참여해보세요.
        </div>
      ) : (
        <div className="group__list">
          {[...groups]
            .sort((a, b) => b.createdAt - a.createdAt)
            .map((g) => (
              <button key={g.code} className="group-card" onClick={() => setOpenCode(g.code)}>
                <div>
                  <p className="t-subtitle">{g.name}</p>
                  <p className="t-caption group-card__sub">
                    멤버 {g.memberUids.length}명 · 코드 {g.code}
                  </p>
                </div>
                <span className="t-caption group-card__go">›</span>
              </button>
            ))}
        </div>
      )}
    </section>
  )
}

function GroupDetail({
  group,
  uid,
  onBack,
  onLeave,
}: {
  group: TravelGroup
  uid: string
  onBack: () => void
  onLeave: () => void
}) {
  const [spots, setSpots] = useState<GroupSpot[]>([])
  useEffect(() => watchGroupSpots(group.code, setSpots), [group.code])

  const done = spots.filter((s) => s.verifiedColor).length

  return (
    <section className="group">
      <div className="group__head">
        <button className="t-subtitle group__back" onClick={onBack}>
          ‹ 그룹 목록
        </button>
        <button className="t-caption group__leave" onClick={onLeave}>
          나가기
        </button>
      </div>

      <h2 className="t-title">{group.name}</h2>
      <p className="t-caption group-card__sub">
        초대 코드 <strong>{group.code}</strong> · 멤버 {group.memberUids.length}명 · 인증 {done}/
        {spots.length}
      </p>

      <div className="chips">
        {Object.entries(group.members).map(([memberUid, memberName]) => (
          <span key={memberUid} className={'chip t-caption' + (memberUid === uid ? ' is-active' : '')}>
            {memberName}
            {memberUid === group.ownerUid ? ' · 방장' : ''}
          </span>
        ))}
      </div>

      {spots.length === 0 ? (
        <div className="empty t-body">
          아직 그룹 지도에 담긴 곳이 없어요.
          <br />
          멤버 중 누군가 장소를 담으면 여기 모여요.
        </div>
      ) : (
        <div className="stamps">
          {[...spots]
            .sort((a, b) => a.addedAt - b.addedAt)
            .map((s) => (
              <article key={s.contentId} className={'stamp' + (s.verifiedColor ? ' is-done' : '')}>
                <span
                  className="stamp__mark"
                  style={s.verifiedColor ? { background: s.verifiedColor } : undefined}
                >
                  {s.verifiedColor ? '✓' : ''}
                </span>
                <div>
                  <p className="t-subtitle">{s.title}</p>
                  <p className="t-caption stamp__sub">
                    {s.category} · {group.members[s.addedByUid] ?? '멤버'}님이 추가
                  </p>
                </div>
              </article>
            ))}
        </div>
      )}
    </section>
  )
}
