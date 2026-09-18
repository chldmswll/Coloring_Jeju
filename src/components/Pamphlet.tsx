import { useEffect, useRef, useState } from 'react'
import { DownloadIcon } from './Icons'
import type { TripSpot } from '../types'

/**
 * 콜라주 — 고른 사진들을 가로로 긴 한 장으로 모아준다. 글씨는 전부 제주돌담체.
 *
 * 테마는 세 가지다.
 * - 노트: 나무 책상 위에 펼쳐둔 다이어리에 스크랩한 모습 (카드 전체가 추출색)
 * - 파스텔: 점무늬 종이에 별·음표 낙서를 곁들인 밝은 모습 (카드 전체가 추출색)
 * - 화이트: 하얀 바탕에 하얀 폴라로이드만, 테이프도 없이
 */

export type CollageTheme = '노트' | '파스텔' | '화이트'
export const COLLAGE_THEMES: CollageTheme[] = ['노트', '파스텔', '화이트']

/** 테마 고르는 화면에서 이름 아래 적는 한 줄 설명. */
export const COLLAGE_THEME_DESC: Record<CollageTheme, string> = {
  노트: '나무 책상 위 다이어리',
  파스텔: '점무늬 종이와 낙서',
  화이트: '하얀 바탕에 깔끔하게',
}

/** 콜라주 한 장에 들어가는 내용 — 어느 여행인지는 부르는 쪽이 정해서 넘긴다. */
export interface CollageInput {
  title: string
  period: string
  pieces: TripSpot[]
  theme: CollageTheme
}

/** 가로로 긴 한 장. */
const CANVAS_W = 1280
const CANVAS_H = 820

/** 노트 테마 — 책상 나무가 보이는 바깥 여백과 펼친 책의 지면. */
const DESK_MARGIN = 40
const LEFT_PAGE_RATIO = 0.13
const SPINE_W = 14
const PAGE_PAD = 26
const TOP_PEEK = 34
const TITLE_GAP = 18
const TITLE_BLOCK_H = 150
const PAGE_BOTTOM_PAD = 26

const BOOK_W = CANVAS_W - DESK_MARGIN * 2
const BOOK_H = CANVAS_H - DESK_MARGIN * 2
const GRID_H = BOOK_H - TOP_PEEK - TITLE_GAP - TITLE_BLOCK_H - PAGE_BOTTOM_PAD
const LEFT_PAGE_W = BOOK_W * LEFT_PAGE_RATIO
const RIGHT_PAGE_X = DESK_MARGIN + LEFT_PAGE_W + SPINE_W
const RIGHT_PAGE_W = BOOK_W - LEFT_PAGE_W - SPINE_W

/** 파스텔·화이트 테마 — 종이 가장자리 여백. */
const SHEET_PAD = 56

/** 타일이 이보다 많으면 마지막 칸을 "+N"으로 접어 넣는다 — 사진이 너무 잘게 쪼개지지 않게. */
const MAX_TILES = 9
/** 미리보기 박스를 이 비율로 맞춰야 안 찌그러진다. */
export const PAMPHLET_ASPECT = CANVAS_W / CANVAS_H

/**
 * 콜라주 글씨 전용 폰트 — 헤더 제목과 같은 제주돌담체(styles/fonts.css 에서 등록). 굵기가 하나뿐이라
 * 전부 400 으로 쓴다 — 700 을 주면 캔버스가 가짜 굵게를 덧칠해 획이 뭉개진다.
 */
const TITLE_FONT = 'JejuDoldam'

/** 콜라주에 붙일 수 있는 그림 — 실제 사진이거나, 테마 미리보기용으로 직접 그린 예시 캔버스. */
type Drawable = HTMLImageElement | HTMLCanvasElement

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

function roundRectPath(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  r: number,
) {
  ctx.beginPath()
  ctx.moveTo(x + r, y)
  ctx.arcTo(x + w, y, x + w, y + h, r)
  ctx.arcTo(x + w, y + h, x, y + h, r)
  ctx.arcTo(x, y + h, x, y, r)
  ctx.arcTo(x, y, x + w, y, r)
  ctx.closePath()
}

/** object-fit: cover 와 같은 방식으로, 잘린 부분 없이 칸을 꽉 채운다. */
function drawCover(
  ctx: CanvasRenderingContext2D,
  img: Drawable,
  x: number,
  y: number,
  w: number,
  h: number,
) {
  const scale = Math.max(w / img.width, h / img.height)
  const dw = img.width * scale
  const dh = img.height * scale
  ctx.drawImage(img, x + (w - dw) / 2, y + (h - dh) / 2, dw, dh)
}

/** 색이 밝으면 어두운 잉크색, 어두우면 흰 글씨 — 어떤 추출색 위에도 글자가 읽힌다. */
function readableTextColor(hex: string): string {
  const m = /^#([0-9a-f]{6})$/i.exec(hex)
  if (!m) return 'rgba(43, 46, 36, 0.88)'
  const n = parseInt(m[1], 16)
  const r = (n >> 16) & 255
  const g = (n >> 8) & 255
  const b = n & 255
  const luminance = (0.299 * r + 0.587 * g + 0.114 * b) / 255
  return luminance > 0.62 ? 'rgba(43, 46, 36, 0.88)' : 'rgba(255, 255, 255, 0.95)'
}

/** 칸보다 길면 뒤를 잘라 "…"을 붙인다. 지금 ctx 에 걸린 font 기준으로 잰다. */
function fitText(ctx: CanvasRenderingContext2D, text: string, maxW: number): string {
  let out = text
  while (ctx.measureText(out).width > maxW && out.length > 1) out = out.slice(0, -1)
  return out === text ? text : out.slice(0, -1) + '…'
}

/* ───────────────────── 질감 ───────────────────── */

/** 고정 시드 난수 — 그릴 때마다 나뭇결·종이결이 달라지면 화면이 들썩여서 시드를 박아둔다. */
function seededRandom(seed: number): () => number {
  let s = seed >>> 0
  return () => {
    s = (s + 0x6d2b79f5) >>> 0
    let t = s
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

/** 미세한 알갱이 텍스처 타일 — 이게 있어야 "컴퓨터로 칠한 색면" 느낌이 사라진다. */
let grainTile: HTMLCanvasElement | null = null
function getGrainTile(): HTMLCanvasElement {
  if (grainTile) return grainTile
  const size = 140
  const c = document.createElement('canvas')
  c.width = size
  c.height = size
  const g = c.getContext('2d')!
  const img = g.createImageData(size, size)
  const rand = seededRandom(20260916)
  for (let i = 0; i < img.data.length; i += 4) {
    const v = 128 + (rand() - 0.5) * 120
    img.data[i] = v
    img.data[i + 1] = v
    img.data[i + 2] = v
    img.data[i + 3] = 255
  }
  g.putImageData(img, 0, 0)
  grainTile = c
  return c
}

/** 지정한 영역에만 알갱이를 얹는다. overlay 합성이라 밝기는 그대로고 질감만 남는다. */
function overlayGrain(
  ctx: CanvasRenderingContext2D,
  x: number,
  y: number,
  w: number,
  h: number,
  alpha: number,
) {
  const pattern = ctx.createPattern(getGrainTile(), 'repeat')
  if (!pattern) return
  ctx.save()
  ctx.globalCompositeOperation = 'overlay'
  ctx.globalAlpha = alpha
  ctx.fillStyle = pattern
  ctx.fillRect(x, y, w, h)
  ctx.restore()
}

/* ───────────────────── 책상·페이지·소품 ───────────────────── */

/**
 * 낡은 나무 책상 — 판자마다 바탕 톤을 조금씩 다르게 깔고, 판자 안에서 결을 흐르는 곡선으로
 * 긋는다. 직선 줄무늬만 그으면 나무가 아니라 줄무늬 천처럼 보인다.
 */
function drawWoodDesk(ctx: CanvasRenderingContext2D, w: number, h: number) {
  const rand = seededRandom(7788)
  ctx.fillStyle = '#c3b193'
  ctx.fillRect(0, 0, w, h)

  // 판자 — 높이를 조금씩 다르게 해서 규칙적으로 안 보이게.
  const seams: number[] = []
  let y = -30 + rand() * 40
  while (y < h + 50) {
    seams.push(y)
    y += 96 + rand() * 62
  }

  const plankTones = ['#cbb999', '#bda98a', '#d3c2a3', '#b5a07f', '#c6b394']

  for (let i = 0; i < seams.length - 1; i++) {
    const top = seams[i]
    const bottom = seams[i + 1]
    const plankH = bottom - top

    // 판자마다 바탕색을 아예 다르게 깔아야 "판자들"로 읽힌다.
    const tone = plankTones[Math.floor(rand() * plankTones.length)]
    const plankGrad = ctx.createLinearGradient(0, top, 0, bottom)
    plankGrad.addColorStop(0, tone)
    plankGrad.addColorStop(0.5, tone)
    plankGrad.addColorStop(1, '#ab9878')
    ctx.fillStyle = plankGrad
    ctx.fillRect(0, top, w, plankH)

    // 나뭇결 — 판자 안에서 길게 흐르는 곡선. 진한 결/밝은 결을 섞는다.
    const lines = Math.round(plankH / 3.4)
    for (let n = 0; n < lines; n++) {
      const ly = top + (plankH * (n + 0.5)) / lines + (rand() - 0.5) * 4
      const amp = 2 + rand() * 9
      const dark = rand() > 0.42
      ctx.save()
      ctx.globalAlpha = dark ? 0.05 + rand() * 0.11 : 0.04 + rand() * 0.08
      ctx.strokeStyle = dark ? '#6b5330' : '#eadfc6'
      ctx.lineWidth = 0.7 + rand() * 2.4
      ctx.beginPath()
      ctx.moveTo(-10, ly)
      ctx.bezierCurveTo(
        w * 0.28,
        ly + amp,
        w * 0.62,
        ly - amp * 0.8,
        w + 10,
        ly + (rand() - 0.5) * 5,
      )
      ctx.stroke()
      ctx.restore()
    }

    // 옹이 몇 개 — 결이 비껴 흐르는 지점.
    if (rand() > 0.55) {
      const kx = rand() * w
      const ky = top + plankH * (0.3 + rand() * 0.4)
      const kr = 5 + rand() * 9
      ctx.save()
      ctx.globalAlpha = 0.16
      const knot = ctx.createRadialGradient(kx, ky, 1, kx, ky, kr)
      knot.addColorStop(0, '#5f4526')
      knot.addColorStop(1, 'rgba(95, 69, 38, 0)')
      ctx.fillStyle = knot
      ctx.beginPath()
      ctx.ellipse(kx, ky, kr, kr * 0.62, 0, 0, Math.PI * 2)
      ctx.fill()
      ctx.restore()
    }

    // 이음새 — 어두운 골 + 바로 아래 빛 받는 모서리
    ctx.save()
    ctx.globalAlpha = 0.42
    ctx.strokeStyle = '#6b5026'
    ctx.lineWidth = 1.8
    ctx.beginPath()
    ctx.moveTo(0, bottom)
    ctx.lineTo(w, bottom)
    ctx.stroke()
    ctx.globalAlpha = 0.34
    ctx.strokeStyle = '#efe4c9'
    ctx.lineWidth = 1.2
    ctx.beginPath()
    ctx.moveTo(0, bottom + 2)
    ctx.lineTo(w, bottom + 2)
    ctx.stroke()
    ctx.restore()
  }

  // 가장자리를 살짝 어둡게 — 사진처럼 가운데로 시선이 모인다. 과하면 금방 탁해지니 옅게.
  const vignette = ctx.createRadialGradient(w / 2, h / 2, h * 0.3, w / 2, h / 2, h * 0.85)
  vignette.addColorStop(0, 'rgba(0, 0, 0, 0)')
  vignette.addColorStop(1, 'rgba(45, 30, 12, 0.18)')
  ctx.fillStyle = vignette
  ctx.fillRect(0, 0, w, h)

  overlayGrain(ctx, 0, 0, w, h, 0.18)
}

/**
 * 펼쳐진 다이어리 — 왼쪽은 빈 페이지, 오른쪽이 콜라주가 올라가는 페이지.
 * 종이는 단색 면이 아니라 (1) 아래로 갈수록 살짝 가라앉는 그라데이션, (2) 접힌 골을 향해
 * 깊어지는 그늘, (3) 바깥 모서리에 도톰하게 겹친 책장, (4) 종이결 알갱이로 만든다.
 */
function drawOpenBook(ctx: CanvasRenderingContext2D) {
  const x = DESK_MARGIN
  const y = DESK_MARGIN
  const spineX = x + LEFT_PAGE_W + SPINE_W / 2

  // 책 아래 그림자 — 넓고 옅게 깔아야 종이가 떠 보이지 않는다.
  ctx.save()
  ctx.shadowColor = 'rgba(40, 26, 10, 0.45)'
  ctx.shadowBlur = 40
  ctx.shadowOffsetY = 16
  roundRectPath(ctx, x, y, BOOK_W, BOOK_H, 8)
  ctx.fillStyle = '#efe6d1'
  ctx.fill()
  ctx.restore()

  // 바깥으로 겹쳐 보이는 책장들 — 두께감.
  ctx.save()
  ctx.globalAlpha = 0.5
  for (let i = 3; i >= 1; i--) {
    ctx.fillStyle = i % 2 ? '#dccfb2' : '#e8ddc4'
    roundRectPath(ctx, x - i * 1.6, y + i * 1.2, BOOK_W + i * 3.2, BOOK_H - i * 1.2, 8)
    ctx.fill()
  }
  ctx.restore()

  // 종이 바탕
  ctx.save()
  roundRectPath(ctx, x, y, BOOK_W, BOOK_H, 8)
  ctx.clip()

  const paper = ctx.createLinearGradient(0, y, 0, y + BOOK_H)
  paper.addColorStop(0, '#f9f3e3')
  paper.addColorStop(0.45, '#f5eedb')
  paper.addColorStop(1, '#efe5cd')
  ctx.fillStyle = paper
  ctx.fillRect(x, y, BOOK_W, BOOK_H)

  // 왼쪽 페이지는 빛을 덜 받아 살짝 가라앉게.
  ctx.fillStyle = 'rgba(150, 125, 85, 0.1)'
  ctx.fillRect(x, y, LEFT_PAGE_W, BOOK_H)

  // 접힌 골 — 가운데가 가장 깊고 양쪽으로 부드럽게 풀린다.
  const gutter = ctx.createLinearGradient(spineX - 74, 0, spineX + 74, 0)
  gutter.addColorStop(0, 'rgba(92, 70, 38, 0)')
  gutter.addColorStop(0.36, 'rgba(92, 70, 38, 0.1)')
  gutter.addColorStop(0.5, 'rgba(78, 58, 30, 0.3)')
  gutter.addColorStop(0.64, 'rgba(92, 70, 38, 0.08)')
  gutter.addColorStop(1, 'rgba(92, 70, 38, 0)')
  ctx.fillStyle = gutter
  ctx.fillRect(spineX - 74, y, 148, BOOK_H)

  // 페이지 안쪽 가장자리에 아주 옅은 그늘 — 종이가 살짝 휘어 보이게.
  const edge = ctx.createLinearGradient(x + BOOK_W - 60, 0, x + BOOK_W, 0)
  edge.addColorStop(0, 'rgba(120, 95, 55, 0)')
  edge.addColorStop(1, 'rgba(120, 95, 55, 0.12)')
  ctx.fillStyle = edge
  ctx.fillRect(x + BOOK_W - 60, y, 60, BOOK_H)

  overlayGrain(ctx, x, y, BOOK_W, BOOK_H, 0.14)
  ctx.restore()
}

/**
 * 단풍잎 — 뾰족한 갈래 다섯 개를 가진 진짜 단풍 실루엣. 좌표는 잎 중심 기준의 반쪽이고,
 * 나머지 반쪽은 x 를 뒤집어 그린다(단풍잎은 좌우대칭이라 절반만 들고 있으면 된다).
 */
const MAPLE_HALF: [number, number][] = [
  [0.0, -1.0],
  [0.14, -0.62],
  [0.36, -0.7],
  [0.3, -0.42],
  [0.62, -0.46],
  [0.5, -0.2],
  [0.92, -0.12],
  [0.62, 0.1],
  [0.74, 0.28],
  [0.42, 0.26],
  [0.36, 0.5],
  [0.16, 0.34],
  [0.1, 0.62],
]

function drawMapleLeaf(
  ctx: CanvasRenderingContext2D,
  cx: number,
  cy: number,
  size: number,
  rotationDeg: number,
  color: string,
  shadeColor: string,
) {
  const s = size / 2
  ctx.save()
  ctx.translate(cx, cy)
  ctx.rotate((rotationDeg * Math.PI) / 180)

  ctx.beginPath()
  ctx.moveTo(0, MAPLE_HALF[0][1] * s)
  for (const [px, py] of MAPLE_HALF) ctx.lineTo(px * s, py * s)
  for (let i = MAPLE_HALF.length - 1; i >= 0; i--) {
    const [px, py] = MAPLE_HALF[i]
    ctx.lineTo(-px * s, py * s)
  }
  ctx.closePath()

  ctx.save()
  ctx.shadowColor = 'rgba(40, 25, 10, 0.35)'
  ctx.shadowBlur = size * 0.16
  ctx.shadowOffsetY = size * 0.06
  // 잎 안에서도 색이 조금씩 변해야 종잇조각처럼 안 보인다.
  const grad = ctx.createLinearGradient(-s, -s, s, s)
  grad.addColorStop(0, color)
  grad.addColorStop(1, shadeColor)
  ctx.fillStyle = grad
  ctx.fill()
  ctx.restore()

  // 잎맥 — 가운데 한 줄과 좌우로 뻗는 네 줄.
  ctx.save()
  ctx.clip()
  ctx.globalAlpha = 0.28
  ctx.strokeStyle = 'rgba(255, 245, 225, 0.9)'
  ctx.lineWidth = Math.max(0.8, size * 0.022)
  ctx.lineCap = 'round'
  const veins: [number, number][] = [
    [0, -0.92],
    [0.52, -0.5],
    [-0.52, -0.5],
    [0.78, -0.06],
    [-0.78, -0.06],
  ]
  for (const [vx, vy] of veins) {
    ctx.beginPath()
    ctx.moveTo(0, s * 0.42)
    ctx.lineTo(vx * s * 0.9, vy * s * 0.9)
    ctx.stroke()
  }
  ctx.restore()

  // 잎자루
  ctx.globalAlpha = 0.9
  ctx.strokeStyle = shadeColor
  ctx.lineWidth = Math.max(1.2, size * 0.035)
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.moveTo(0, s * 0.5)
  ctx.lineTo(0.04 * s, s * 0.92)
  ctx.stroke()
  ctx.restore()
}

/* ───────────────────── 폴라로이드 배치 ───────────────────── */

/** 폴라로이드 하나씩 번갈아 기울어지게 — 매번 랜덤이면 새로고침마다 들썩여서 고정 패턴을 쓴다. */
const TILT_DEG = [-7, 6, -9, 5, -6, 8, -5, 7, -8]
/** 화이트 테마는 반듯한 게 어울려서 훨씬 덜 기울이고, 몇 장은 아예 똑바로 둔다. */
const WHITE_TILT_DEG = [-3, 2, 0, -2, 3, 0, -1, 2, -2]

/** 카드마다 위아래로 조금씩 어긋나게 — 일렬로 딱 맞으면 인쇄물처럼 뻣뻣해 보인다. */
const OFFSET_RATIO = [-0.07, 0.06, -0.04, 0.08, -0.06, 0.05, -0.08, 0.04, -0.05]

/**
 * 가로로 긴 판이라, 사진을 왼쪽에서 오른쪽으로 죽 늘어놓는다(많으면 두 줄).
 * 카드 전체 높이는 사진 가로변의 약 1.3배라, 칸 너비와 높이 양쪽에 맞춰 크기를 정한다.
 */
function collageLayout(
  n: number,
  gridX: number,
  gridY: number,
  gridW: number,
  gridH: number,
): { cx: number; cy: number; size: number }[] {
  const rows = n > 5 ? 2 : 1
  const perRow = Math.ceil(n / rows)
  const cellW = gridW / perRow
  const cellH = gridH / rows
  const size = Math.min(cellW / 1.22, cellH / 1.42)

  return Array.from({ length: n }, (_, i) => {
    const row = Math.floor(i / perRow)
    const col = i % perRow
    // 마지막 줄이 덜 찼으면 가운데로 모아 한쪽이 비어 보이지 않게 한다.
    const inRow = row === rows - 1 ? n - perRow * row : perRow
    const rowW = cellW * inRow
    const rowX = gridX + (gridW - rowW) / 2
    return {
      cx: rowX + col * cellW + cellW / 2,
      cy: gridY + row * cellH + cellH / 2 + cellH * OFFSET_RATIO[i % OFFSET_RATIO.length],
      size,
    }
  })
}

/**
 * 카드 모양.
 * - color: 카드 전체가 추출색(가느다란 테두리가 아니라).
 * - white: 카드는 하얀 종이 그대로.
 */
type FrameStyle = 'color' | 'white'

/**
 * 폴라로이드 한 장 — 사진은 카드 안에 옅은 여백을 두고 앉고, 아래 남는 영역에 장소 이름 +
 * (있으면) 그때 남긴 한마디를 손글씨 메모처럼 적는다. 테이프 색을 주면 위쪽에 마스킹테이프가
 * 살짝 다른 각도로 걸쳐 붙는다.
 */
function drawPolaroid(
  ctx: CanvasRenderingContext2D,
  opts: {
    cx: number
    cy: number
    size: number
    rotationDeg: number
    img: Drawable | null
    color: string
    title: string
    caption: string | null
    frame: FrameStyle
    /** null 이면 테이프 없이 붙인다. */
    tapeColor: string | null
  },
) {
  const { cx, cy, size, rotationDeg, img, color, title, caption, frame, tapeColor } = opts
  const white = frame === 'white'
  // 사진은 정사각형이 아니라 가로로 살짝 긴 4:3 — 종이 사진에 더 가깝다.
  const photoW = size
  const photoH = size * 0.78
  const padSide = size * 0.075
  const padTop = size * 0.075
  const padBottom = size * (caption ? 0.32 : 0.22)
  const frameW = photoW + padSide * 2
  const frameH = photoH + padTop + padBottom

  ctx.save()
  ctx.translate(cx, cy)
  ctx.rotate((rotationDeg * Math.PI) / 180)

  // 카드 + 그림자 — 여기가 이 폴라로이드의 진짜 배경이다.
  // 종이라서 모서리는 거의 각지게(둥근 모서리를 크게 주면 스티커처럼 보인다).
  // 하얀 카드는 하얀 바탕 위에 놓이니 그림자를 무채색으로 옅게 — 누런 그림자가 지면 때 탄 것 같다.
  ctx.save()
  ctx.shadowColor = white ? 'rgba(30, 30, 30, 0.16)' : 'rgba(55, 40, 18, 0.28)'
  ctx.shadowBlur = white ? 20 : 26
  ctx.shadowOffsetY = white ? 7 : 11
  roundRectPath(ctx, -frameW / 2, -frameH / 2, frameW, frameH, 3)
  ctx.fillStyle = white ? '#ffffff' : color
  ctx.fill()
  ctx.restore()

  // 색 카드에는 아주 옅게 결을 얹어 사진만 도드라져 보이지 않게 한다. 하얀 카드는 매끈하게 둔다.
  if (!white) {
    ctx.save()
    roundRectPath(ctx, -frameW / 2, -frameH / 2, frameW, frameH, 3)
    ctx.clip()
    overlayGrain(ctx, -frameW / 2, -frameH / 2, frameW, frameH, 0.1)
    ctx.restore()
  }

  // 사진
  const photoX = -photoW / 2
  const photoY = -frameH / 2 + padTop
  ctx.save()
  roundRectPath(ctx, photoX, photoY, photoW, photoH, 2)
  ctx.clip()
  if (img) drawCover(ctx, img, photoX, photoY, photoW, photoH)
  else {
    ctx.fillStyle = 'rgba(255, 255, 255, 0.4)'
    ctx.fillRect(photoX, photoY, photoW, photoH)
  }
  ctx.restore()

  // 장소 이름
  ctx.fillStyle = white ? 'rgba(40, 40, 40, 0.86)' : readableTextColor(color)
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  const titleY = caption ? frameH / 2 - padBottom + padBottom * 0.36 : frameH / 2 - padBottom / 2
  ctx.font = `400 ${Math.max(11, Math.round(size * 0.088))}px "${TITLE_FONT}", Pretendard, sans-serif`
  const maxTextW = frameW - padSide * 3
  ctx.fillText(fitText(ctx, title, maxTextW), 0, titleY)

  // 그때 남긴 한마디 — 이름 아래 한 줄, 더 작고 옅게. 사진이 주인공이라 글씨는 거들기만 한다.
  if (caption) {
    ctx.font = `400 ${Math.max(10, Math.round(size * 0.07))}px "${TITLE_FONT}", Pretendard, sans-serif`
    ctx.globalAlpha = white ? 0.6 : 0.72
    ctx.fillText(fitText(ctx, caption, maxTextW), 0, frameH / 2 - padBottom * 0.26)
    ctx.globalAlpha = 1
  }

  // 마스킹테이프 — 카드 위쪽 모서리에 살짝 다른 각도로 걸쳐 붙인 것처럼.
  if (tapeColor) {
    ctx.save()
    ctx.translate(frameW * 0.06, -frameH / 2 + 2)
    ctx.rotate(((rotationDeg >= 0 ? -1 : 1) * 17 * Math.PI) / 180)
    const tapeW = frameW * 0.46
    const tapeH = frameW * 0.16
    ctx.fillStyle = tapeColor
    ctx.fillRect(-tapeW / 2, -tapeH / 2, tapeW, tapeH)
    // 테이프 양 끝을 살짝 진하게 — 종이에 눌러 붙인 느낌.
    ctx.fillStyle = 'rgba(120, 100, 70, 0.14)'
    ctx.fillRect(-tapeW / 2, -tapeH / 2, tapeW * 0.08, tapeH)
    ctx.fillRect(tapeW / 2 - tapeW * 0.08, -tapeH / 2, tapeW * 0.08, tapeH)
    ctx.restore()
  }

  ctx.restore()
}

/* ───────────────────── 파스텔 테마 ───────────────────── */

const PASTEL_INK = ['#f3a8c4', '#a9c8ef', '#f7d98a', '#a9ddc5', '#c9b6e8']

/** 별 하나 — 네 갈래가 볼록하게 들어간 반짝이 모양. */
function drawStar(ctx: CanvasRenderingContext2D, cx: number, cy: number, r: number, color: string) {
  ctx.save()
  ctx.translate(cx, cy)
  ctx.beginPath()
  ctx.moveTo(0, -r)
  ctx.quadraticCurveTo(r * 0.18, -r * 0.18, r, 0)
  ctx.quadraticCurveTo(r * 0.18, r * 0.18, 0, r)
  ctx.quadraticCurveTo(-r * 0.18, r * 0.18, -r, 0)
  ctx.quadraticCurveTo(-r * 0.18, -r * 0.18, 0, -r)
  ctx.closePath()
  ctx.fillStyle = color
  ctx.fill()
  ctx.restore()
}

/** 음표 하나 — 머리 하나에 기둥과 깃발. */
function drawNote(ctx: CanvasRenderingContext2D, cx: number, cy: number, s: number, color: string) {
  ctx.save()
  ctx.translate(cx, cy)
  ctx.rotate(-0.2)
  ctx.fillStyle = color
  ctx.strokeStyle = color
  ctx.lineWidth = s * 0.16
  ctx.lineCap = 'round'
  ctx.beginPath()
  ctx.ellipse(-s * 0.35, s * 0.55, s * 0.4, s * 0.3, -0.35, 0, Math.PI * 2)
  ctx.fill()
  ctx.beginPath()
  ctx.moveTo(s * 0.02, s * 0.5)
  ctx.lineTo(s * 0.02, -s * 0.8)
  ctx.stroke()
  ctx.beginPath()
  ctx.moveTo(s * 0.02, -s * 0.8)
  ctx.quadraticCurveTo(s * 0.75, -s * 0.5, s * 0.5, s * 0.02)
  ctx.stroke()
  ctx.restore()
}

/** 파스텔 종이 — 옅은 점무늬 바탕에 별·음표 낙서를 흩뿌린다. */
function drawPastelScene(ctx: CanvasRenderingContext2D) {
  const grad = ctx.createLinearGradient(0, 0, CANVAS_W, CANVAS_H)
  grad.addColorStop(0, '#fffdf6')
  grad.addColorStop(0.5, '#fdf7f3')
  grad.addColorStop(1, '#f4f7fd')
  ctx.fillStyle = grad
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)

  // 점무늬 — 규칙적으로 깔되 아주 옅게.
  ctx.save()
  ctx.globalAlpha = 0.5
  for (let y = 24; y < CANVAS_H; y += 34) {
    for (let x = 24 + ((y / 34) % 2) * 17; x < CANVAS_W; x += 34) {
      ctx.beginPath()
      ctx.arc(x, y, 1.7, 0, Math.PI * 2)
      ctx.fillStyle = (x + y) % 3 === 0 ? '#f6d7e4' : '#dfe7f6'
      ctx.fill()
    }
  }
  ctx.restore()

  // 낙서 — 자리는 고정 시드로 정해서 다시 그려도 안 들썩인다.
  const rand = seededRandom(424242)
  for (let i = 0; i < 26; i++) {
    const x = 30 + rand() * (CANVAS_W - 60)
    const y = 30 + rand() * (CANVAS_H - 60)
    const color = PASTEL_INK[Math.floor(rand() * PASTEL_INK.length)]
    ctx.save()
    ctx.globalAlpha = 0.55 + rand() * 0.35
    if (i % 4 === 0) drawNote(ctx, x, y, 12 + rand() * 8, color)
    else drawStar(ctx, x, y, 6 + rand() * 9, color)
    ctx.restore()
  }
  overlayGrain(ctx, 0, 0, CANVAS_W, CANVAS_H, 0.08)
}

/* ───────────────────── 화이트 테마 ───────────────────── */

/** 화이트 — 아무 무늬 없는 하얀 종이. 하얀 카드가 떠 보이도록 바탕만 아주 살짝 회색을 준다. */
function drawWhiteScene(ctx: CanvasRenderingContext2D) {
  ctx.fillStyle = '#f6f6f5'
  ctx.fillRect(0, 0, CANVAS_W, CANVAS_H)
}

/* ───────────────────── 조립 ───────────────────── */

/** 테마마다 사진을 붙일 자리, 제목 자리, 글자색, 카드 모양, 테이프 색이 다르다. */
interface SceneLayout {
  gridX: number
  gridY: number
  gridW: number
  gridH: number
  titleX: number
  titleY: number
  titleColor: string
  periodColor: string
  frame: FrameStyle
  tilts: number[]
  /** null 이면 테이프 없이 붙인다. */
  tapeColors: string[] | null
}

/** 폴라로이드 한 장에 필요한 것 — 이미 불러온 그림과 추출색, 적을 글자. */
interface Tile {
  img: Drawable | null
  color: string
  title: string
  caption: string | null
}

/** 콜라주 글씨 폰트를 기다린다 — 안 기다리면 첫 그림만 기본 글꼴로 그려진다. */
function loadTitleFonts() {
  return Promise.allSettled([document.fonts.load(`400 40px "${TITLE_FONT}"`), document.fonts.ready])
}

function sceneFor(theme: CollageTheme): SceneLayout {
  if (theme === '노트') {
    return {
      gridX: RIGHT_PAGE_X + PAGE_PAD,
      gridY: DESK_MARGIN + TOP_PEEK,
      gridW: RIGHT_PAGE_W - PAGE_PAD * 2,
      gridH: GRID_H,
      titleX: RIGHT_PAGE_X + PAGE_PAD + 4,
      titleY: DESK_MARGIN + TOP_PEEK + GRID_H + TITLE_GAP + 40,
      titleColor: '#a8592e',
      periodColor: '#b97645',
      frame: 'color',
      tilts: TILT_DEG,
      tapeColors: ['rgba(226, 206, 168, 0.82)'],
    }
  }
  // 파스텔·화이트는 종이 한 장을 통째로 쓰니 사진·제목 자리가 같다.
  const sheet = {
    gridX: SHEET_PAD,
    gridY: SHEET_PAD - 10,
    gridW: CANVAS_W - SHEET_PAD * 2,
    gridH: CANVAS_H - SHEET_PAD - TITLE_BLOCK_H,
    titleX: SHEET_PAD + 4,
    titleY: CANVAS_H - TITLE_BLOCK_H + 66,
  }
  if (theme === '화이트') {
    return {
      ...sheet,
      titleColor: '#2f2f2f',
      periodColor: '#9a9a9a',
      frame: 'white',
      tilts: WHITE_TILT_DEG,
      tapeColors: null,
    }
  }
  return {
    ...sheet,
    titleColor: '#d4739b',
    periodColor: '#8aa7d8',
    frame: 'color',
    tilts: TILT_DEG,
    // 파스텔 테마는 사진마다 다른 색 테이프를 붙인다.
    tapeColors: ['rgba(246, 183, 207, 0.72)', 'rgba(174, 205, 240, 0.72)', 'rgba(250, 224, 160, 0.72)'],
  }
}

/** 테마 장면을 깔고 그 위에 타일·제목을 올린다. 그림은 전부 미리 불러와 넘겨야 한다(동기로 그린다). */
function paintCollage(
  ctx: CanvasRenderingContext2D,
  input: Omit<CollageInput, 'pieces'>,
  tiles: Tile[],
  extra: number,
) {
  if (input.theme === '노트') {
    // 책상 위에 다이어리를 펼쳐놓은 장면.
    drawWoodDesk(ctx, CANVAS_W, CANVAS_H)
    drawOpenBook(ctx)
    // 단풍잎 — 책상 위에 몇 장 떨어져 있는 정도로만. 소품을 늘어놓을수록 금방 조잡해진다.
    drawMapleLeaf(ctx, DESK_MARGIN * 0.5, CANVAS_H * 0.28, 62, -32, '#d2703a', '#a4401f')
    drawMapleLeaf(ctx, CANVAS_W - DESK_MARGIN * 0.45, CANVAS_H * 0.68, 68, 24, '#e0a33a', '#b26a1f')
  } else if (input.theme === '화이트') {
    drawWhiteScene(ctx)
  } else {
    drawPastelScene(ctx)
  }
  const scene = sceneFor(input.theme)

  // 폴라로이드들 — 그리드 영역에 스크랩북처럼 붙인다.
  const { gridX, gridY, gridW, gridH } = scene
  const positions = collageLayout(tiles.length, gridX, gridY, gridW, gridH)
  tiles.forEach((tile, i) => {
    const pos = positions[i]
    drawPolaroid(ctx, {
      cx: pos.cx,
      cy: pos.cy,
      size: pos.size,
      rotationDeg: scene.tilts[i % scene.tilts.length],
      img: tile.img,
      color: tile.color,
      title: tile.title,
      caption: tile.caption,
      frame: scene.frame,
      tapeColor: scene.tapeColors ? scene.tapeColors[i % scene.tapeColors.length] : null,
    })
  })

  if (extra > 0) {
    // 남은 장수도 폴라로이드처럼 — 사진 대신 "+N장 더"라고 적힌 빈 카드 한 장.
    const w = Math.min(gridW, gridH) * 0.24
    const h = w * 1.26
    const cx = gridX + gridW - w * 0.62
    const cy = gridY + gridH - h * 0.62
    ctx.save()
    ctx.translate(cx, cy)
    ctx.rotate((-5 * Math.PI) / 180)
    ctx.save()
    ctx.shadowColor = 'rgba(50, 38, 20, 0.3)'
    ctx.shadowBlur = 14
    ctx.shadowOffsetY = 7
    roundRectPath(ctx, -w / 2, -h / 2, w, h, 3)
    ctx.fillStyle = scene.frame === 'white' ? '#ffffff' : '#efe6d2'
    ctx.fill()
    ctx.restore()
    ctx.fillStyle = 'rgba(120, 100, 70, 0.85)'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.font = `400 ${Math.round(w * 0.3)}px "${TITLE_FONT}", Pretendard, sans-serif`
    ctx.fillText(`+${extra}`, 0, -h * 0.06)
    ctx.font = `400 ${Math.round(w * 0.15)}px "${TITLE_FONT}", Pretendard, sans-serif`
    ctx.fillText('장 더', 0, h * 0.2)
    ctx.restore()
  }

  // 제목 블록 — 왼쪽 아래에 적어둔 것처럼 왼쪽 정렬. 여행 이름과 기간 두 줄.
  const titleX = scene.titleX
  const titleY = scene.titleY
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'

  ctx.fillStyle = scene.titleColor
  let titleSize = 48
  ctx.font = `400 ${titleSize}px "${TITLE_FONT}", Pretendard, sans-serif`
  while (ctx.measureText(input.title).width > gridW && titleSize > 22) {
    titleSize -= 2
    ctx.font = `400 ${titleSize}px "${TITLE_FONT}", Pretendard, sans-serif`
  }
  ctx.fillText(input.title, titleX, titleY)

  ctx.fillStyle = scene.periodColor
  ctx.font = `400 24px "${TITLE_FONT}", Pretendard, sans-serif`
  ctx.fillText(input.period, titleX, titleY + 40)
}

async function drawPamphlet(canvas: HTMLCanvasElement, input: CollageInput, ordered: TripSpot[]) {
  canvas.width = CANVAS_W
  canvas.height = CANVAS_H

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 너무 많으면 마지막 칸을 "+N개"로 접어서, 사진 한 장 한 장이 너무 작아지지 않게 한다.
  const cap = ordered.length > MAX_TILES ? MAX_TILES - 1 : ordered.length
  const shown = ordered.slice(0, cap)

  const [images] = await Promise.all([
    Promise.all(shown.map((p) => (p.photo ? loadImage(p.photo).catch(() => null) : Promise.resolve(null)))),
    loadTitleFonts(),
  ])

  paintCollage(
    ctx,
    input,
    shown.map((p, i) => ({
      img: images[i],
      color: p.verifiedColor ?? '#e9f2e4',
      title: p.title,
      caption: p.caption,
    })),
    ordered.length - shown.length,
  )
}

/* ───────────────────── 테마 미리보기 ───────────────────── */

/**
 * 미리보기에 붙일 예시 사진 — 실제 사진 대신 제주 풍경을 단순한 도형으로 그린다.
 * 카드 색(color)은 그 풍경에서 뽑혔을 법한 색으로 맞춰 둔다.
 */
const SAMPLE_SCENES: {
  title: string
  color: string
  sky: [string, string]
  kind: 'mountain' | 'sea' | 'sunset' | 'oreum'
}[] = [
  { title: '유채꽃밭', color: '#f2cf4a', sky: ['#9fd0f0', '#e4f3fb'], kind: 'mountain' },
  { title: '협재 해변', color: '#5cc4c0', sky: ['#8fcdf2', '#d9f1fb'], kind: 'sea' },
  { title: '노을 산책', color: '#e7a3c0', sky: ['#b99ad8', '#f6c1c9'], kind: 'sunset' },
  { title: '새별오름', color: '#8cbf7a', sky: ['#b7e0f5', '#eef8fb'], kind: 'oreum' },
]

function drawSampleScene(scene: (typeof SAMPLE_SCENES)[number]): HTMLCanvasElement {
  const w = 240
  const h = 188
  const c = document.createElement('canvas')
  c.width = w
  c.height = h
  const g = c.getContext('2d')!
  const sky = g.createLinearGradient(0, 0, 0, h)
  sky.addColorStop(0, scene.sky[0])
  sky.addColorStop(1, scene.sky[1])
  g.fillStyle = sky
  g.fillRect(0, 0, w, h)

  if (scene.kind === 'mountain') {
    // 한라산 + 앞에 깔린 유채꽃
    g.fillStyle = '#8ea5a8'
    g.beginPath()
    g.moveTo(0, h * 0.7)
    g.quadraticCurveTo(w * 0.5, h * 0.18, w, h * 0.7)
    g.fill()
    g.fillStyle = '#f2cf4a'
    g.fillRect(0, h * 0.66, w, h)
    g.fillStyle = '#7fa65a'
    g.fillRect(0, h * 0.9, w, h)
  } else if (scene.kind === 'sea') {
    // 바다 + 현무암
    g.fillStyle = '#3fb7c0'
    g.fillRect(0, h * 0.5, w, h)
    g.fillStyle = '#7fd6d3'
    g.fillRect(0, h * 0.5, w, h * 0.06)
    g.fillStyle = '#2f3437'
    g.beginPath()
    g.ellipse(w * 0.2, h * 0.92, w * 0.26, h * 0.14, 0, 0, Math.PI * 2)
    g.ellipse(w * 0.78, h * 0.96, w * 0.3, h * 0.16, 0, 0, Math.PI * 2)
    g.fill()
  } else if (scene.kind === 'sunset') {
    // 노을 지는 해 + 보랏빛 바다
    g.fillStyle = '#ffe0a8'
    g.beginPath()
    g.arc(w * 0.5, h * 0.6, w * 0.12, 0, Math.PI * 2)
    g.fill()
    g.fillStyle = '#8f7aa8'
    g.fillRect(0, h * 0.62, w, h)
    g.fillStyle = '#4a3b4f'
    g.fillRect(0, h * 0.88, w, h)
  } else {
    // 둥근 오름 두 개
    g.fillStyle = '#7fb56a'
    g.beginPath()
    g.ellipse(w * 0.3, h * 0.95, w * 0.5, h * 0.5, 0, 0, Math.PI * 2)
    g.fill()
    g.fillStyle = '#a9d38f'
    g.beginPath()
    g.ellipse(w * 0.85, h * 1.02, w * 0.45, h * 0.4, 0, 0, Math.PI * 2)
    g.fill()
  }
  return c
}

let sampleTiles: Tile[] | null = null
function getSampleTiles(): Tile[] {
  sampleTiles ??= SAMPLE_SCENES.map((s) => ({
    img: drawSampleScene(s),
    color: s.color,
    title: s.title,
    caption: null,
  }))
  return sampleTiles
}

/** 테마 고르는 화면의 작은 예시 그림 — 실제 콜라주와 같은 그리기 코드에 예시 사진만 넣는다. */
export function ThemePreview({ theme }: { theme: CollageTheme }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)

  useEffect(() => {
    let cancelled = false
    void loadTitleFonts().then(() => {
      const ctx = canvasRef.current?.getContext('2d')
      if (cancelled || !ctx) return
      paintCollage(ctx, { theme, title: '제주 여행', period: '2박 3일' }, getSampleTiles(), 0)
    })
    return () => {
      cancelled = true
    }
  }, [theme])

  // width/height 를 먼저 박아둬야 다 그리기 전에도 칸 비율이 흔들리지 않는다.
  return <canvas ref={canvasRef} className="theme-preview" width={CANVAS_W} height={CANVAS_H} />
}

/**
 * 고른 사진들을 시간순으로 정렬해 캔버스에 그린다.
 * 캔버스와 "다 그렸는지"를 돌려주므로, 저장 버튼은 다 그려진 뒤에 눌리게 할 수 있다.
 */
export function useCollage(canvasRef: React.RefObject<HTMLCanvasElement | null>, input: CollageInput) {
  const [ready, setReady] = useState(false)
  const ordered = [...input.pieces].sort((a, b) => (a.verifiedAt ?? 0) - (b.verifiedAt ?? 0))
  // effect 의존성 배열을 배열 참조가 아니라 실제 내용으로 비교하려고 문자열로 만든다.
  const piecesKey = ordered
    .map((p) => `${p.contentId}:${p.photo ?? ''}:${p.verifiedColor ?? ''}:${p.caption ?? ''}`)
    .join('|')

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || ordered.length === 0) return
    let cancelled = false
    setReady(false)
    void drawPamphlet(canvas, input, ordered).then(() => {
      if (!cancelled) setReady(true)
    })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [input.title, input.period, input.theme, piecesKey])

  return { ready, hasPieces: ordered.length > 0 }
}

/** 콜라주 한 장 — 캔버스와 저장 버튼. */
export function Pamphlet({ input }: { input: CollageInput }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const { ready, hasPieces } = useCollage(canvasRef, input)

  function download() {
    const canvas = canvasRef.current
    if (!canvas) return
    const a = document.createElement('a')
    a.download = `${input.title} 콜라주.png`
    a.href = canvas.toDataURL('image/png')
    a.click()
  }

  if (!hasPieces) {
    return <div className="empty t-body">아직 인증한 사진이 없어요.</div>
  }

  return (
    <div className="pamphlet">
      <canvas ref={canvasRef} className="pamphlet__canvas" />
      <button
        className="btn-primary t-button pamphlet__download"
        disabled={!ready}
        onClick={download}
        aria-label="콜라주 저장"
      >
        <DownloadIcon />
      </button>
    </div>
  )
}
