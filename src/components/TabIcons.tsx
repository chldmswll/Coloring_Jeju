/** 하단 탭바 아이콘 — 색은 항상 currentColor 를 써서, 부모(.tabbar__item)가 활성/비활성일 때
 * 텍스트와 똑같이 색이 바뀐다(별도 활성 스타일을 아이콘마다 만들 필요가 없다). */
type IconProps = { className?: string }

const shared = {
  width: 24,
  height: 24,
  viewBox: '0 0 24 24',
  fill: 'none' as const,
  stroke: 'currentColor',
  strokeWidth: 1.8,
  strokeLinecap: 'round' as const,
  strokeLinejoin: 'round' as const,
}

export function HomeIcon({ className }: IconProps) {
  return (
    <svg {...shared} className={className}>
      <path d="M4 11.5 12 4l8 7.5" />
      <path d="M6 10v9.5h12V10" />
      <path d="M10 19.5v-6h4v6" />
    </svg>
  )
}

/** 여행 탭 — 비행기. 루트의 plane.svg 를 그대로 옮긴 것이라 선 굵기도 원본대로 2 를 쓴다. */
export function PlaneIcon({ className }: IconProps) {
  return (
    <svg {...shared} strokeWidth={2} className={className}>
      <path d="M17.8 19.2L16 11L19.5 7.5C21 6 21.5 4 21 3C20 2.5 18 3 16.5 4.5L13 8L4.8 6.2C4.3 6.1 3.9 6.3 3.7 6.7L3.4 7.2C3.2 7.7 3.3 8.2 3.7 8.5L9 12L7 15H4L3 16L6 18L8 21L9 20V17L12 15L15.5 20.3C15.8 20.7 16.3 20.8 16.8 20.6L17.3 20.4C17.7 20.1 17.9 19.7 17.8 19.2Z" />
    </svg>
  )
}

export function StampIcon({ className }: IconProps) {
  return (
    <svg {...shared} className={className}>
      <rect x="5" y="4.5" width="14" height="14" rx="2" />
      <circle cx="5" cy="8" r="1.1" fill="currentColor" stroke="none" />
      <circle cx="5" cy="15" r="1.1" fill="currentColor" stroke="none" />
      <circle cx="19" cy="8" r="1.1" fill="currentColor" stroke="none" />
      <circle cx="19" cy="15" r="1.1" fill="currentColor" stroke="none" />
      <path d="m9 12 2 2 4-4.5" />
    </svg>
  )
}

export function GalleryIcon({ className }: IconProps) {
  return (
    <svg {...shared} className={className}>
      <path d="M8 8V6.6A1.6 1.6 0 0 1 9.6 5h8.8A1.6 1.6 0 0 1 20 6.6v9a1.6 1.6 0 0 1-1.6 1.6H17" />
      <rect x="4" y="8.5" width="13" height="11" rx="1.6" />
      <circle cx="7.4" cy="11.7" r="1" fill="currentColor" stroke="none" />
      <path d="m6 17 2.6-3 2 2 2.4-3 2.5 3.2" />
    </svg>
  )
}

export function AlbumIcon({ className }: IconProps) {
  return (
    <svg {...shared} className={className}>
      <rect x="3.5" y="6" width="14" height="14" rx="2" />
      <path d="M7 20.5h11a1.5 1.5 0 0 0 1.5-1.5v-11" />
      <circle cx="8" cy="10.5" r="1.4" />
      <path d="m4.5 17 3.5-3.5a1.5 1.5 0 0 1 2.1 0l1.4 1.4a1.5 1.5 0 0 0 2.1 0l1-1a1.5 1.5 0 0 1 2.1 0l1.3 1.3" />
    </svg>
  )
}

export function MyIcon({ className }: IconProps) {
  return (
    <svg {...shared} className={className}>
      <circle cx="12" cy="8.2" r="3.2" />
      <path d="M5 19.5c1-3.3 3.7-5 7-5s6 1.7 7 5" />
    </svg>
  )
}
