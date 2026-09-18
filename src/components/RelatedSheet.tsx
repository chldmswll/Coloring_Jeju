import type { RelatedResult } from '../api/relatedApi'
import { AddIcon, CheckIcon } from './Icons'
import type { TourSpot, TripSpot } from '../types'

/** 받침이 있으면 "을", 없으면 "를". 한글로 안 끝나면 둘 다 적는다. */
function withObjectParticle(word: string): string {
  const code = word.charCodeAt(word.length - 1) - 0xac00
  if (code < 0 || code > 11171) return `${word}을(를)`
  return `${word}${code % 28 === 0 ? '를' : '을'}`
}

/**
 * 장소를 여행에 담은 직후 뜨는 바텀시트 — "이 곳을 고른 사람들이 가장 많이 간 곳".
 * 추천이 하나라도 있을 때만 열린다(App 이 결과를 먼저 받아보고 띄운다). 목록에서 바로 담을 수
 * 있고, 여기서 담은 곳은 다시 이 시트를 띄우지 않는다(끝없이 이어지지 않게).
 */
export function RelatedSheet({
  base,
  result,
  savedIds,
  onAdd,
  onClose,
}: {
  base: TripSpot
  result: RelatedResult
  savedIds: Set<string>
  onAdd: (spot: TourSpot) => void
  onClose: () => void
}) {
  const baseName = base.title.replace(/\s*[[(（].*?[\])）]/g, '').trim() || base.title

  return (
    <div className="sheet-scrim" onClick={onClose}>
      <div className="sheet related" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />

        <p className="t-caption related__done">
          <span className="related__done-check">
            <CheckIcon />
          </span>
          {withObjectParticle(baseName)} 여행에 담았어요
        </p>
        <h2 className="t-title related__title">함께 가면 좋은 곳</h2>
        <p className="t-body related__lead">이 여행지를 선택한 사람들이 가장 많이 간 곳이에요</p>

        <ol className="related__list">
          {result.items.map((it, i) => {
            const added = savedIds.has(it.spot.contentId)
            const thumb = it.spot.thumbnail ?? it.spot.image
            return (
              <li key={it.spot.contentId} className="related__item">
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
                <button
                  className={'related__add t-caption' + (added ? ' is-added' : '')}
                  disabled={added}
                  onClick={() => onAdd(it.spot)}
                >
                  {added ? (
                    <>
                      <CheckIcon className="icon-inline" /> 담음
                    </>
                  ) : (
                    <>
                      <AddIcon className="icon-inline" /> 담기
                    </>
                  )}
                </button>
              </li>
            )
          })}
        </ol>

        <p className="t-caption related__source">
          한국관광공사 데이터랩 · {result.baseYm.slice(0, 4)}년 {Number(result.baseYm.slice(4))}월 기준
        </p>

        <button className="btn-primary t-button related__close" onClick={onClose}>
          확인
        </button>
      </div>
    </div>
  )
}
