import type { RelatedAttraction, RelatedResult } from '../api/relatedApi'
import { AddIcon, CheckIcon } from './Icons'
import type { TourSpot } from '../types'

/**
 * 장소를 여행에 담은 직후 뜨는 바텀시트 — "이 곳을 고른 사람들이 가장 많이 간 곳".
 * 추천이 하나라도 있을 때만 열린다(App 이 결과를 먼저 받아보고 띄운다). 목록에서 바로 담거나
 * 뺄 수 있고, 여기서 담은 곳은 다시 이 시트를 띄우지 않는다(끝없이 이어지지 않게).
 */
export function RelatedSheet({
  result,
  savedIds,
  onToggle,
  onOpen,
  onClose,
}: {
  result: RelatedResult
  savedIds: Set<string>
  onToggle: (spot: TourSpot) => void
  onOpen: (spot: TourSpot) => void
  onClose: () => void
}) {
  return (
    <div className="sheet-scrim" onClick={onClose}>
      <div className="sheet related" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />
        <h2 className="t-title">함께 가면 좋은 곳</h2>
        <RelatedList items={result.items} savedIds={savedIds} onToggle={onToggle} onOpen={onOpen} />

        {/* 닫기 버튼은 두지 않는다 — 시트 밖(어두운 부분)을 누르면 닫힌다. */}
        <RelatedSource baseYm={result.baseYm} />
      </div>
    </div>
  )
}

/**
 * 추천 장소 목록 — 담은 직후 시트와 장소 상세 시트가 같이 쓴다.
 *
 * 한 줄은 홈의 검색 결과와 같은 모양·동작이다: 장소를 누르면 상세 시트가 열리고, 오른쪽 +를
 * 누르면 담기고(✓), 한 번 더 누르면 빠진다. `onToggle` 이 null 이면(여행이 끝났거나 안 골랐으면)
 * 버튼 없이 보여주기만 한다.
 */
export function RelatedList({
  items,
  savedIds,
  onToggle,
  onOpen,
}: {
  items: RelatedAttraction[]
  savedIds: Set<string>
  onToggle: ((spot: TourSpot) => void) | null
  onOpen: (spot: TourSpot) => void
}) {
  return (
    <ol className="related__list">
      {items.map((it, i) => {
        const added = savedIds.has(it.spot.contentId)
        const thumb = it.spot.thumbnail ?? it.spot.image
        return (
          <li key={it.spot.contentId} className="related__item">
            <button className="related__main" onClick={() => onOpen(it.spot)}>
              <span className="related__rank">{i + 1}</span>
              {thumb ? (
                <img className="related__thumb" src={thumb} alt="" />
              ) : (
                <span className="related__thumb" />
              )}
              <span className="related__text">
                <span className="t-subtitle related__name">{it.name}</span>
                <span className="t-caption related__meta">
                  {[it.category, it.region].filter(Boolean).join(' · ')}
                </span>
              </span>
            </button>
            {onToggle && (
              <button
                className={'place__add' + (added ? ' is-added' : '')}
                aria-label={added ? `${it.name} 빼기` : `${it.name} 담기`}
                onClick={() => onToggle(it.spot)}
              >
                {added ? <CheckIcon /> : <AddIcon />}
              </button>
            )}
          </li>
        )
      })}
    </ol>
  )
}

export function RelatedSource({ baseYm }: { baseYm: string }) {
  return (
    <p className="t-caption related__source">
      한국관광공사 데이터랩 · {baseYm.slice(0, 4)}년 {Number(baseYm.slice(4))}월 기준
    </p>
  )
}
