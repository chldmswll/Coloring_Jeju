/**
 * 앱 전체가 공유하는 데이터 모양. 안드로이드 앱의 RecommendedSpot 와는 같은 구조라,
 * 두 클라이언트가 같은 assets JSON 을 읽을 수 있다. (여행/스팟 데이터는 웹 전용 — 아래 Trip 참고)
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

/** 개인(혼자) 또는 그룹(초대코드로 같이 담기). */
export type TripKind = 'personal' | 'group'

/**
 * 여행 하나. 지도 핀·스탬프·조각모음 앨범이 전부 이 단위로 묶인다.
 *
 * Firestore `trips/{tripId}` 문서. 안드로이드 앱과는 공유하지 않는 웹 전용 컬렉션이다
 * (안드로이드가 쓰는 `groups/{code}` 와는 별개).
 */
export interface Trip {
  id: string
  name: string
  kind: TripKind
  /** 'YYYY-MM-DD'. <input type="date"> 값을 그대로 저장한다. */
  startDate: string
  endDate: string
  ownerUid: string
  /** personal 은 항상 [ownerUid] 하나뿐. */
  memberUids: string[]
  members: Record<string, string>
  /** group 일 때만 6자리 코드. personal 은 null. */
  inviteCode: string | null
  createdAt: number
}

/**
 * 여행에 담긴 장소 하나. `trips/{tripId}/spots/{contentId}` 문서.
 *
 * verifiedColor 가 있으면 인증 완료 — 지도 마커가 컬러로, 스탬프가 완료로, 조각모음 타임라인에
 * 카드로 뜬다. photo 는 인증할 때 찍은 사진의 축소본(data URL), caption 은 그때 적은 한마디,
 * verifiedAt 은 인증한 시각(ms) — 셋 다 인증 전에는 null.
 */
export interface TripSpot {
  contentId: string
  title: string
  image: string | null
  category: string
  lat: number
  lng: number
  addedAt: number
  addedByUid: string
  headline: string
  description: string | null
  verifiedColor: string | null
  photo: string | null
  caption: string | null
  verifiedAt: number | null
}
