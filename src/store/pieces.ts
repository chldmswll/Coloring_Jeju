import { useCallback, useSyncExternalStore } from 'react'
import type { CollectedPiece } from '../types'

/**
 * 조각모음 — 인증할 때마다 쌓이는 "장소 + 사진 + 그날 뽑은 색" 카드.
 *
 * 안드로이드에서는 화면 상태로만 들고 있어서 앱을 끄면 사라졌는데, 인증 상태는 남고 조각만
 * 사라지는 게 앞뒤가 안 맞아 여기서는 처음부터 저장한다. 사진은 축소한 data URL 로 넣는다
 * (extractColors.toThumbnailDataUrl) — 원본을 넣으면 localStorage 한도를 금방 넘긴다.
 */

const STORAGE_KEY = 'coloring-jeju:pieces'

let cache: CollectedPiece[] = read()
const listeners = new Set<() => void>()

function read(): CollectedPiece[] {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    return raw ? (JSON.parse(raw) as CollectedPiece[]) : []
  } catch {
    return []
  }
}

export const piecesStore = {
  getAll: () => cache,

  add(piece: CollectedPiece) {
    const next = [...cache.filter((p) => p.contentId !== piece.contentId), piece]
    cache = next
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(next))
    } catch {
      // 용량이 찼다면 사진 없이라도 기록은 남긴다 — 색과 장소가 조각의 본체다.
      cache = next.map((p) => ({ ...p, photo: '' }))
      localStorage.setItem(STORAGE_KEY, JSON.stringify(cache))
    }
    listeners.forEach((l) => l())
  },

  subscribe(listener: () => void) {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
}

export function usePieces(): CollectedPiece[] {
  return useSyncExternalStore(
    useCallback((l: () => void) => piecesStore.subscribe(l), []),
    () => cache,
  )
}
