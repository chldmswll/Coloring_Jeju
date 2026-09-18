import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 폰트는 index.css 가 불러오는 styles/fonts.css 의 @font-face 로 등록된다(src/font 의
// woff 파일을 직접 쓴다) — 이게 없으면 font-family: 'Pretendard' 는 이름만 있고
// 실제로는 시스템 기본 폰트(Windows 는 맑은 고딕)로 조용히 대체되어 그려진다.
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
