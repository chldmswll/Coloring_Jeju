import { useState } from 'react'
import {
  Pamphlet,
  ThemePreview,
  COLLAGE_THEMES,
  COLLAGE_THEME_DESC,
  type CollageTheme,
} from './Pamphlet'
import { CheckIcon } from './Icons'
import type { Trip, TripSpot } from '../types'

/**
 * 앨범 — 먼저 내 여행 목록을 보여주고, 여행 하나를 고르면 그 여행에서 인증하며 찍은 사진을
 * 갤러리처럼 펼친다.
 *
 * "콜라주 생성"은 테마 고르기 → 넣을 사진 고르기 → 콜라주 한 장 순서다. 여러 테마로 만들어
 * 놓고 고르는 게 아니라, 처음에 고른 테마로 딱 한 장만 만든다.
 */

/** 안정된 빈 배열 참조 — 아직 스팟을 못 받아온 여행에 매번 새 배열을 넘기지 않도록. */
const EMPTY_SPOTS: TripSpot[] = []

/** 인증을 마치고 사진까지 남은 곳만, 최근에 찍은 사진이 위로. */
function photosOf(spots: TripSpot[]): TripSpot[] {
  return spots
    .filter((s) => s.verifiedColor && s.photo)
    .sort((a, b) => (b.verifiedAt ?? 0) - (a.verifiedAt ?? 0))
}

export function GalleryTab({
  trips,
  spotsByTrip,
}: {
  trips: Trip[]
  spotsByTrip: Record<string, TripSpot[]>
}) {
  const [openId, setOpenId] = useState<string | null>(null)
  // 보고 있던 여행이 지워지거나 나가면 find 가 비어서 자연스럽게 목록으로 돌아간다.
  const trip = trips.find((t) => t.id === openId)

  if (trip) {
    return (
      <TripGallery
        key={trip.id}
        trip={trip}
        spots={spotsByTrip[trip.id] ?? EMPTY_SPOTS}
        onBack={() => setOpenId(null)}
      />
    )
  }
  return <TripPicker trips={trips} spotsByTrip={spotsByTrip} onOpen={setOpenId} />
}

/** 여행 카드에 미리 보여줄 사진 수 — 2x2 칸. */
const PREVIEW_CELLS = 4

/**
 * 앨범 첫 화면 — 여행을 두 줄 그리드로. 카드마다 최신 사진 4장을 2x2 로 보여주고, 사진이 모자란
 * 칸은 회색 네모로 채운다(사진이 없어도 카드 모양이 똑같이 유지되게). 아래에 이름·기간·모은 색.
 */
function TripPicker({
  trips,
  spotsByTrip,
  onOpen,
}: {
  trips: Trip[]
  spotsByTrip: Record<string, TripSpot[]>
  onOpen: (tripId: string) => void
}) {
  if (trips.length === 0) {
    return (
      <div className="empty t-body">
        아직 여행이 없어요.
        <br />
        "여행" 탭에서 새 여행을 만들어보세요.
      </div>
    )
  }

  const ordered = [...trips].sort((a, b) => b.startDate.localeCompare(a.startDate))

  return (
    <section className="album-trips">
      {ordered.map((t) => {
        const photos = photosOf(spotsByTrip[t.id] ?? EMPTY_SPOTS)
        return (
          <button key={t.id} className="album-trip" onClick={() => onOpen(t.id)}>
            <span className="album-trip__mosaic" aria-hidden="true">
              {Array.from({ length: PREVIEW_CELLS }, (_, i) =>
                photos[i] ? (
                  <img key={i} className="album-trip__cell" src={photos[i].photo!} alt="" />
                ) : (
                  <span key={i} className="album-trip__cell is-empty" />
                ),
              )}
            </span>
            <span className="album-trip__name">{t.name}</span>
            <span className="t-caption album-trip__sub">
              {t.startDate} ~ {t.endDate}
            </span>
            {photos.length > 0 && (
              <span className="album-trip__dots" aria-hidden="true">
                {photos.slice(0, 8).map((p) => (
                  <span
                    key={p.contentId}
                    className="album-trip__dot"
                    style={{ background: p.verifiedColor ?? undefined }}
                  />
                ))}
              </span>
            )}
          </button>
        )
      })}
    </section>
  )
}

/** 여행 하나의 사진 갤러리 — 여기서 콜라주를 만든다. */
function TripGallery({
  trip,
  spots,
  onBack,
}: {
  trip: Trip
  spots: TripSpot[]
  onBack: () => void
}) {
  const photos = photosOf(spots)
  const [choosingTheme, setChoosingTheme] = useState(false)
  // 테마를 고르면 사진 고르기로 넘어간다 — null 이면 그냥 둘러보는 중.
  const [pickTheme, setPickTheme] = useState<CollageTheme | null>(null)
  const [selected, setSelected] = useState<Set<string>>(new Set())
  const [made, setMade] = useState<{ theme: CollageTheme; pieces: TripSpot[] } | null>(null)
  const picking = pickTheme !== null
  const allSelected = photos.length > 0 && selected.size === photos.length

  if (made) {
    return (
      <section className="collage">
        <div className="collage__head">
          <button className="t-subtitle group__back" onClick={() => setMade(null)}>
            ‹ {trip.name}
          </button>
        </div>
        <Pamphlet
          input={{
            title: trip.name,
            period: `${trip.startDate} ~ ${trip.endDate}`,
            pieces: made.pieces,
            theme: made.theme,
          }}
        />
      </section>
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

  function stopPicking() {
    setPickTheme(null)
    setSelected(new Set())
  }

  return (
    <section className="gallery">
      <div className="gallery__head">
        {picking ? (
          <>
            <button className="t-subtitle group__back" onClick={stopPicking}>
              취소
            </button>
            <button
              className="btn-primary t-button"
              disabled={selected.size === 0}
              onClick={() => {
                if (!pickTheme) return
                setMade({
                  theme: pickTheme,
                  pieces: photos.filter((p) => selected.has(p.contentId)),
                })
                stopPicking()
              }}
            >
              만들기 {selected.size > 0 ? `(${selected.size})` : ''}
            </button>
          </>
        ) : (
          <>
            <button className="t-subtitle group__back" onClick={onBack}>
              ‹ 여행 목록
            </button>
            {photos.length > 0 && (
              <button className="btn-primary t-button" onClick={() => setChoosingTheme(true)}>
                콜라주 생성
              </button>
            )}
          </>
        )}
      </div>

      <div>
        <h2 className="t-title gallery__title">{trip.name}</h2>
        <p className="t-caption app-hint">
          {trip.startDate} ~ {trip.endDate}
        </p>
      </div>

      {picking && (
        <div className="gallery__pick-bar">
          <p className="t-caption app-hint">
            '{pickTheme}' 테마로 만들어요. 콜라주에 넣을 사진을 골라주세요.
          </p>
          {/* 다 골라져 있으면 같은 자리에서 한 번에 풀 수 있게 한다. */}
          <button
            className="t-subtitle group__back gallery__pick-all"
            onClick={() =>
              setSelected(allSelected ? new Set() : new Set(photos.map((p) => p.contentId)))
            }
          >
            {allSelected ? '전체 해제' : '전체 선택'}
          </button>
        </div>
      )}

      {photos.length === 0 ? (
        <div className="empty t-body">
          아직 이 여행에서 찍은 사진이 없어요.
          <br />
          스탬프 탭에서 여행지를 인증하면 여기에 쌓여요.
        </div>
      ) : (
        <ul className="gallery__grid">
          {photos.map((spot) => {
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
      )}

      {choosingTheme && (
        <ThemePickerModal
          onClose={() => setChoosingTheme(false)}
          onPick={(theme) => {
            setChoosingTheme(false)
            setPickTheme(theme)
          }}
        />
      )}
    </section>
  )
}

/** 콜라주 테마 고르기 — 테마마다 작은 예시 그림을 한 줄로 늘어놓아 한눈에 비교하게 한다. */
function ThemePickerModal({
  onPick,
  onClose,
}: {
  onPick: (theme: CollageTheme) => void
  onClose: () => void
}) {
  return (
    <div className="modal-scrim" onClick={onClose}>
      <div className="modal" onClick={(e) => e.stopPropagation()}>
        <button className="modal__close" aria-label="닫기" onClick={onClose}>
          ✕
        </button>
        <h2 className="t-title">콜라주 테마 고르기</h2>
        <div className="theme-grid">
          {COLLAGE_THEMES.map((t) => (
            <button key={t} className="theme-option" onClick={() => onPick(t)}>
              <ThemePreview theme={t} />
              <span className="t-subtitle">{t}</span>
              <span className="t-caption theme-option__desc">{COLLAGE_THEME_DESC[t]}</span>
            </button>
          ))}
        </div>
      </div>
    </div>
  )
}
