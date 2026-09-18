import logoUrl from '../assets/logo.svg'

/**
 * 로고(붓 모양 J) + "컬러링 제주"(제주돌담체) 한 덩어리 — 앱 헤더와 로그인·회원가입 화면이 똑같이
 * 보이도록 여기 하나만 둔다. 모양은 App.css 의 .app-brand 가 맡는다.
 */
export function Brand({ className }: { className?: string }) {
  return (
    <div className={'app-brand' + (className ? ` ${className}` : '')}>
      <img className="app-brand__logo" src={logoUrl} alt="" />
      <h1 className="app-brand__title">컬러링 제주</h1>
    </div>
  )
}
