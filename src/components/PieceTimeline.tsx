import type { TripSpot } from '../types'

/** ms 타임스탬프를 '2026.09.20', '14:32' 로 나눠 돌려준다. */
function formatDateTime(ts: number | null): { date: string; time: string } {
  if (!ts) return { date: '', time: '' }
  const d = new Date(ts)
  const pad = (n: number) => String(n).padStart(2, '0')
  return {
    date: `${d.getFullYear()}.${pad(d.getMonth() + 1)}.${pad(d.getDate())}`,
    time: `${pad(d.getHours())}:${pad(d.getMinutes())}`,
  }
}

/**
 * 세로 사진이면 폭을, 가로 사진이면 높이를 100px로 고정하고 나머지 한 쪽은 원본 비율대로
 * 자라게 한다 — 자르지도 않고 여백도 안 생기면서, 어느 쪽이 "짧은 변"인지는 사진을 직접
 * 읽어보기 전엔 알 수 없어서 로드된 뒤(onLoad)에 자연 크기(naturalWidth/Height)를 보고
 * 정한다. CSS만으로는 이 방향 분기를 할 수 없다.
 */
function fitPolaroidPhoto(img: HTMLImageElement) {
  if (img.naturalWidth >= img.naturalHeight) {
    img.style.height = '100px'
    img.style.width = 'auto'
  } else {
    img.style.width = '100px'
    img.style.height = 'auto'
  }
}

/**
 * 앨범 탭의 여행 상세 전용 — 담기만 하고 아직 인증 안 한 곳은 빼고, 실제로 사진을 찍어 인증한
 * 곳만 시간 순서대로 세로 타임라인으로 보여준다.
 *
 * 사진은 테두리 없이 카드 전체를 채우고, 장소 이름과 남긴 문구는 사진 옆에 메모처럼 붙인다.
 * (조각모음 탭은 이거랑 다르게 갈 예정이라 별도 컴포넌트로 뒀다 — App.tsx 의 PieceAlbumDetail 참고.)
 */
export function PieceTimeline({ pieces }: { pieces: TripSpot[] }) {
  const ordered = [...pieces].sort((a, b) => (a.verifiedAt ?? 0) - (b.verifiedAt ?? 0))

  if (ordered.length === 0) {
    return <div className="empty t-body">아직 인증한 곳이 없어요.</div>
  }

  return (
    <div className="timeline">
      {ordered.map((p) => {
        const { date, time } = formatDateTime(p.verifiedAt)
        return (
          <article key={p.contentId} className="timeline__item">
            <p className="t-caption timeline__meta">
              {date} · {time}
            </p>
            <div className="journal-entry">
              <div className="polaroid">
                {p.photo ? (
                  <img
                    src={p.photo}
                    alt=""
                    onLoad={(e) => fitPolaroidPhoto(e.currentTarget)}
                    ref={(el) => {
                      // onLoad 는 캐시에서 즉시 로드되면 리액트가 붙이기 전에 이미 끝나 있을 수
                      // 있다 — complete 인 경우 ref 에서 바로 한 번 더 맞춰준다.
                      if (el?.complete) fitPolaroidPhoto(el)
                    }}
                  />
                ) : (
                  <span className="polaroid__empty" style={{ background: p.verifiedColor! }} />
                )}
              </div>
              <div className="journal-note">
                <p className="t-subtitle journal-note__place">{p.title}</p>
                {p.caption && <p className="t-body journal-note__caption">{p.caption}</p>}
              </div>
            </div>
          </article>
        )
      })}
    </div>
  )
}
