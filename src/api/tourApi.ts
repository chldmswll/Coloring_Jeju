import type { TourSpot } from '../types'

/**
 * TourAPI(KorService2) 클라이언트.
 *
 * 브라우저에서 apis.data.go.kr 을 직접 부르면 403 이 돌아온다 — Origin 헤더가 붙는 요청을
 * 막고 CORS 헤더도 주지 않기 때문. 그래서 모든 호출이 `/api/tour/*` 를 거친다. 개발 중에는
 * Vite 개발 서버가(vite.config.ts), 배포 후에는 서버리스 함수가 그 자리를 대신한다.
 * serviceKey 는 그쪽에서 붙으므로 이 파일에는 키가 없다.
 */

/**
 * 지도에 올릴 만한 곳으로 인정하는 contenttypeid.
 * 12 관광지 / 14 문화시설 / 15 축제공연행사 / 28 레포츠.
 *
 * 쇼핑(38)·음식점과 카페(39)·숙박(32)은 일부러 뺐다. 제주의 색을 모으는 지도인데 마트나
 * 프랜차이즈 카페가 핀으로 꽂힐 이유가 없다. 여행코스(25)도 장소가 아니라 경로라 제외한다.
 * TourAPI 는 이것들을 한 목록에 섞어 주므로 여기서 한 번 걸러 UI 전체가 같은 기준을 쓰게 한다.
 */
const ATTRACTION_TYPE_IDS = new Set(['12', '14', '15', '28'])

export const CATEGORIES = [
  { label: '전체', typeId: null },
  { label: '자연', typeId: '12' },
  { label: '문화', typeId: '14' },
  { label: '레포츠', typeId: '28' },
] as const

export type CategoryLabel = (typeof CATEGORIES)[number]['label']

/** contenttypeid 를 화면에 쓰는 한 글자 분류로. 관광지가 아니면 null. */
export function categoryLabelOf(contentTypeId: string): string | null {
  switch (contentTypeId) {
    case '12':
      return '자연'
    case '14':
    case '15':
      return '문화'
    case '28':
      return '레포츠'
    default:
      return null
  }
}

/**
 * TourAPI 가 사진 URL 을 전부 `http://` 로 준다. 브라우저는 https 페이지에서 http 이미지를
 * 막으므로(mixed content) 여기서 올려준다. 안드로이드에서도 같은 이유로 같은 처리를 한다.
 */
function toHttps(url: string | null | undefined): string | null {
  if (!url) return null
  return url.startsWith('http://') ? 'https://' + url.slice(7) : url
}

function stripHtml(text: string | null | undefined): string | null {
  if (!text) return null
  return text.replace(/<[^>]*>/g, '').trim() || null
}

function toTourSpot(item: Record<string, string>): TourSpot {
  return {
    contentId: item.contentid ?? '',
    contentTypeId: item.contenttypeid ?? '',
    title: item.title ?? '',
    addr1: item.addr1 ?? '',
    lat: Number(item.mapy) || null,
    lng: Number(item.mapx) || null,
    image: toHttps(item.firstimage),
    thumbnail: toHttps(item.firstimage2),
    overview: stripHtml(item.overview),
  }
}

async function call(op: string, params: Record<string, string>): Promise<TourSpot[]> {
  return (await callPage(op, params)).spots
}

/** call 과 같지만 전체 건수도 돌려준다 — "더 보기"를 띄울지 정할 때 쓴다. */
async function callPage(
  op: string,
  params: Record<string, string>,
): Promise<{ spots: TourSpot[]; total: number }> {
  const query = new URLSearchParams(params).toString()
  const res = await fetch(`/api/tour/${op}?${query}`)
  if (!res.ok) throw new Error(`TourAPI ${op} 실패 (${res.status})`)
  const json = await res.json()

  const code = json?.response?.header?.resultCode
  if (code !== '0000') {
    throw new Error(json?.response?.header?.resultMsg ?? '알 수 없는 오류가 발생했어요.')
  }

  // 결과가 없으면 items 가 빈 문자열로 온다. item 이 하나면 배열이 아니라 객체다.
  const raw = json?.response?.body?.items
  const item = typeof raw === 'object' && raw !== null ? raw.item : null
  const list: Record<string, string>[] = Array.isArray(item) ? item : item ? [item] : []
  return { spots: list.map(toTourSpot), total: Number(json?.response?.body?.totalCount) || 0 }
}

/** 관광지가 아닌 항목을 걸러낸다 — 검색·목록·추천이 "무엇이 관광지인가"에 어긋나지 않도록. */
function onlyAttractions(spots: TourSpot[]): TourSpot[] {
  return spots.filter((s) => ATTRACTION_TYPE_IDS.has(s.contentTypeId))
}

const LIST_PAGE_SIZE = 40

/**
 * 제주 지역 목록 조회. 검색어가 비었을 때 기본으로 보여줄 목록. 한 페이지씩 받는다.
 *
 * - KorService2 는 옛 지역코드(areaCode)가 비어 있는 장소가 많다(성산일출봉도 그렇다) — 법정동
 *   코드(lDongRegnCd, 제주 50)로 불러야 제주 전체에서 뽑힌다.
 * - 정렬을 안 주면 제목 가나다순이라 "가"로 시작하는 곳만 나온다. 조회순(B)으로 받아 많이 찾는
 *   곳이 먼저 오게 한다.
 * - "전체"는 유형을 안 좁혀서 음식점·숙박도 섞여 오고, 그건 받은 뒤에 거른다. 조회순이면 대부분
 *   관광지라 한 페이지에서 많이 줄지 않는다.
 */
export async function areaBasedList(
  contentTypeId: string | null,
  pageNo = 1,
): Promise<{ spots: TourSpot[]; hasMore: boolean }> {
  const params: Record<string, string> = {
    numOfRows: String(LIST_PAGE_SIZE),
    pageNo: String(pageNo),
    lDongRegnCd: '50',
    arrange: 'B',
  }
  if (contentTypeId) params.contentTypeId = contentTypeId
  const { spots, total } = await callPage('areaBasedList2', params)
  return { spots: onlyAttractions(spots), hasMore: pageNo * LIST_PAGE_SIZE < total }
}

/** 키워드 검색 — 전국을 뒤진 뒤 주소에 "제주"가 들어간 것만 남긴다. */
export async function searchKeyword(keyword: string): Promise<TourSpot[]> {
  const spots = await call('searchKeyword2', { numOfRows: '30', pageNo: '1', keyword })
  return onlyAttractions(spots).filter((s) => s.addr1.includes('제주'))
}

/**
 * 상세 조회. 목록 API 는 overview 를 주지 않으므로, 검색으로 담은 장소의 설명은
 * 상세 시트를 열 때 이걸로 따로 가져온다.
 */
export async function detail(contentId: string): Promise<TourSpot | null> {
  const spots = await call('detailCommon2', { contentId })
  return spots[0] ?? null
}
