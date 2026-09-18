import { Fragment, useEffect, useMemo, useRef, useState } from 'react'
import { AuthScreen } from './components/AuthScreen'
import { MyPage } from './components/MyPage'
import { useAuthUser } from './firebase/auth'
import { detail } from './api/tourApi'
import { categoryLabelOf } from './api/tourApi'
import { SearchSheet } from './components/SearchSheet'
import { VerifySheet } from './components/VerifySheet'
import recommendedSpots from './data/recommendedSpots.json'
import { JejuMap, type MapPin } from './map/JejuMap'
import { HomeIcon, StampIcon, GalleryIcon, PlaneIcon, MyIcon } from './components/TabIcons'
import { AlbumTab } from './components/AlbumTab'
import { AddIcon } from './components/Icons'
import { GalleryTab } from './components/GalleryTab'
import { RelatedSheet } from './components/RelatedSheet'
import { relatedAttractions, type RelatedResult } from './api/relatedApi'
import {
  addTripSpot,
  removeTripSpot,
  tripStatus,
  watchAllTripSpots,
  watchMyTrips,
} from './firebase/trips'
import type { RecommendedSpot, Trip, TripSpot, TourSpot } from './types'
import './App.css'

const RECOMMENDED = recommendedSpots as RecommendedSpot[]

type MainTab = '홈' | '스탬프' | '앨범' | '여행' | '마이'
/** 홈 화면 지도 보기 — 추천 지도(browsing)와 여행 지도(내가 고른 여행에 담긴 곳)는 서로 다른 것.
 * 어떤 여행이 "현재 작업 중인 여행"인지는 헤더의 드롭다운(전역)이 따로 정한다 — 그래서 추천 지도를
 * 보는 중에도 "추가하기"는 항상 그 여행에 들어간다. */
type HomeMapView = '추천 지도' | '여행 지도'

const TAB_ICON: Record<MainTab, typeof HomeIcon> = {
  홈: HomeIcon,
  스탬프: StampIcon,
  앨범: GalleryIcon,
  여행: PlaneIcon,
  마이: MyIcon,
}

/** 안정된 빈 배열 참조 — 매 렌더 새 배열을 만들면 이를 의존하는 useMemo가 무의미해진다. */
const EMPTY_SPOTS: TripSpot[] = []

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
  const [homeMapView, setHomeMapView] = useState<HomeMapView>('추천 지도')
  const [openSpot, setOpenSpot] = useState<TripSpot | null>(null)
  const [verifying, setVerifying] = useState<TripSpot | null>(null)
  // "함께 가면 좋은 곳" — 담은 직후 추천을 먼저 받아보고, 하나라도 있을 때만 시트를 띄운다.
  // 추천이 없는 장소는 아무 일도 없었던 것처럼 넘어간다.
  const [related, setRelated] = useState<{
    base: TripSpot
    tripId: string
    result: RelatedResult
  } | null>(null)
  const relatedRequest = useRef(0)

  // 여행 목록 — 홈 드롭다운, 여행 탭, 앨범(조각모음), 마이페이지가 전부 이 하나를 같이 본다.
  const [trips, setTrips] = useState<Trip[]>([])
  useEffect(() => watchMyTrips(user.uid, setTrips), [user.uid])

  // 홈 화면에서 고른 여행. 드롭다운에 "선택 안 함"이 없으므로 여행이 하나라도 있으면 항상
  // 무언가 골라져 있어야 한다. `selectedTripId`가 아직 없거나(사용자가 고르기 전) 가리키는
  // 여행이 없어지면(삭제 등) — 오늘 날짜가 속한 진행중 여행을 우선, 없으면 가장 최근 여행으로
  // 매 렌더 다시 계산한다. 한 번만 도는 effect+ref로 하면 타이밍에 따라 반영이 안 될 수 있어
  // 아예 파생값으로 뒀다 — 사용자가 직접 고른 뒤엔 selectedTripId가 그 여행을 가리키므로
  // 그대로 존중된다.
  const [selectedTripId, setSelectedTripId] = useState<string | null>(null)
  const explicitTrip = trips.find((t) => t.id === selectedTripId) ?? null
  const defaultTrip =
    trips.find((t) => tripStatus(t) === 'ongoing') ??
    [...trips].sort((a, b) => b.startDate.localeCompare(a.startDate))[0] ??
    null
  const selectedTrip = explicitTrip ?? defaultTrip

  // 모든 여행의 스팟을 같이 구독한다 — 마이페이지 통계와 조각모음 앨범뷰에 쓰인다.
  // 트립 "구성"(추가/삭제)이 바뀔 때만 다시 구독한다 — 이름 등 다른 필드 변경으로 매번
  // 다시 구독하지 않도록 id 집합만 키로 쓴다.
  const tripIdsKey = [...trips.map((t) => t.id)].sort().join(',')
  const [spotsByTrip, setSpotsByTrip] = useState<Record<string, TripSpot[]>>({})
  useEffect(() => {
    const ids = tripIdsKey ? tripIdsKey.split(',') : []
    return watchAllTripSpots(ids, setSpotsByTrip)
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [tripIdsKey])

  // 드롭다운 폭 — 매번 선택에 따라 크기가 바뀌면 헤더가 들썩여서, 있는 여행 이름 중 가장 긴
  // 것에 맞춰 한 번에 고정한다. 'ch'(숫자 '0' 폭 기준)는 한글 글자 폭의 절반 정도라 실제보다
  // 훨씬 좁게 계산돼서 글자가 잘렸었다 — 한글 한 글자가 대략 1em이라 'em'으로 계산한다.
  const tripSelectWidthEm = useMemo(
    () => Math.max(4, ...trips.map((t) => t.name.length)) + 3,
    [trips],
  )

  const tripSpots = useMemo(
    () => (selectedTrip ? spotsByTrip[selectedTrip.id] ?? EMPTY_SPOTS : EMPTY_SPOTS),
    [selectedTrip, spotsByTrip],
  )
  const allSpots = useMemo(() => Object.values(spotsByTrip).flat(), [spotsByTrip])
  const savedIds = useMemo(() => new Set(tripSpots.map((s) => s.contentId)), [tripSpots])
  // 여행 기간이 끝난(다녀옴) 여행은 기록을 더 이상 고치지 못하게 한다 — 여행지 추가/삭제, 미션 수행 모두 막는다.
  const tripEnded = selectedTrip ? tripStatus(selectedTrip) === 'past' : false

  const pins: MapPin[] = useMemo(() => {
    if (homeMapView === '여행 지도') {
      return tripSpots.map((s) => ({
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
    // 추천 지도 — 지금 고른 여행에 이미 담아 인증까지 한 곳이면 여기서도 컬러로 보여준다.
    return RECOMMENDED.map((r) => ({
      id: r.contentId,
      title: r.title,
      imageUrl: r.imageUrl,
      emoji: r.emoji,
      lat: r.lat,
      lng: r.lng,
      rank: r.rank,
      verifiedColor: tripSpots.find((s) => s.contentId === r.contentId)?.verifiedColor ?? null,
    }))
  }, [homeMapView, tripSpots])

  const openFromPin = (pin: MapPin) => {
    const already = tripSpots.find((s) => s.contentId === pin.id)
    if (already) return setOpenSpot(already)
    const rec = RECOMMENDED.find((r) => r.contentId === pin.id)
    if (rec) setOpenSpot(fromRecommended(rec, user.uid))
  }

  const isOpenSaved = openSpot ? savedIds.has(openSpot.contentId) : false

  function suggestRelated(base: TripSpot, tripId: string) {
    // 인덱스 파일을 처음 받는 동안 다른 곳을 또 담으면, 마지막으로 담은 곳의 추천만 띄운다.
    const token = ++relatedRequest.current
    const exclude = new Set(savedIds)
    exclude.add(base.contentId)
    relatedAttractions(base.contentId, exclude)
      .then((result) => {
        if (token !== relatedRequest.current || !result || result.items.length === 0) return
        setRelated({ base, tripId, result })
      })
      .catch(() => {
        /* 추천은 덤이라, 못 받아와도 담기 자체는 이미 끝났으니 조용히 넘어간다 */
      })
  }

  return (
    <div className="app">
      <header className="app-header">
        <div className="app-header__row">
          <h1 className="t-display">컬러링 제주</h1>

          {/* 지금 "작업 중인 여행" — 홈·스탬프에서만 쓰이고 둘이 이 선택을 같이 본다.
              조각모음은 여행 하나를 고르는 화면이 아니라 전부 앨범으로 늘어놓는 화면이라 필요 없다. */}
          {(mainTab === '홈' || mainTab === '스탬프') && (
            <select
              className="trip-select t-caption"
              style={{ width: `${tripSelectWidthEm}em` }}
              value={selectedTrip?.id ?? ''}
              onChange={(e) => setSelectedTripId(e.target.value || null)}
            >
              {[...trips]
                .sort((a, b) => b.startDate.localeCompare(a.startDate))
                .map((t) => (
                  <option key={t.id} value={t.id}>
                    {t.name}
                  </option>
                ))}
            </select>
          )}
        </div>
        <p className="t-body app-header__sub">제주에서 만난 색을 모아보세요</p>
      </header>

      <main className="app-main">
        {mainTab === '홈' && (
          <>
            <div className="segmented" role="tablist">
              {(['추천 지도', '여행 지도'] as HomeMapView[]).map((view) => (
                <button
                  key={view}
                  role="tab"
                  aria-selected={homeMapView === view}
                  className={'segmented__item t-subtitle' + (homeMapView === view ? ' is-active' : '')}
                  onClick={() => setHomeMapView(view)}
                >
                  {view}
                </button>
              ))}
            </div>

            {homeMapView === '여행 지도' && !selectedTrip ? (
              <div className="empty t-body">
                여행을 선택해주세요.
                <br />
                위에서 여행을 고르거나 "여행" 탭에서 새로 만들어보세요.
              </div>
            ) : (
              <JejuMap pins={pins} onPinClick={openFromPin} />
            )}
            {homeMapView === '여행 지도' && selectedTrip && tripSpots.length === 0 && (
              <p className="t-caption app-hint">
                아직 이 여행에 담긴 곳이 없어요. 아래에서 검색해 담아보세요.
              </p>
            )}

            <div className="add-panel">
              <h2 className="t-title">여행지 추가하기</h2>
              {selectedTrip ? (
                tripEnded ? (
                  <p className="t-caption app-hint">
                    여행 기간이 끝나 더 이상 여행지를 추가하거나 뺄 수 없어요.
                  </p>
                ) : (
                  <>
                    <SearchSheet
                      savedIds={savedIds}
                      onToggle={(spot) => {
                        if (savedIds.has(spot.contentId)) void removeTripSpot(selectedTrip.id, spot.contentId)
                        else {
                          const s = fromTourSpot(spot, user.uid)
                          if (s) {
                            void addTripSpot(selectedTrip.id, s)
                            suggestRelated(s, selectedTrip.id)
                          }
                        }
                      }}
                      onOpen={(spot) => {
                        const s = fromTourSpot(spot, user.uid)
                        if (s) setOpenSpot(tripSpots.find((v) => v.contentId === s.contentId) ?? s)
                      }}
                    />
                  </>
                )
              ) : (
                <p className="t-caption app-hint">
                  여행을 선택하거나 "여행" 탭에서 새 여행을 만들어주세요.
                </p>
              )}
            </div>
          </>
        )}

        {mainTab === '스탬프' &&
          (selectedTrip ? (
            <StampList saved={tripSpots} onVerify={setVerifying} locked={tripEnded} />
          ) : (
            <div className="empty t-body">
              여행을 선택해주세요.
              <br />
              홈에서 여행을 고르거나 "여행" 탭에서 새로 만들어보세요.
            </div>
          ))}

        {mainTab === '앨범' && <GalleryTab trips={trips} spotsByTrip={spotsByTrip} />}
        {mainTab === '여행' && (
          <AlbumTab user={user} trips={trips} onTripSelect={setSelectedTripId} />
        )}
        {mainTab === '마이' && (
          <MyPage user={user} allSpots={allSpots} tripCount={trips.length} />
        )}
      </main>

      <nav className="tabbar">
        {(['홈', '스탬프', '앨범', '여행', '마이'] as MainTab[]).map((tab) => {
          const TabIcon = TAB_ICON[tab]
          return (
            <button
              key={tab}
              className={'tabbar__item t-caption' + (mainTab === tab ? ' is-active' : '')}
              onClick={() => setMainTab(tab)}
            >
              <TabIcon className="tabbar__icon" />
              {tab}
            </button>
          )
        })}
      </nav>

      {openSpot && (
        <PlaceSheet
          spot={openSpot}
          isSaved={isOpenSaved}
          onToggle={
            selectedTrip && !tripEnded
              ? () => {
                  if (isOpenSaved) void removeTripSpot(selectedTrip.id, openSpot.contentId)
                  else {
                    void addTripSpot(selectedTrip.id, openSpot)
                    suggestRelated(openSpot, selectedTrip.id)
                  }
                  setOpenSpot(null)
                }
              : null
          }
          lockedReason={selectedTrip && tripEnded ? '여행이 끝나 더 이상 담거나 뺄 수 없어요' : undefined}
          onClose={() => setOpenSpot(null)}
        />
      )}

      {related && (
        <RelatedSheet
          base={related.base}
          result={related.result}
          savedIds={new Set((spotsByTrip[related.tripId] ?? []).map((s) => s.contentId))}
          onAdd={(spot) => {
            const s = fromTourSpot(spot, user.uid)
            if (s) void addTripSpot(related.tripId, s)
          }}
          onClose={() => setRelated(null)}
        />
      )}

      {verifying && selectedTrip && (
        <VerifySheet tripId={selectedTrip.id} spot={verifying} onClose={() => setVerifying(null)} />
      )}
    </div>
  )
}

/** 스탬프 목록 — 현재 선택된 여행에 담긴 곳들. 담은 곳이 곧 인증 미션이 된다. */
function StampList({
  saved,
  onVerify,
  locked,
}: {
  saved: TripSpot[]
  onVerify: (spot: TripSpot) => void
  locked: boolean
}) {
  // 선택 상태를 따로 저장하지 않고 파생시킨다 — 인증되거나 빠진 장소가 선택으로 남지 않도록.
  const [selectedId, setSelectedId] = useState<string | null>(null)
  const selected = !locked ? saved.find((s) => s.contentId === selectedId && !s.verifiedColor) ?? null : null
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

  // 완료된 곳은 이미 끝난 결과라 아래로, 아직 할 일(미완료)이 위로 오게 하고 가로선으로 나눈다.
  const ordered = [...saved].sort((a, b) => Number(Boolean(a.verifiedColor)) - Number(Boolean(b.verifiedColor)))
  const doneStartIndex = ordered.findIndex((s) => s.verifiedColor)

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

      {locked && (
        <p className="t-caption app-hint">여행 기간이 끝나 더 이상 미션을 수행할 수 없어요.</p>
      )}

      <div className="progress">
        <span style={{ width: `${(done / saved.length) * 100}%` }} />
      </div>

      <div className="stamps-list">
        {ordered.map((s, i) => {
          const isDone = Boolean(s.verifiedColor)
          return (
            <Fragment key={s.contentId}>
              {i === doneStartIndex && doneStartIndex > 0 && <hr className="stamps-divider" />}
              <StampCard
                spot={s}
                selected={s.contentId === selectedId}
                onClick={
                  isDone || locked
                    ? undefined
                    : () => setSelectedId(s.contentId === selectedId ? null : s.contentId)
                }
              />
            </Fragment>
          )
        })}
      </div>
    </section>
  )
}

function StampCard({
  spot,
  selected,
  onClick,
}: {
  spot: TripSpot
  selected: boolean
  onClick: (() => void) | undefined
}) {
  const isDone = Boolean(spot.verifiedColor)
  return (
    <article
      className={'stamp' + (isDone ? ' is-done' : '') + (selected ? ' is-selected' : '')}
      onClick={onClick}
    >
      <span className="stamp__mark-wrap">
        <span className="stamp__mark">
          {/* 인증 전엔 여행지 사진을 흑백으로, 인증하면 컬러로 보여준다 — 업로드한 인증 사진이 아니다. */}
          <StampPhoto src={spot.image} grayscale={!isDone} />
        </span>
      </span>
      <p className="t-title stamp__name">{spot.title}</p>
      {!isDone && (
        <span className="stamp__chevron" aria-hidden="true">
          ›
        </span>
      )}
    </article>
  )
}

/** 사진이 없거나 로드에 실패하면 깨진 이미지 아이콘 대신 구름 이모지로 대신한다. */
function StampPhoto({ src, grayscale }: { src: string | null; grayscale: boolean }) {
  const [broken, setBroken] = useState(false)
  if (!src || broken) return <span aria-hidden="true">☁️</span>
  return (
    <img
      className={'stamp__mark-photo' + (grayscale ? ' is-grayscale' : '')}
      src={src}
      alt=""
      onError={() => setBroken(true)}
    />
  )
}


function PlaceSheet({
  spot,
  isSaved,
  onToggle,
  lockedReason,
  onClose,
}: {
  spot: TripSpot
  isSaved: boolean
  onToggle: (() => void) | null
  lockedReason?: string
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
          {onToggle ? (
            <button className="pill t-subtitle" onClick={onToggle}>
              {isSaved ? (
                '이 여행에서 삭제'
              ) : (
                <>
                  <AddIcon className="icon-inline" /> 이 여행에 추가
                </>
              )}
            </button>
          ) : (
            <span className="t-caption">{lockedReason ?? '여행을 고르면 담을 수 있어요'}</span>
          )}
        </div>
        {spot.image && <img className="sheet__hero" src={spot.image} alt="" />}
        <span className="tag t-caption">{spot.category}</span>
        <p className="t-caption sheet__addr">{spot.headline}</p>
        <p className="t-body sheet__desc">{description}</p>
      </div>
    </div>
  )
}

function fromRecommended(r: RecommendedSpot, uid: string): TripSpot {
  return {
    contentId: r.contentId,
    title: r.title,
    image: r.imageUrl,
    category: r.category,
    lat: r.lat,
    lng: r.lng,
    addedAt: Date.now(),
    addedByUid: uid,
    headline: r.headline,
    description: r.description,
    verifiedColor: null,
    photo: null,
    caption: null,
    verifiedAt: null,
  }
}

/** 좌표가 없으면 지도에 못 올리므로 담을 수도 없다 — 그래서 null 을 돌려준다. */
function fromTourSpot(s: TourSpot, uid: string): TripSpot | null {
  if (s.lat === null || s.lng === null) return null
  return {
    contentId: s.contentId,
    title: s.title,
    image: s.image ?? s.thumbnail,
    category: categoryLabelOf(s.contentTypeId) ?? '관광지',
    lat: s.lat,
    lng: s.lng,
    addedAt: Date.now(),
    addedByUid: uid,
    headline: s.addr1,
    // 상세 시트가 detailCommon 으로 진짜 설명을 가져오도록 비워 둔다.
    description: null,
    verifiedColor: null,
    photo: null,
    caption: null,
    verifiedAt: null,
  }
}
