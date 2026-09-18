import { useMemo, useState } from 'react'
import { Pamphlet, COLLAGE_THEMES, type CollageTheme } from './Pamphlet'
import { CheckIcon } from './Icons'
import type { Trip, TripSpot } from '../types'

/**
 * 앨범 — 인증하며 찍은 사진을 갤러리처럼 쭉 펼쳐 보여준다.
 * 오른쪽 위 "콜라주 생성"을 누르면 사진을 골라 한 장짜리 콜라주로 묶을 수 있다.
 *
 * 콜라주 제목은 고른 사진이 한 여행에서 나왔으면 그 여행 이름을, 여러 여행에 걸쳐 있으면
 * 사진을 찍은 기간으로 대신한다.
 */

interface Shot {
  spot: TripSpot
  trip: Trip
}

function ymd(ts: number): string {
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function GalleryTab({
  trips,
  spotsByTrip,
}: {
  trips: Trip[]
  spotsByTrip: Record<string, TripSpot[]>
}) {
  const [picking, setPicking] = useState(false)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [made, setMade] = useState<Shot[] | null>(null)
  const [theme, setTheme] = useState<CollageTheme>('노트')

  // 최근에 찍은 사진이 위로. 여행이 지워지면 사진도 같이 사라진다.
  const shots = useMemo(() => {
    const all: Shot[] = []
    for (const trip of trips) {
      for (const spot of spotsByTrip[trip.id] ?? []) {
        if (spot.verifiedColor && spot.photo) all.push({ spot, trip })
      }
    }
    return all.sort((a, b) => (b.spot.verifiedAt ?? 0) - (a.spot.verifiedAt ?? 0))
  }, [trips, spotsByTrip])

  if (made) {
    const pieces = made.map((s) => s.spot)
    const tripIds = new Set(made.map((s) => s.trip.id))
    const onlyTrip = tripIds.size === 1 ? made[0].trip : null
    const dates = pieces.map((p) => p.verifiedAt ?? 0).filter(Boolean).sort()
    return (
      <section className="collage">
        <div className="collage__head">
          <button className="t-subtitle group__back" onClick={() => setMade(null)}>
            ‹ 앨범
          </button>
          <div className="segmented" role="tablist">
            {COLLAGE_THEMES.map((t) => (
              <button
                key={t}
                role="tab"
                aria-selected={theme === t}
                className={'segmented__item t-subtitle' + (theme === t ? ' is-active' : '')}
                onClick={() => setTheme(t)}
              >
                {t}
              </button>
            ))}
          </div>
        </div>

        <Pamphlet
          input={{
            title: onlyTrip ? onlyTrip.name : '제주에서 모은 색',
            period: onlyTrip
              ? `${onlyTrip.startDate} ~ ${onlyTrip.endDate}`
              : dates.length
                ? `${ymd(dates[0])} ~ ${ymd(dates[dates.length - 1])}`
                : '',
            photographers: onlyTrip ? Object.values(onlyTrip.members).join(', ') : undefined,
            pieces,
            theme,
          }}
        />
      </section>
    )
  }

  if (shots.length === 0) {
    return (
      <div className="empty t-body">
        아직 찍은 사진이 없어요.
        <br />
        스탬프 탭에서 여행지를 인증하면 여기에 쌓여요.
      </div>
    )
  }

  function toggle(contentId: string) {
    setSelected((prev) => {
      const next = new Set(prev)
      if (next.has(contentId)) next.delete(contentId)
      else next.add(contentId)
      return next
    })
  }

  return (
    <section className="gallery">
      <div className="gallery__head">
        {picking ? (
          <>
            <button
              className="t-subtitle group__back"
              onClick={() => {
                setPicking(false)
                setSelected(new Set())
              }}
            >
              취소
            </button>
            <button
              className="btn-primary t-button"
              disabled={selected.size === 0}
              onClick={() => {
                setMade(shots.filter((s) => selected.has(s.spot.contentId)))
                setPicking(false)
                setSelected(new Set())
              }}
            >
              만들기 {selected.size > 0 ? `(${selected.size})` : ''}
            </button>
          </>
        ) : (
          <>
            <p className="t-caption section-label gallery__count">사진 {shots.length}장</p>
            <button className="btn-primary t-button" onClick={() => setPicking(true)}>
              콜라주 생성
            </button>
          </>
        )}
      </div>

      {picking && <p className="t-caption app-hint">콜라주에 넣을 사진을 골라주세요.</p>}

      <ul className="gallery__grid">
        {shots.map(({ spot }) => {
          const isSelected = selected.has(spot.contentId)
          return (
            <li key={spot.contentId} className="gallery__cell">
              <button
                className={'gallery__shot' + (isSelected ? ' is-selected' : '')}
                style={{ borderColor: spot.verifiedColor ?? undefined }}
                onClick={() => picking && toggle(spot.contentId)}
              >
                <img src={spot.photo!} alt={spot.title} />
                {picking && (
                  <span className={'gallery__pick' + (isSelected ? ' is-selected' : '')}>
                    {isSelected && <CheckIcon />}
                  </span>
                )}
              </button>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
