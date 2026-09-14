import { useRef, useState } from 'react'
import { extractTopColors, toThumbnailDataUrl } from '../color/extractColors'
import { markTripSpotVerified } from '../firebase/trips'
import type { TripSpot } from '../types'

/**
 * 색 인증 — 사진을 찍고, 그 사진에서 뽑은 색 중 하나를 골라 장소에 기록한다.
 *
 * 이 한 번의 쓰기(markTripSpotVerified)가 스탬프 행을 인증완료로 바꾸고, 지도 마커를
 * 흑백에서 컬러로 돌리고, 무지개 한 칸을 채우고, 조각모음 앨범에 카드로 남긴다. 화면들이
 * 전부 같은 값 하나(여행의 spots)에서 파생되므로 서로 어긋날 수가 없다.
 *
 * `capture="environment"` 덕에 폰에서는 바로 후면 카메라가 열리고, PC 에서는 파일 선택이 뜬다.
 * 단 카메라는 보안 컨텍스트에서만 열리므로 배포는 반드시 HTTPS 여야 한다.
 */
export function VerifySheet({
  tripId,
  spot,
  onClose,
}: {
  tripId: string
  spot: TripSpot
  onClose: () => void
}) {
  const [photo, setPhoto] = useState<{ url: string; file: File } | null>(null)
  const [colors, setColors] = useState<string[]>([])
  const [picked, setPicked] = useState<string | null>(null)
  const [caption, setCaption] = useState('')
  const [busy, setBusy] = useState(false)
  const inputRef = useRef<HTMLInputElement>(null)

  async function onPick(file: File) {
    setBusy(true)
    setPhoto({ url: URL.createObjectURL(file), file })
    const found = await extractTopColors(file)
    setColors(found)
    setPicked(found[0] ?? null)
    setBusy(false)
  }

  async function complete() {
    if (!picked || !photo) return
    setBusy(true)
    await markTripSpotVerified(
      tripId,
      spot.contentId,
      picked,
      await toThumbnailDataUrl(photo.file),
      caption.trim() || null,
    )
    setBusy(false)
    onClose()
  }

  return (
    <div className="sheet-scrim" onClick={onClose}>
      <div className="sheet" onClick={(e) => e.stopPropagation()}>
        <div className="sheet__handle" />
        <h2 className="t-title">{spot.title}</h2>
        <p className="t-caption sheet__addr">
          사진을 찍으면 그 장소의 색을 뽑아 무지개에 담아요.
        </p>

        <div className="verify__frame">
          {photo ? (
            <img src={photo.url} alt="" />
          ) : (
            <span className="t-body verify__placeholder">아직 사진이 없어요</span>
          )}
        </div>

        <input
          ref={inputRef}
          type="file"
          accept="image/*"
          capture="environment"
          hidden
          onChange={(e) => {
            const file = e.target.files?.[0]
            if (file) void onPick(file)
          }}
        />

        {colors.length > 0 && (
          <>
            <p className="t-caption sheet__addr">추출된 대표 색 · 색을 선택해주세요</p>
            <div className="swatches">
              {colors.map((c) => (
                <button
                  key={c}
                  className={'swatch' + (picked === c ? ' is-picked' : '')}
                  style={{ background: c }}
                  aria-label={c}
                  onClick={() => setPicked(c)}
                />
              ))}
            </div>

            <p className="t-caption sheet__addr">이 순간을 기록할 한마디 (선택)</p>
            <textarea
              className="search__input t-body verify__caption"
              rows={2}
              placeholder="예: 바람이 좋아서 한참 앉아 있었다"
              value={caption}
              onChange={(e) => setCaption(e.target.value)}
            />
          </>
        )}

        <div className="verify__actions">
          <button className="pill t-subtitle" onClick={() => inputRef.current?.click()}>
            {photo ? '다시 찍기' : '카메라 열기'}
          </button>
          <button
            className="btn-primary t-button"
            disabled={!picked || busy}
            onClick={() => void complete()}
          >
            미션 완료
          </button>
        </div>
      </div>
    </div>
  )
}
