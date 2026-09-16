import type { TourSpot } from '../types'

/**
 * "함께 가면 좋은 곳" — 한국관광 데이터랩의 연관 관광지 순위를, 앱이 쓰는 TourAPI 장소로 미리
 * 이어 붙여둔 인덱스(src/data/relatedIndex.json)에서 읽는다.
 *
 * 인덱스는 `npm run related:index`(scripts/build-related-index.mjs)가 만든다. 두 API 가 같은 곳을
 * 다른 이름으로 부르는 문제를 거기서 한 번에 풀어두기 때문에, 앱은 네트워크 호출 없이 바로
 * 결과를 낸다. 데이터가 월 단위로 갱신되니 가끔 다시 돌려서 커밋하면 된다.
 */

interface IndexPlace {
  contentTypeId: string
  title: string
  addr1: string
  lat: number
  lng: number
  image: string | null
  thumbnail: string | null
  category: string
  region: string
}

interface RelatedIndex {
  baseYm: string
  places: Record<string, IndexPlace>
  related: Record<string, string[]>
}

export interface RelatedAttraction {
  /** 괄호 속 부연("만장굴 (제주도 국가지질공원)")을 뗀 표시용 이름. */
  name: string
  /** "자연공원", "테마공원" 같은 데이터랩 소분류. */
  category: string
  region: string
  spot: TourSpot
}

export interface RelatedResult {
  /** 'YYYYMM' — 어느 달 데이터인지 화면에 출처로 적는다. */
  baseYm: string
  items: RelatedAttraction[]
}

// 첫 화면 번들에 넣지 않고, 처음 쓸 때 따로 받아온다.
let index: Promise<RelatedIndex> | null = null
const loadIndex = () =>
  (index ??= import('../data/relatedIndex.json').then((m) => m.default as RelatedIndex))

/** `contentId` 를 고른 사람들이 많이 간 관광지를 순위대로. `excludeIds` 는 이미 여행에 담긴 곳. */
export async function relatedAttractions(
  contentId: string,
  excludeIds: Set<string>,
  limit = 5,
): Promise<RelatedResult | null> {
  const { baseYm, places, related } = await loadIndex()
  const ids = related[contentId]
  if (!ids) return null

  const items = ids
    .filter((id) => !excludeIds.has(id))
    .slice(0, limit)
    .map((id): RelatedAttraction => {
      const p = places[id]
      return {
        name: p.title.replace(/\s*[[(（].*?[\])）]/g, '').trim() || p.title,
        category: p.category,
        region: p.region,
        spot: {
          contentId: id,
          contentTypeId: p.contentTypeId,
          title: p.title,
          addr1: p.addr1,
          lat: p.lat,
          lng: p.lng,
          image: p.image,
          thumbnail: p.thumbnail,
          overview: null,
        },
      }
    })
  return { baseYm, items }
}
