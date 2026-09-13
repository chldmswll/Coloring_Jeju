/**
 * 앱 전체가 공유하는 데이터 모양. 안드로이드 앱의 RecommendedSpot / SavedSpot 와 같은 구조라,
 * 두 클라이언트가 같은 assets JSON 과 같은 저장 형식을 읽을 수 있다.
 */

/** 추천 지도에 고정으로 올라가는 장소 — src/data/recommendedSpots.json 한 줄. */
export interface RecommendedSpot {
  contentId: string
  title: string
  category: string
  emoji: string
  lat: number
  lng: number
  imageUrl: string | null
  headline: string
  description: string
  /** 한국관광공사 중심관광지 순위(hubRank). 낮을수록 다른 관광지와 많이 연결된 곳. */
  rank: number
  signgu: string
}

/**
 * MY 지도에 담은 장소. 브라우저 localStorage 에 저장된다.
 *
 * verifiedColor 는 그 장소에서 뽑아낸 색(#RRGGBB)이고, null 이면 아직 인증 전 —
 * 지도 마커 사진이 흑백으로 남아 있는 상태가 정확히 이 경우다.
 */
export interface SavedSpot {
  contentId: string
  title: string
  image: string | null
  category: string
  lat: number
  lng: number
  addedAt: number
  headline: string
  description: string | null
  verifiedColor: string | null
}

/** TourAPI(KorService2) 검색 결과 한 건. */
export interface TourSpot {
  contentId: string
  contentTypeId: string
  title: string
  addr1: string
  lat: number | null
  lng: number | null
  image: string | null
  thumbnail: string | null
  overview: string | null
}

/** 조각모음 카드 한 장 — 어디서 어떤 색을 어떤 사진으로 얻었는지. */
export interface CollectedPiece {
  contentId: string
  placeName: string
  /** data: URL. 사진 원본을 그대로 두면 localStorage 용량을 넘기므로 축소해서 저장한다. */
  photo: string
  color: string
  collectedAt: number
}
