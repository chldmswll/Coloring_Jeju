/**
 * 사진에서 대표 색 3개를 뽑는다. 안드로이드의 AndroidX Palette 가 하던 일을 웹에서 직접 한 것.
 *
 * 방법은 색 양자화(quantization): RGB 를 거친 격자로 묶어 같은 칸에 떨어지는 픽셀을 한 색으로
 * 세고, 많이 나온 순으로 고른다. 사진 한 장에서 "이 장소의 색"을 고르는 용도라 이 정도면 충분하고,
 * 라이브러리 없이 동작한다.
 */

const SAMPLE_EDGE = 96 // 원본 대신 이 크기로 줄여서 읽는다. 색 분포는 거의 그대로고 훨씬 빠르다.
const BUCKETS_PER_CHANNEL = 6 // 6^3 = 216칸. 너무 잘게 나누면 비슷한 색이 흩어져 표가 안 모인다.

/** 사진(파일)에서 대표 색 [최대 3개]를 #RRGGBB 로 돌려준다. */
export async function extractTopColors(file: File | Blob): Promise<string[]> {
  const bitmap = await createImageBitmap(file)
  const scale = SAMPLE_EDGE / Math.max(bitmap.width, bitmap.height)
  const w = Math.max(1, Math.round(bitmap.width * scale))
  const h = Math.max(1, Math.round(bitmap.height * scale))

  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  const ctx = canvas.getContext('2d', { willReadFrequently: true })!
  ctx.drawImage(bitmap, 0, 0, w, h)
  bitmap.close()

  const { data } = ctx.getImageData(0, 0, w, h)
  const counts = new Map<number, { n: number; r: number; g: number; b: number }>()
  const step = 256 / BUCKETS_PER_CHANNEL

  for (let i = 0; i < data.length; i += 4) {
    const [r, g, b, a] = [data[i], data[i + 1], data[i + 2], data[i + 3]]
    if (a < 200) continue

    // 거의 검거나 거의 흰 픽셀은 뺀다 — 하늘·그늘이 화면을 덮어 "이 장소의 색"을 가리기 때문.
    const max = Math.max(r, g, b)
    const min = Math.min(r, g, b)
    if (max < 28 || min > 232) continue

    const key =
      Math.floor(r / step) * BUCKETS_PER_CHANNEL * BUCKETS_PER_CHANNEL +
      Math.floor(g / step) * BUCKETS_PER_CHANNEL +
      Math.floor(b / step)

    const slot = counts.get(key)
    if (slot) {
      slot.n++
      slot.r += r
      slot.g += g
      slot.b += b
    } else {
      counts.set(key, { n: 1, r, g, b })
    }
  }

  const top = [...counts.values()].sort((a, b) => b.n - a.n).slice(0, 3)
  if (top.length === 0) return ['#4e9e6e'] // 전부 걸러진 사진 — 고를 게 있어야 하니 브랜드색으로.

  // 칸의 중앙값이 아니라 그 칸에 든 픽셀들의 평균을 쓴다. 실제 사진 색에 더 가깝다.
  return top.map(({ n, r, g, b }) => toHex(Math.round(r / n), Math.round(g / n), Math.round(b / n)))
}

function toHex(r: number, g: number, b: number): string {
  return '#' + [r, g, b].map((v) => v.toString(16).padStart(2, '0')).join('')
}

/**
 * 조각모음에 남길 사진을 data URL 로 줄인다. 원본을 그대로 두면 localStorage 한도(보통 5MB)를
 * 사진 몇 장만에 넘겨 저장이 통째로 실패한다.
 */
export async function toThumbnailDataUrl(file: File | Blob, edge = 320): Promise<string> {
  const bitmap = await createImageBitmap(file)
  const scale = edge / Math.max(bitmap.width, bitmap.height)
  const w = Math.max(1, Math.round(bitmap.width * scale))
  const h = Math.max(1, Math.round(bitmap.height * scale))

  const canvas = document.createElement('canvas')
  canvas.width = w
  canvas.height = h
  canvas.getContext('2d')!.drawImage(bitmap, 0, 0, w, h)
  bitmap.close()
  return canvas.toDataURL('image/jpeg', 0.8)
}
