/** 지도에 실제로 찍히는 핀 하나가 필요로 하는 최소 정보. */
export interface ClusterablePin {
  lat: number
  lng: number
  /** 중심관광지 순위. 낮을수록 대표로 남는다. 순위가 없는 곳은 Infinity. */
  rank: number
}

/** 실제로 그려진 마커 하나: head 가 보이는 핀, extra 는 그 아래 숨은 개수. */
export interface PinCluster<T extends ClusterablePin> {
  head: T
  extra: number
}

const EARTH_RADIUS_M = 6_371_000

export function metersBetween(a: ClusterablePin, b: ClusterablePin): number {
  const latMid = ((a.lat + b.lat) / 2) * (Math.PI / 180)
  const dx = (a.lng - b.lng) * (Math.PI / 180) * Math.cos(latMid) * EARTH_RADIUS_M
  const dy = (a.lat - b.lat) * (Math.PI / 180) * EARTH_RADIUS_M
  return Math.hypot(dx, dy)
}

/**
 * 카카오맵 레벨에서 화면 1px 이 덮는 거리(m).
 *
 * 카카오 레벨은 숫자가 작을수록 확대라 웹 메르카토르 줌과 반대다. 레벨 3이 약 1m/px 기준이고
 * 한 단계마다 2배씩 넓어진다.
 */
export function metersPerPixel(level: number): number {
  return Math.pow(2, level - 3)
}

/**
 * 화면에서 겹칠 핀들을 하나로 합친다. 순위가 좋은 것부터 훑으면서, 아직 안 뽑힌 핀 중
 * minMeters 보다 가까운 것들을 흡수한다. 살아남은 핀이 사진을 유지하고 `+N` 배지를 단다.
 *
 * 두 핀 사이의 화면상 픽셀 거리는 줌 레벨에만 의존하고 지도를 어디로 옮겼는지와는 무관하다.
 * 그래서 이 계산은 레벨이 바뀔 때만 다시 하면 되고, 드래그할 때마다 할 필요가 없다.
 */
export function clusterPins<T extends ClusterablePin>(
  pins: readonly T[],
  minMeters: number,
): PinCluster<T>[] {
  if (pins.length < 2) return pins.map((head) => ({ head, extra: 0 }))

  const remaining = [...pins].sort((a, b) => a.rank - b.rank)
  const clusters: PinCluster<T>[] = []

  while (remaining.length > 0) {
    const head = remaining.shift()!
    let extra = 0
    for (let i = remaining.length - 1; i >= 0; i--) {
      if (metersBetween(head, remaining[i]) < minMeters) {
        remaining.splice(i, 1)
        extra++
      }
    }
    clusters.push({ head, extra })
  }
  return clusters
}
