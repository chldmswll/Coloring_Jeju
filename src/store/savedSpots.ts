import { useCallback, useSyncExternalStore } from 'react'
import type { SavedSpot } from '../types'

/**
 * MY 지도의 단일 출처 — 안드로이드 앱의 SavedSpotsStore 를 그대로 옮긴 것.
 *
 * 홈·지도와 스탬프는 "같은 장소 목록"이어야 한다는 게 이 앱의 규칙이라, 두 화면이 각자 목록을
 * 들고 동기화를 약속하는 대신 이 저장소 하나를 구독한다. 어디서 담든/인증하든 한 번의 쓰기로
 * 모든 화면이 같이 갱신된다.
 *
 * 서버가 없으니 localStorage 가 유일한 기록이다. 나중에 Firestore 를 붙이면 이 파일만 바꾸면 된다.
 */

const STORAGE_KEY = 'coloring-jeju:saved-spots'

let cache: SavedSpot[] = read()
const listeners = new Set<() => void>()

function read(): SavedSpot[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as SavedSpot[]) : []
  } catch {
    // 손상된 값 하나 때문에 지도가 통째로 안 뜨는 것보다, 빈 목록으로 시작하는 편이 낫다.
    return []
  }
}

function write(next: SavedSpot[]) {
  cache = next
  localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
  listeners.forEach((l) => l())
}

export const savedSpotsStore = {
  getAll: () => cache,

  isSaved: (contentId: string) => cache.some((s) => s.contentId === contentId),

  add(spot: SavedSpot) {
    // 이미 얻은 색은 지키다 — 같은 곳을 다시 담았다고 인증이 풀려 마커가 흑백으로 돌아가면 안 된다.
    const earned = cache.find((s) => s.contentId === spot.contentId)?.verifiedColor ?? null
    write([
      ...cache.filter((s) => s.contentId !== spot.contentId),
      { ...spot, verifiedColor: spot.verifiedColor ?? earned },
    ])
  },

  remove(contentId: string) {
    write(cache.filter((s) => s.contentId !== contentId))
  },

  /**
   * 그 장소에서 뽑은 색을 기록한다. 이 한 번의 쓰기가 스탬프 행을 "인증완료"로 바꾸고,
   * 지도 마커를 흑백에서 컬러로 돌리고, 무지개 한 칸을 채운다.
   */
  markVerified(contentId: string, color: string) {
    if (!cache.some((s) => s.contentId === contentId)) return
    write(cache.map((s) => (s.contentId === contentId ? { ...s, verifiedColor: color } : s)))
  },

  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}

/** MY 지도를 구독하는 훅. 목록이 바뀌면 쓰는 화면이 전부 다시 그려진다. */
export function useSavedSpots(): SavedSpot[] {
  return useSyncExternalStore(
    useCallback((l: () => void) => savedSpotsStore.subscribe(l), []),
    () => cache,
  )
}
