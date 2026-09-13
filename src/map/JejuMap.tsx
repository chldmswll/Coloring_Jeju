import { useEffect, useRef } from 'react'
import { clusterPins, metersPerPixel, type ClusterablePin } from './clustering'
import { useKakaoLoader } from './useKakaoLoader'
import './JejuMap.css'

/** 지도에 올라가는 핀 하나. verifiedColor 가 있으면 사진이 컬러로, 없으면 흑백으로 그려진다. */
export interface MapPin extends ClusterablePin {
  id: string
  title: string
  imageUrl: string | null
  emoji: string
  verifiedColor: string | null
}

interface Props {
  pins: MapPin[]
  onPinClick: (pin: MapPin) => void
}

const JEJU_CENTER = { lat: 33.38, lng: 126.55 }

/**
 * 지도를 끌 수 있는 범위 — 제주 본섬에 우도(126.95E)·마라도(33.11N)·비양도(126.23E)까지
 * 여유를 둔 사각형. 모든 핀을 화면 가운데로 가져올 수는 있으면서, 육지나 먼 바다로는 끌려가지
 * 않게 한다. 카카오맵에는 범위 제한 API가 없어서 지도가 움직일 때마다 직접 되돌린다.
 */
const BOUNDS = { north: 33.7, east: 127.1, south: 33.05, west: 126.0 }

/** 제주 전체가 들어오는 기본 배율. 카카오는 숫자가 클수록 더 넓게 보인다. */
const DEFAULT_LEVEL = 10
/** 이보다 넓게는 못 보게 막는다 — 제주만 다루는 지도라 육지까지 보일 이유가 없다. */
const MAX_LEVEL = 11
/** 핀에 맞춰 확대할 때 이보다 더 당기지는 않는다. 한 곳만 있을 때 과하게 확대되는 걸 막는다. */
const MIN_FIT_LEVEL = 7

const PIN_SIZE_PX = 36
/** 핀 크기의 이 배수보다 가까우면 한 마커로 합친다. */
const CLUSTER_GAP = 1.15

export function JejuMap({ pins, onPinClick }: Props) {
  const { loaded, error } = useKakaoLoader()
  const containerRef = useRef<HTMLDivElement>(null)
  const mapRef = useRef<any>(null)
  const overlaysRef = useRef<any[]>([])
  /** 마지막으로 카메라를 맞춘 핀 구성. 사진이 뒤늦게 로드돼도 다시 맞추지 않으려고 기억해 둔다. */
  const fitKeyRef = useRef<string | null>(null)
  // 최신 핀·핸들러를 ref 로 들고 있어야 지도 이벤트 콜백이 오래된 값을 붙잡지 않는다.
  const pinsRef = useRef(pins)
  const clickRef = useRef(onPinClick)
  pinsRef.current = pins
  clickRef.current = onPinClick

  // 지도 생성 — 한 번만.
  useEffect(() => {
    if (!loaded || !containerRef.current || mapRef.current) return
    const { kakao } = window

    const map = new kakao.maps.Map(containerRef.current, {
      center: new kakao.maps.LatLng(JEJU_CENTER.lat, JEJU_CENTER.lng),
      level: DEFAULT_LEVEL,
    })
    map.setMaxLevel(MAX_LEVEL)
    mapRef.current = map

    const draw = () => drawMarkers(map, pinsRef.current, overlaysRef, clickRef)
    draw()
    // 겹침 여부는 배율에만 좌우된다 — 드래그가 아니라 zoom 에서만 다시 계산하면 된다.
    kakao.maps.event.addListener(map, 'zoom_changed', draw)

    // 범위 밖으로 나가면 되돌린다. 되돌린 좌표는 이미 범위 안이라 다시 불려도 더는 안 바뀐다.
    let clamping = false
    const clamp = () => {
      if (clamping) return
      const c = map.getCenter()
      const lat = Math.min(Math.max(c.getLat(), BOUNDS.south), BOUNDS.north)
      const lng = Math.min(Math.max(c.getLng(), BOUNDS.west), BOUNDS.east)
      if (lat === c.getLat() && lng === c.getLng()) return
      clamping = true
      map.setCenter(new kakao.maps.LatLng(lat, lng))
      clamping = false
    }
    kakao.maps.event.addListener(map, 'center_changed', clamp)

    return () => {
      kakao.maps.event.removeListener(map, 'zoom_changed', draw)
      kakao.maps.event.removeListener(map, 'center_changed', clamp)
    }
  }, [loaded])

  // 핀이 바뀌면 다시 그리고, *구성 자체*가 바뀐 경우에만 카메라를 맞춘다.
  useEffect(() => {
    const map = mapRef.current
    if (!map) return
    drawMarkers(map, pins, overlaysRef, clickRef)

    const fitKey = pins.map((p) => p.id).join('|')
    if (fitKeyRef.current === fitKey) return
    fitKeyRef.current = fitKey
    fitCamera(map, pins)
  }, [pins, loaded])

  if (error) return <div className="map-frame map-error t-body">{error}</div>
  return <div ref={containerRef} className="map-frame" />
}

/** 보이는 핀이 모두 들어오게 맞춘다. 핀이 없으면 제주 전체로 돌아간다. */
function fitCamera(map: any, pins: MapPin[]) {
  const { kakao } = window
  if (pins.length === 0) {
    map.setCenter(new kakao.maps.LatLng(JEJU_CENTER.lat, JEJU_CENTER.lng))
    map.setLevel(DEFAULT_LEVEL)
    return
  }
  if (pins.length === 1) {
    map.setCenter(new kakao.maps.LatLng(pins[0].lat, pins[0].lng))
    map.setLevel(MIN_FIT_LEVEL)
    return
  }
  const bounds = new kakao.maps.LatLngBounds()
  pins.forEach((p) => bounds.extend(new kakao.maps.LatLng(p.lat, p.lng)))
  map.setBounds(bounds, 28, 28, 28, 28)
  // 핀이 한곳에 몰려 있으면 setBounds 가 과하게 당긴다 — 상한을 둔다.
  if (map.getLevel() < MIN_FIT_LEVEL) map.setLevel(MIN_FIT_LEVEL)
}

function drawMarkers(
  map: any,
  pins: MapPin[],
  overlaysRef: React.RefObject<any[]>,
  clickRef: React.RefObject<(pin: MapPin) => void>,
) {
  const { kakao } = window
  overlaysRef.current.forEach((o) => o.setMap(null))
  overlaysRef.current = []

  const minMeters = PIN_SIZE_PX * CLUSTER_GAP * metersPerPixel(map.getLevel())

  clusterPins(pins, minMeters).forEach(({ head, extra }) => {
    const el = document.createElement('button')
    el.type = 'button'
    el.className = 'map-pin' + (head.verifiedColor ? ' is-verified' : '')
    el.setAttribute('aria-label', head.title)
    if (head.verifiedColor) el.style.borderColor = head.verifiedColor

    if (head.imageUrl) {
      const img = document.createElement('img')
      img.src = head.imageUrl
      img.alt = ''
      el.appendChild(img)
    } else {
      // TourAPI 에 사진이 없는 곳 — 빈 원 대신 카테고리 이모지를 둔다.
      el.appendChild(document.createTextNode(head.emoji))
    }

    if (extra > 0) {
      const badge = document.createElement('span')
      badge.className = 'map-pin__badge'
      badge.textContent = `+${extra}`
      el.appendChild(badge)
    }

    el.onclick = () => {
      if (extra > 0) {
        // 겹친 마커는 어느 장소를 누른 건지 알 수 없다 — 추측하지 말고 확대해서 풀어준다.
        map.setLevel(Math.max(1, map.getLevel() - 2), {
          anchor: new kakao.maps.LatLng(head.lat, head.lng),
          animate: true,
        })
      } else {
        clickRef.current(head)
      }
    }

    const overlay = new kakao.maps.CustomOverlay({
      position: new kakao.maps.LatLng(head.lat, head.lng),
      content: el,
      clickable: true,
      yAnchor: 0.5,
      xAnchor: 0.5,
    })
    overlay.setMap(map)
    overlaysRef.current.push(overlay)
  })
}
