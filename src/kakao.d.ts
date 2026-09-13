/**
 * 카카오맵 SDK 는 npm 패키지가 아니라 <script> 로 붙기 때문에 전역 타입을 직접 선언한다.
 * 공식 타입 정의가 없어 `any` 로 두되, 이 파일 하나로 범위를 가둔다.
 */
declare global {
  interface Window {
    kakao: any
  }
}

interface ImportMetaEnv {
  readonly VITE_KAKAO_MAP_KEY: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}

export {}
