import { useEffect, useMemo, useState } from 'react'
import { AuthScreen } from './components/AuthScreen'
import { GroupTab } from './components/GroupTab'
import { MyPage } from './components/MyPage'
import { useAuthUser } from './firebase/auth'
import { detail } from './api/tourApi'
import { categoryLabelOf } from './api/tourApi'
import { SearchSheet } from './components/SearchSheet'
import { VerifySheet } from './components/VerifySheet'
import recommendedSpots from './data/recommendedSpots.json'
import { JejuMap, type MapPin } from './map/JejuMap'
import { usePieces } from './store/pieces'
import { savedSpotsStore, useSavedSpots } from './store/savedSpots'
import type { RecommendedSpot, SavedSpot, TourSpot } from './types'
import './App.css'

const RECOMMENDED = recommendedSpots as RecommendedSpot[]

type MapTab = '추천 지도' | 'MY 지도'
type MainTab = '홈' | '스탬프' | '조각' | '그룹' | '마이'

/**
 * 로그인 여부에 따라 인증 화면과 본 앱을 가른다.
 *
 * `loading` 동안 아무것도 그리지 않는 게 중요하다 — Firebase 가 저장된 세션을 복원하기 전에
 * 판단하면, 이미 로그인한 사람에게 로그인 화면이 한 번 깜빡였다 사라진다.
 */
export default function App() {
  const { user, loading } = useAuthUser()
  if (loading) return <div className="boot t-body">불러오는 중…</div>
  if (!user) return <AuthScreen onAuthenticated={() => undefined} />
  return <MainApp user={user} />
}

function MainApp({ user }: { user: import('firebase/auth').User }) {
  const [mainTab, setMainTab] = useState<MainTab>('홈')
  const [mapTab, setMapTab] = useState<MapTab>('추천 지도')
  const [openSpot, setOpenSpot] = useState<SavedSpot | null>(null)
  const [verifying, setVerifying] = useState<SavedSpot | null>(null)
  const saved = useSavedSpots()
  const savedIds = useMemo(() => new Set(saved.map((s) => s.contentId)), [saved])

  // 추천 핀도 인증 상태는 저장소에서 읽는다 — 한쪽 탭에서만 인증된 것처럼 보이면 안 된다.
  const pins: MapPin[] = useMemo(() => {
    if (mapTab === 'MY 지도') {
      return saved.map((s) => ({
        id: s.contentId,
        title: s.title,
        imageUrl: s.image,
        emoji: '📍',
        lat: s.lat,
        lng: s.lng,
        rank: Number.POSITIVE_INFINITY,
        verifiedColor: s.verifiedColor,
      }))
    }
    return RECOMMENDED.map((r) => ({
      id: r.contentId,
      title: r.title,
      imageUrl: r.imageUrl,
      emoji: r.emoji,
      lat: r.lat,
      lng: r.lng,
      rank: r.rank,
      verifiedColor: saved.find((s) => s.contentId === r.contentId)?.verifiedColor ?? null,
    }))
  }, [mapTab, saved])

  const openFromPin = (pin: MapPin) => {
    const already = saved.find((s) => s.contentId === pin.id)
    if (already) return setOpenSpot(already)
    const rec = RECOMMENDED.find((r) => r.contentId === pin.id)
    if (rec) setOpenSpot(fromRecommended(rec))
  }

  const isOpenSaved = openSpot ? savedIds.has(openSpot.contentId) : false

  return (
    <div className="app">
      <header className="app-header">
        <h1 className="t-display">제주 컬러 지도</h1>
        <p className="t-body app-header__sub">이번 여행의 무지개를 채워보세요</p>
      </header>

      <main className="app-main">
        {mainTab === '홈' && (
          <>
            <RainbowRow saved={saved} />
            <div className="segmented" role="tablist">
              {(['추천 지도', 'MY 지도'] as MapTab[]).map((tab) => (
                <button
                  key={tab}
                  role="tab"
                  aria-selected={mapTab === tab}
                  className={'segmented__item t-subtitle' + (mapTab === tab ? ' is-active' : '')}
                  onClick={() => setMapTab(tab)}
                >
                  {tab}
                </button>
              ))}
            </div>

            {mapTab === 'MY 지도' && saved.length === 0 ? (
              <div className="empty t-body">
                아직 담은 여행지가 없어요.
                <br />
                아래에서 검색하거나 추천 지도에서 담아보세요.
              </div>
            ) : (
              <JejuMap pins={pins} onPinClick={openFromPin} />
            )}

            <div className="add-panel">
              <h2 className="t-title">내 지도에 여행지 추가하기</h2>
              <p className="t-caption app-hint">
                추천 여행지 {RECOMMENDED.length}곳 · 총 {saved.length}곳 추가됨
              </p>
              <SearchSheet
                savedIds={savedIds}
                onToggle={(spot) => {
                  if (savedIds.has(spot.contentId)) savedSpotsStore.remove(spot.contentId)
                  else {
                    const s = fromTourSpot(spot)
                    if (s) savedSpotsStore.add(s)
                  }
                }}
                onOpen={(spot) => {
                  const s = fromTourSpot(spot)
                  if (s) setOpenSpot(saved.find((v) => v.contentId === s.contentId) ?? s)
                }}
              />
            </div>
          </>
        )}

        {mainTab === '스탬프' && <StampList saved={saved} onVerify={setVerifying} />}
        {mainTab === '조각' && <PieceGrid />}
        {mainTab === '그룹' && <GroupTab user={user} />}
        {mainTab === '마이' && <MyPage user={user} />}
      </main>

      <nav className="tabbar">
        {(['홈', '스탬프', '조각', '그룹', '마이'] as MainTab[]).map((tab) => (
          <button
            key={tab}
            className={'tabbar__item t-caption' + (mainTab === tab ? ' is-active' : '')}
            onClick={() => setMainTab(tab)}
          >
            <span className="tabbar__dot" />
            {tab}
          </button>
        ))}
      </nav>

      {openSpot && (
        <PlaceSheet
          spot={openSpot}
          isSaved={isOpenSaved}
          onToggle={() =>
            isOpenSaved ? savedSpotsStore.remove(openSpot.contentId) : savedSpotsStore.add(openSpot)
          }
          onClose={() => setOpenSpot(null)}
        />
      )}

      {verifying && <VerifySheet spot={verifying} onClose={() => setVerifying(null)} />}
    </div>
  )
}

/** 무지개 6칸 — 인증해서 색을 얻은 순서대로 채운다. */
function RainbowRow({ saved }: { saved: SavedSpot[] }) {
  const earned = [...saved]
    .sort((a, b) => a.addedAt - b.addedAt)
    .map((s) => s.verifiedColor)
    .filter((c): c is string => c !== null)

  return (
    <section>
      <p className="t-subtitle rainbow__label">
        이번 여행의 무지개 · 6색 중 {Math.min(earned.length, 6)}색 수집
      </p>
      <div className="rainbow">
        {Array.from({ length: 6 }, (_, i) => (
          <span
            key={i}
            className="rainbow__slot"
            style={earned[i] ? { background: earned[i], borderColor: earned[i] } : undefined}
          />
        ))}
      </div>
    </section>
  )
}

/** 스탬프 목록 — MY 지도와 같은 장소들. 담은 곳이 곧 인증 미션이 된다. */
function StampList({
  saved,
  onVerify,
}: {
  saved: SavedSpot[]
  onVerify: (spot: SavedSpot) => void
}) {
  // 선택 상태를 따로 저장하지 않고 파생시킨다 — 인증되거나 빠진 장소가 선택으로 남지 않도록.
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const selected = saved.find((s) => s.contentId === selectedId && !s.verifiedColor) ?? null
  const done = saved.filter((s) => s.verifiedColor).length

  if (saved.length === 0) {
    return (
      <div className="empty t-body">
        아직 스탬프가 없어요.
        <br />
        홈·지도에서 여행지를 담으면 이곳에 인증 미션이 생겨요.
      </div>
    )
  }

  return (
    <section className="stamps">
      <div className="stamps__head">
        <p className="t-subtitle">
          {done} / {saved.length} 완료
        </p>
        {selected && (
          <button className="btn-primary t-button" onClick={() => onVerify(selected)}>
            인증하기
          </button>
        )}
      </div>

      <div className="progress">
        <span style={{ width: `${(done / saved.length) * 100}%` }} />
      </div>

      {saved.map((s) => {
        const isDone = Boolean(s.verifiedColor)
        return (
          <article
            key={s.contentId}
            className={
              'stamp' +
              (isDone ? ' is-done' : '') +
              (!isDone && s.contentId === selectedId ? ' is-selected' : '')
            }
            onClick={() => !isDone && setSelectedId(s.contentId === selectedId ? null : s.contentId)}
          >
            <span
              className="stamp__mark"
              style={s.verifiedColor ? { background: s.verifiedColor } : undefined}
            >
              {isDone ? '✓' : ''}
            </span>
            <div>
              <p className="t-subtitle">{s.title}</p>
              <p className="t-caption stamp__sub">
                {s.category} · {isDone ? '인증완료' : '미인증'}
              </p>
            </div>
          </article>
        )
      })}
    </section>
  )
}

/** 조각모음 — 어디서 어떤 색을 얻었는지 모아 보는 내 팔레트. */
function PieceGrid() {
  const pieces = usePieces()
  if (pieces.length === 0) {
    return (
      <div className="empty t-body">
        아직 모은 조각이 없어요.
        <br />
        스탬프에서 장소를 인증하면 이곳에 쌓여요.
      </div>
    )
  }
  return (
    <section className="pieces">
      {[...pieces]
        .sort((a, b) => b.collectedAt - a.collectedAt)
        .map((p) => (
          <article key={p.contentId} className="piece">
            {p.photo ? (
              <img src={p.photo} alt="" />
            ) : (
              <span className="piece__photo-empty" style={{ background: p.color }} />
            )}
            <span className="piece__color" style={{ background: p.color }} />
            <p className="t-caption piece__name">{p.placeName}</p>
          </article>
        ))}
    </section>
  )
}

function PlaceSheet({
  spot,
  isSaved,
  onToggle,
  onClose,
}: {
  spot: SavedSpot
  isSaved: boolean
  onToggle: () => void
  onClose: () => void
}) {
  // 목록 API 는 overview 를 주지 않는다 — 설명이 비어 있으면 상세 조회로 채운다.
  const [fetched, setFetched] = useState<string | null>(null)
  useEffect(() => {
    if (spot.description) return
    let cancelled = false
    setFetched(null)
    detail(spot.contentId)
      .then((d) => {
        if (!cancelled) setFetched(d?.overview ?? null)
      })
      .catch(() => {
        /* 설명을 못 받아도 주소 줄은 남으므로 조용히 넘어간다 */
      })
    return () => {
      cancelled = true
    }
  }, [spot.contentId, spot.description])

  const description = spot.description ?? fetched ?? '설명을 불러오는 중…'

  return (
    <div className="sheet-scrim" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />
        <div className="sheet__head">
          <h2 className="t-title">{spot.title}</h2>
          <button className="pill t-subtitle" onClick={onToggle}>
            {isSaved ? 'MY 지도에서 삭제' : '+ MY 지도에 추가'}
          </button>
        </div>
        {spot.image && <img className="sheet__hero" src={spot.image} alt="" />}
        <span className="tag t-caption">{spot.category}</span>
        <p className="t-caption sheet__addr">{spot.headline}</p>
        <p className="t-body sheet__desc">{description}</p>
      </div>
    </div>
  )
}

function fromRecommended(r: RecommendedSpot): SavedSpot {
  return {
    contentId: r.contentId,
    title: r.title,
    image: r.imageUrl,
    category: r.category,
    lat: r.lat,
    lng: r.lng,
    addedAt: Date.now(),
    headline: r.headline,
    description: r.description,
    verifiedColor: null,
  }
}

/** 좌표가 없으면 지도에 못 올리므로 담을 수도 없다 — 그래서 null 을 돌려준다. */
function fromTourSpot(s: TourSpot): SavedSpot | null {
  if (s.lat === null || s.lng === null) return null
  return {
    contentId: s.contentId,
    title: s.title,
    image: s.image ?? s.thumbnail,
    category: categoryLabelOf(s.contentTypeId) ?? '관광지',
    lat: s.lat,
    lng: s.lng,
    addedAt: Date.now(),
    headline: s.addr1,
    // 상세 시트가 detailCommon 으로 진짜 설명을 가져오도록 비워 둔다.
    description: null,
    verifiedColor: null,
  }
}
