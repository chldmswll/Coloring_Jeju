import { useEffect, useRef, useState } from 'react'
import type { Trip, TripSpot } from '../types'

/**
 * 조각모음 — 여행 하나에서 인증한 사진들을 자동으로 콜라주 한 장으로 모아준다("팜플렛").
 * 나무 책상 위에 펼쳐둔 다이어리 — 오른쪽 페이지에 폴라로이드를 스크랩하고 아래에 여행
 * 제목을 손글씨로 적은 모습을 캔버스로 그린다. 사진이 없는 곳은 그때 뽑은 색으로 대신 채운다.
 *
 * 그리는 로직(drawPamphlet)은 앨범 목록의 작은 미리보기(PamphletPreview)와 상세 화면의
 * 큰 버전(Pamphlet) 둘 다에서 그대로 재사용한다 — 같은 콜라주를 크기만 다르게 보여주는 것이므로.
 */

const CANVAS_W = 940
/** 책상 나무가 보이는 바깥 여백 — 넉넉해야 "책상 위에 놓인 노트"로 읽힌다. */
const DESK_MARGIN = 62
/** 왼쪽 빈 페이지가 책 너비에서 차지하는 비율 — 나머지가 사진을 붙이는 오른쪽 페이지. */
const LEFT_PAGE_RATIO = 0.15
/** 책 가운데 접힌 부분(어두운 그림자 줄)의 너비. */
const SPINE_W = 14
/** 오른쪽 페이지 안쪽 좌우 여백. */
const PAGE_PAD = 26
/** 페이지 위쪽, 삐져나온 사진 소품을 위해 비워두는 공간. */
const TOP_PEEK = 46
const GRID_H = 660
const TITLE_GAP = 30
const TITLE_BLOCK_H = 210
const PAGE_BOTTOM_PAD = 40

const BOOK_W = CANVAS_W - DESK_MARGIN * 2
const BOOK_H = TOP_PEEK + GRID_H + TITLE_GAP + TITLE_BLOCK_H + PAGE_BOTTOM_PAD
const CANVAS_H = BOOK_H + DESK_MARGIN * 2
const LEFT_PAGE_W = BOOK_W * LEFT_PAGE_RATIO
const RIGHT_PAGE_X = DESK_MARGIN + LEFT_PAGE_W + SPINE_W
const RIGHT_PAGE_W = BOOK_W - LEFT_PAGE_W - SPINE_W

/** 타일이 이보다 많으면 마지막 칸을 "+N"으로 접어 넣는다 — 사진이 너무 잘게 쪼개지지 않게. */
const MAX_TILES = 9
/** 미리보기 박스를 이 비율로 맞춰야 안 찌그러진다. */
export const PAMPHLET_ASPECT = CANVAS_W / CANVAS_H

/** 손글씨 느낌 제목 전용 폰트 — index.html 에서 Google Fonts로 불러온다. 본문은 계속 Pretendard. */
const TITLE_FONT = 'Gaegu'

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const img = new Image()
    img.onload = () => resolve(img)
    img.onerror = reject
    img.src = src
  })
}

/** 사진 개수에 맞춰 정사각형에 가까운 격자(열x행)를 정한다. 미리 짜둔 배치가 없는 개수(7장+)에만 쓴다. */
function gridFor(n: number): { cols: number; rows: number } {
  const cols = Math.max(1, Math.ceil(Math.sqrt(n)))
  const rows = Math.max(1, Math.ceil(n / cols))
  return { cols, rows }
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
  img: HTMLImageElement,
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

/**
 * 자연스럽게 겹쳐 붙인 느낌을 내려고, 사진이 적을 때(1~6장)는 미리 잡아둔 배치를 쓰고,
 * 그보다 많으면 격자로 떨어진다. x/y 는 그리드 영역 안에서의 비율(0~1), scale 은 크기 배수.
 */
const CLUSTER_LAYOUTS: Record<
  number,
  { base: number; slots: { x: number; y: number; scale: number }[] }
> = {
  1: { base: 0.62, slots: [{ x: 0.5, y: 0.46, scale: 1 }] },
  2: {
    base: 0.46,
    slots: [
      { x: 0.35, y: 0.32, scale: 1.05 },
      { x: 0.65, y: 0.66, scale: 1 },
    ],
  },
  3: {
    base: 0.38,
    slots: [
      { x: 0.3, y: 0.24, scale: 0.95 },
      { x: 0.72, y: 0.36, scale: 1.08 },
      { x: 0.42, y: 0.72, scale: 1.02 },
    ],
  },
  4: {
    base: 0.34,
    slots: [
      { x: 0.28, y: 0.22, scale: 0.94 },
      { x: 0.72, y: 0.34, scale: 1.06 },
      { x: 0.31, y: 0.66, scale: 1.02 },
      { x: 0.73, y: 0.79, scale: 0.98 },
    ],
  },
  5: {
    base: 0.29,
    slots: [
      { x: 0.24, y: 0.19, scale: 0.95 },
      { x: 0.66, y: 0.16, scale: 1.06 },
      { x: 0.79, y: 0.5, scale: 0.94 },
      { x: 0.27, y: 0.55, scale: 1.02 },
      { x: 0.58, y: 0.82, scale: 1 },
    ],
  },
  6: {
    base: 0.25,
    slots: [
      { x: 0.2, y: 0.16, scale: 0.94 },
      { x: 0.55, y: 0.14, scale: 1.04 },
      { x: 0.83, y: 0.36, scale: 0.94 },
      { x: 0.2, y: 0.52, scale: 1 },
      { x: 0.53, y: 0.62, scale: 1.06 },
      { x: 0.8, y: 0.83, scale: 0.94 },
    ],
  },
}

function clusterLayout(
  n: number,
  gridX: number,
  gridY: number,
  gridW: number,
  gridH: number,
): { cx: number; cy: number; size: number }[] {
  const layout = CLUSTER_LAYOUTS[n]
  if (layout) {
    // base 배수는 "한마디까지 달린 가장 높은 카드"가 자기 자리에 들어가는 값으로 잡아뒀다.
    const base = Math.min(gridW, gridH) * layout.base
    return layout.slots.map((s) => ({
      cx: gridX + s.x * gridW,
      cy: gridY + s.y * gridH,
      size: base * s.scale,
    }))
  }
  // 미리 잡아둔 배치가 없을 만큼 많으면(7장+) 격자로 떨어진다.
  const { cols, rows } = gridFor(n)
  const cellW = gridW / cols
  const cellH = gridH / rows
  // 카드 전체 높이가 사진 가로변의 약 1.26배라, 칸 높이에도 맞춰 잡아야 위아래로 안 넘친다.
  const size = Math.min(cellW / 1.2, cellH / 1.3)
  return Array.from({ length: n }, (_, i) => {
    const col = i % cols
    const row = Math.floor(i / cols)
    return {
      cx: gridX + col * cellW + cellW / 2,
      cy: gridY + row * cellH + cellH / 2,
      size,
    }
  })
}

/**
 * 폴라로이드 한 장 — 카드 전체가 추출색이다(가느다란 테두리가 아니라). 사진은 그 색 카드
 * 안에 옅은 여백을 두고 앉고, 아래 남는 색 영역에 장소 이름 + (있으면) 그때 남긴 한마디를
 * 손글씨 메모처럼 적는다. 위쪽엔 마스킹테이프가 살짝 다른 각도로 걸쳐 붙어 있다.
 */
function drawPolaroid(
  ctx: CanvasRenderingContext2D,
  opts: {
    cx: number
    cy: number
    size: number
    rotationDeg: number
    img: HTMLImageElement | null
    color: string
    title: string
    caption: string | null
  },
) {
  const { cx, cy, size, rotationDeg, img, color, title, caption } = opts
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

  // 추출색 카드 전체 + 그림자 — 여기가 이 폴라로이드의 진짜 배경이다.
  // 종이라서 모서리는 거의 각지게(둥근 모서리를 크게 주면 스티커처럼 보인다).
  ctx.save()
  ctx.shadowColor = 'rgba(55, 40, 18, 0.28)'
  ctx.shadowBlur = 26
  ctx.shadowOffsetY = 11
  roundRectPath(ctx, -frameW / 2, -frameH / 2, frameW, frameH, 3)
  ctx.fillStyle = color
  ctx.fill()
  ctx.restore()

  // 카드에도 아주 옅게 결을 얹어 사진만 도드라져 보이지 않게 한다.
  ctx.save()
  roundRectPath(ctx, -frameW / 2, -frameH / 2, frameW, frameH, 3)
  ctx.clip()
  overlayGrain(ctx, -frameW / 2, -frameH / 2, frameW, frameH, 0.1)
  ctx.restore()

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

  // 장소 이름 — 굵게.
  const textColor = readableTextColor(color)
  ctx.fillStyle = textColor
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  const titleY = caption ? frameH / 2 - padBottom + padBottom * 0.36 : frameH / 2 - padBottom / 2
  ctx.font = `700 ${Math.max(11, Math.round(size * 0.088))}px "${TITLE_FONT}", Pretendard, sans-serif`
  let titleText = title
  const maxTextW = frameW - padSide * 3
  while (ctx.measureText(titleText).width > maxTextW && titleText.length > 1) {
    titleText = titleText.slice(0, -1)
  }
  if (titleText !== title) titleText = titleText.slice(0, -1) + '…'
  ctx.fillText(titleText, 0, titleY)

  // 그때 남긴 한마디 — 이름 아래 한 줄, 더 작고 옅게. 사진이 주인공이라 글씨는 거들기만 한다.
  if (caption) {
    ctx.font = `400 ${Math.max(10, Math.round(size * 0.075))}px "${TITLE_FONT}", Pretendard, sans-serif`
    let memo = caption
    while (ctx.measureText(memo).width > maxTextW && memo.length > 1) memo = memo.slice(0, -1)
    if (memo !== caption) memo = memo.slice(0, -1) + '…'
    ctx.globalAlpha = 0.72
    ctx.fillText(`“${memo}”`, 0, frameH / 2 - padBottom * 0.26)
    ctx.globalAlpha = 1
  }

  // 마스킹테이프 — 카드 위쪽 모서리에 살짝 다른 각도로 걸쳐 붙인 것처럼.
  ctx.save()
  ctx.translate(frameW * 0.06, -frameH / 2 + 2)
  ctx.rotate(((rotationDeg >= 0 ? -1 : 1) * 17 * Math.PI) / 180)
  const tapeW = frameW * 0.46
  const tapeH = frameW * 0.16
  ctx.fillStyle = 'rgba(226, 206, 168, 0.82)'
  ctx.fillRect(-tapeW / 2, -tapeH / 2, tapeW, tapeH)
  // 테이프 양 끝을 살짝 진하게 — 종이에 눌러 붙인 느낌.
  ctx.fillStyle = 'rgba(198, 176, 134, 0.5)'
  ctx.fillRect(-tapeW / 2, -tapeH / 2, tapeW * 0.08, tapeH)
  ctx.fillRect(tapeW / 2 - tapeW * 0.08, -tapeH / 2, tapeW * 0.08, tapeH)
  ctx.restore()

  ctx.restore()
}

/* ───────────────────── 조립 ───────────────────── */

async function drawPamphlet(canvas: HTMLCanvasElement, trip: Trip, ordered: TripSpot[]) {
  canvas.width = CANVAS_W
  canvas.height = CANVAS_H

  const ctx = canvas.getContext('2d')
  if (!ctx) return

  // 너무 많으면 마지막 칸을 "+N개"로 접어서, 사진 한 장 한 장이 너무 작아지지 않게 한다.
  const cap = ordered.length > MAX_TILES ? MAX_TILES - 1 : ordered.length
  const shown = ordered.slice(0, cap)
  const extra = ordered.length - shown.length

  const [images] = await Promise.all([
    Promise.all(shown.map((p) => (p.photo ? loadImage(p.photo).catch(() => null) : Promise.resolve(null)))),
    Promise.allSettled([
      document.fonts.load(`700 40px "${TITLE_FONT}"`),
      document.fonts.load(`400 24px "${TITLE_FONT}"`),
      document.fonts.ready,
    ]),
  ])

  // 책상 위에 다이어리를 펼쳐놓은 장면.
  drawWoodDesk(ctx, canvas.width, canvas.height)
  drawOpenBook(ctx)

  // 모서리 소품들 — 나침반, 동전, 반짝임, 낙엽, 삐져나온 사진.
  // 단풍잎 — 책상 위에 몇 장 떨어져 있는 정도로만. 소품을 늘어놓을수록 금방 조잡해진다.
  drawMapleLeaf(ctx, DESK_MARGIN * 0.48, canvas.height * 0.3, 74, -32, '#d2703a', '#a4401f')
  drawMapleLeaf(ctx, canvas.width - DESK_MARGIN * 0.42, canvas.height * 0.63, 82, 24, '#e0a33a', '#b26a1f')
  drawMapleLeaf(ctx, canvas.width * 0.32, canvas.height - DESK_MARGIN * 0.42, 58, 14, '#c14f2c', '#8c2f1b')

  // 폴라로이드들 — 오른쪽 페이지 그리드 영역에 스크랩북처럼 붙인다.
  const gridX = RIGHT_PAGE_X + PAGE_PAD
  const gridY = DESK_MARGIN + TOP_PEEK
  const gridW = RIGHT_PAGE_W - PAGE_PAD * 2

  const positions = clusterLayout(shown.length, gridX, gridY, gridW, GRID_H)
  shown.forEach((p, i) => {
    const pos = positions[i]
    drawPolaroid(ctx, {
      cx: pos.cx,
      cy: pos.cy,
      size: pos.size,
      rotationDeg: TILT_DEG[i % TILT_DEG.length],
      img: images[i],
      color: p.verifiedColor ?? '#e9f2e4',
      title: p.title,
      caption: p.caption,
    })
  })

  if (extra > 0) {
    // 남은 장수도 폴라로이드처럼 — 사진 대신 "+N장 더"라고 적힌 빈 카드 한 장.
    const w = Math.min(gridW, GRID_H) * 0.24
    const h = w * 1.26
    const cx = gridX + gridW - w * 0.62
    const cy = gridY + GRID_H - h * 0.62
    ctx.save()
    ctx.translate(cx, cy)
    ctx.rotate((-5 * Math.PI) / 180)
    ctx.save()
    ctx.shadowColor = 'rgba(50, 38, 20, 0.3)'
    ctx.shadowBlur = 14
    ctx.shadowOffsetY = 7
    roundRectPath(ctx, -w / 2, -h / 2, w, h, 3)
    ctx.fillStyle = '#efe6d2'
    ctx.fill()
    ctx.restore()
    ctx.fillStyle = 'rgba(120, 100, 70, 0.85)'
    ctx.textAlign = 'center'
    ctx.textBaseline = 'middle'
    ctx.font = `700 ${Math.round(w * 0.3)}px "${TITLE_FONT}", Pretendard, sans-serif`
    ctx.fillText(`+${extra}`, 0, -h * 0.06)
    ctx.font = `400 ${Math.round(w * 0.15)}px "${TITLE_FONT}", Pretendard, sans-serif`
    ctx.fillText('장 더', 0, h * 0.2)
    ctx.restore()
  }

  // 제목 블록 — 오른쪽 페이지 왼쪽 아래에 손글씨로 적어둔 것처럼 왼쪽 정렬.
  const titleX = gridX + 4
  let titleY = gridY + GRID_H + TITLE_GAP + 40
  ctx.textAlign = 'left'
  ctx.textBaseline = 'alphabetic'

  ctx.fillStyle = '#a8592e'
  let titleSize = 48
  ctx.font = `700 ${titleSize}px "${TITLE_FONT}", Pretendard, sans-serif`
  while (ctx.measureText(trip.name).width > gridW && titleSize > 22) {
    titleSize -= 2
    ctx.font = `700 ${titleSize}px "${TITLE_FONT}", Pretendard, sans-serif`
  }
  ctx.fillText(trip.name, titleX, titleY)

  titleY += 42
  ctx.fillStyle = '#b97645'
  ctx.font = `400 26px "${TITLE_FONT}", Pretendard, sans-serif`
  ctx.fillText(`${trip.startDate} ~ ${trip.endDate}`, titleX, titleY)

  titleY += 36
  ctx.fillStyle = 'rgba(58, 55, 46, 0.72)'
  ctx.font = '500 18px Pretendard, sans-serif'
  const photographers = Object.values(trip.members).join(', ') || '여행자'
  ctx.fillText(`사진 : ${photographers}`, titleX + 2, titleY)
}

/** 인증한 사진들을 시간순으로 정렬해 캔버스에 그린다. 캔버스 ref·준비 상태를 함께 관리. */
function useCollage(canvasRef: React.RefObject<HTMLCanvasElement | null>, trip: Trip, pieces: TripSpot[]) {
  const [ready, setReady] = useState(false)
  const ordered = [...pieces].sort((a, b) => (a.verifiedAt ?? 0) - (b.verifiedAt ?? 0))
  // effect 의존성 배열을 배열 참조가 아니라 실제 내용으로 비교하려고 문자열로 만든다.
  const piecesKey = ordered
    .map((p) => `${p.contentId}:${p.photo ?? ''}:${p.verifiedColor ?? ''}:${p.caption ?? ''}`)
    .join('|')

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas || ordered.length === 0) return
    let cancelled = false
    setReady(false)
    void drawPamphlet(canvas, trip, ordered).then(() => {
      if (!cancelled) setReady(true)
    })
    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [trip.id, trip.name, trip.startDate, trip.endDate, piecesKey])

  return { ready, hasPieces: ordered.length > 0 }
}

/** 앨범 목록 카드에 쓰는 작은 미리보기 — 다운로드 버튼 없이 캔버스만. */
export function PamphletPreview({ trip, pieces }: { trip: Trip; pieces: TripSpot[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const { hasPieces } = useCollage(canvasRef, trip, pieces)

  if (!hasPieces) {
    return <div className="pamphlet-preview pamphlet-preview--empty" aria-hidden="true" />
  }
  return (
    <div className="pamphlet-preview">
      <canvas ref={canvasRef} className="pamphlet-preview__canvas" />
    </div>
  )
}

/** 상세 화면의 큰 버전 — 다운로드까지 할 수 있다. */
export function Pamphlet({ trip, pieces }: { trip: Trip; pieces: TripSpot[] }) {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const { ready, hasPieces } = useCollage(canvasRef, trip, pieces)

  function download() {
    const canvas = canvasRef.current
    if (!canvas) return
    const a = document.createElement('a')
    a.download = `${trip.name} 팜플렛.png`
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
      >
        사진으로 저장
      </button>
    </div>
  )
}
