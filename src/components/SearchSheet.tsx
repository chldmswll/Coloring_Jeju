import { useEffect, useState } from 'react'
import { areaBasedList, categoryLabelOf, CATEGORIES, searchKeyword } from '../api/tourApi'
import type { CategoryLabel } from '../api/tourApi'
import type { TourSpot } from '../types'

/**
 * "내 지도에 여행지 추가하기" — TourAPI 로 제주의 장소를 찾아 MY 지도에 담는다.
 *
 * 검색어가 비어 있으면 지역 목록을, 있으면 키워드 검색을 부른다. 담긴 여부(savedIds)는 위에서
 * 내려받는다 — 상세 시트에서도 담을 수 있어서 두 곳이 같은 상태를 봐야 하기 때문.
 */
export function SearchSheet({
  savedIds,
  onToggle,
  onOpen,
}: {
  savedIds: Set<string>
  onToggle: (spot: TourSpot) => void
  onOpen: (spot: TourSpot) => void
}) {
  const [query, setQuery] = useState('')
  const [category, setCategory] = useState<CategoryLabel>('전체')
  const [results, setResults] = useState<TourSpot[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    let cancelled = false
    setLoading(true)
    setError(null)

    // 타이핑 중에는 매 글자마다 부르지 않는다. 첫 목록은 기다릴 이유가 없어 바로 부른다.
    const delay = query.trim() ? 400 : 0
    const timer = setTimeout(async () => {
      try {
        const typeId = CATEGORIES.find((c) => c.label === category)?.typeId ?? null
        const spots = query.trim()
          ? await searchKeyword(query.trim())
          : await areaBasedList(typeId)
        if (!cancelled) setResults(spots)
      } catch (e) {
        if (!cancelled) {
          setResults([])
          setError(e instanceof Error ? e.message : '장소를 불러오지 못했어요.')
        }
      } finally {
        if (!cancelled) setLoading(false)
      }
    }, delay)

    return () => {
      cancelled = true
      clearTimeout(timer)
    }
  }, [query, category])

  // 키워드 검색은 서버에서 분류를 좁힐 수 없어 받은 뒤에 거른다.
  const visible = results.filter(
    (s) => category === '전체' || categoryLabelOf(s.contentTypeId) === category,
  )

  return (
    <section className="search">
      <input
        className="search__input t-body"
        placeholder="장소 검색 (예: 우도, 오름)"
        value={query}
        onChange={(e) => setQuery(e.target.value)}
      />

      <div className="chips">
        {CATEGORIES.map((c) => (
          <button
            key={c.label}
            className={'chip t-caption' + (category === c.label ? ' is-active' : '')}
            onClick={() => setCategory(c.label)}
          >
            {c.label}
          </button>
        ))}
      </div>

      {error && <p className="t-caption search__error">{error}</p>}
      {loading && <p className="t-caption search__hint">불러오는 중…</p>}
      {!loading && !error && visible.length === 0 && (
        <p className="t-caption search__hint">조건에 맞는 장소가 없어요.</p>
      )}

      <ul className="places">
        {visible.map((spot) => {
          const added = savedIds.has(spot.contentId)
          // 좌표가 없으면 지도에 못 찍으니 담을 수도 없다.
          const placeable = spot.lat !== null && spot.lng !== null
          return (
            <li key={spot.contentId} className="place">
              <button className="place__main" onClick={() => onOpen(spot)}>
                {spot.thumbnail || spot.image ? (
                  <img src={spot.thumbnail ?? spot.image!} alt="" />
                ) : (
                  <span className="place__thumb-empty" />
                )}
                <span>
                  <span className="t-subtitle place__title">{spot.title}</span>
                  <span className="t-caption place__sub">
                    {categoryLabelOf(spot.contentTypeId) ?? '관광지'}
                    {spot.addr1 ? ` · ${spot.addr1}` : ''}
                  </span>
                </span>
              </button>
              <button
                className={'place__add' + (added ? ' is-added' : '')}
                disabled={!placeable}
                title={placeable ? undefined : '좌표가 없어 지도에 담을 수 없어요'}
                onClick={() => onToggle(spot)}
              >
                {added ? '✓' : '+'}
              </button>
            </li>
          )
        })}
      </ul>
    </section>
  )
}
