import { useEffect, useState } from 'react'

/**
 * 카카오맵 SDK 를 한 번만 불러온다.
 *
 * `autoload=false` 로 받아서 `kakao.maps.load()` 로 직접 초기화하는 게 중요하다 — 스크립트가
 * 붙자마자 자동 초기화되면 React 가 컨테이너를 붙이기 전에 지도를 만들려다 실패한다.
 *
 * 키(VITE_KAKAO_MAP_KEY)는 프론트에 노출되는 값이 맞다. 카카오는 키 자체가 아니라 콘솔에
 * 등록된 도메인으로 보호하므로, 개발용 localhost 와 배포 도메인을 등록해 두어야 지도가 뜬다.
 * 등록이 안 되어 있으면 에러 없이 회색 화면만 나오니 먼저 의심할 것.
 */
export function useKakaoLoader(): { loaded: boolean; error: string | null } {
  const [loaded, setLoaded] = useState(false)
  const [error, setError] = useState<string | null>(null)

  useEffect(() => {
    const key = import.meta.env.VITE_KAKAO_MAP_KEY
    if (!key) {
      setError('VITE_KAKAO_MAP_KEY 가 비어 있어요. .env.local 을 확인해주세요.')
      return
    }

    if (window.kakao?.maps) {
      setLoaded(true)
      return
    }

    const existing = document.getElementById('kakao-map-sdk') as HTMLScriptElement | null
    const script = existing ?? document.createElement('script')
    if (!existing) {
      script.id = 'kakao-map-sdk'
      script.async = true
      script.src = `https://dapi.kakao.com/v2/maps/sdk.js?appkey=${key}&autoload=false`
      document.head.appendChild(script)
    }

    const onLoad = () => window.kakao.maps.load(() => setLoaded(true))
    const onError = () =>
      setError('카카오맵을 불러오지 못했어요. 키와 등록된 도메인을 확인해주세요.')

    script.addEventListener('load', onLoad)
    script.addEventListener('error', onError)
    return () => {
      script.removeEventListener('load', onLoad)
      script.removeEventListener('error', onError)
    }
  }, [])

  return { loaded, error }
}
