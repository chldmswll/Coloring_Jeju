import { StrictMode } from 'react'
import { createRoot } from 'react-dom/client'
// 폰트 파일 자체를 불러오는 게 이 4줄이다 — index.css 의 font-family: 'Pretendard' 는
// 이 CSS가 실제로 @font-face 를 등록해줘야 의미가 있다. 이게 없으면 이름만 지정해놓고
// 실제로는 시스템 기본 폰트(Windows 는 맑은 고딕)로 조용히 대체되어 그려진다.
// tokens.css 의 타이포 유틸(.t-body/.t-subtitle/.t-title/.t-display)이 쓰는 굵기
// 400/600/700/800 만 불러온다 — 9굵기 전부 불러오는 pretendard-dynamic-subset.css 는
// 안 쓰는 굵기까지 받게 되어 5배 가까이 무겁다. 굵기는 전부 동적 서브셋이라 실제 쓰이는
// 글자(유니코드 범위)의 조각만 브라우저가 알아서 내려받는다.
import 'pretendard/dist/web/static/Pretendard-Regular.css'
import 'pretendard/dist/web/static/Pretendard-SemiBold.css'
import 'pretendard/dist/web/static/Pretendard-Bold.css'
import 'pretendard/dist/web/static/Pretendard-ExtraBold.css'
import './index.css'
import App from './App.tsx'

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <App />
  </StrictMode>,
)
