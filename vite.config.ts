import react from '@vitejs/plugin-react'
import { defineConfig, loadEnv } from 'vite'

// https://vite.dev/config/
export default defineConfig(({ mode }) => {
  // 키는 .env.local 에만 두고 저장소에는 올리지 않는다. VITE_ 접두사를 일부러 쓰지 않는 이유는
  // 그 접두사가 붙은 값만 프론트 번들에 주입되기 때문 — 이 키는 아래 프록시(Node 쪽)에서만 읽힌다.
  const env = loadEnv(mode, process.cwd(), '')
  const tourApiKey = env.TOUR_API_SERVICE_KEY ?? ''

  return {
    plugins: [react()],
    server: {
      proxy: {
        /**
         * TourAPI는 브라우저에서 직접 못 부른다 — Origin 헤더가 붙으면 403을 돌려주고
         * Access-Control-Allow-Origin 도 주지 않는다. 그래서 개발 중에는 Vite 개발 서버가
         * 대신 호출해준다. 배포 때는 같은 역할을 하는 서버리스 함수가 필요하다.
         *
         * 프론트에서는 `/api/tour/searchKeyword2?keyword=우도` 처럼 부르면 된다. serviceKey와
         * 공통 파라미터는 여기서 붙으므로 프론트 코드에 키가 등장하지 않는다.
         */
        '/api/tour': {
          target: 'https://apis.data.go.kr',
          changeOrigin: true,
          rewrite: (path) => {
            const [route, query = ''] = path.replace(/^\/api\/tour/, '').split('?')
            const params = new URLSearchParams(query)
            params.set('serviceKey', tourApiKey)
            params.set('MobileOS', 'ETC')
            params.set('MobileApp', 'ColoringJeju')
            params.set('_type', 'json')
            return `/B551011/KorService2${route}?${params.toString()}`
          },
        },
      },
    },
  }
})
