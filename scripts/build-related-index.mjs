/**
 * "함께 가면 좋은 곳" 추천 인덱스를 만든다 → src/data/relatedIndex.json
 *
 *   npm run related:index
 *
 * 한국관광 데이터랩(연관 관광지)과 TourAPI 는 같은 곳을 서로 다른 이름으로 부른다
 * ("검멀레해수욕장" / "검멀레해변"). 앱이 장소를 담을 때마다 이름으로 검색해 이어 붙이면
 * 느리고, 부분일치 검색의 한계로 놓치는 곳이 많다. 그래서 두 쪽 제주 데이터를 통째로 받아
 * 여기서 한 번에 이어 붙이고, 앱은 "TourAPI 장소 ID → 추천 장소 ID" 결과만 읽는다.
 *
 * 연관 순위는 월 단위로 갱신된다. 가끔(몇 달에 한 번) 다시 돌려서 커밋하면 된다.
 * 키는 .env.local 의 TOUR_API_SERVICE_KEY 를 쓴다 — 같은 키로 "관광지별 연관 관광지 정보"
 * 서비스도 활용신청돼 있어야 한다.
 */
import { readFileSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

const ROOT = resolve(dirname(fileURLToPath(import.meta.url)), '..')
const OUT = resolve(ROOT, 'src/data/relatedIndex.json')

/** 추천 목록은 장소당 이만큼만 저장한다 — 앱은 이미 담은 곳을 빼고 5개를 보여준다. */
const TOP_N = 10
/** 앱이 "관광지"로 인정하는 contenttypeid — src/api/tourApi.ts 의 ATTRACTION_TYPE_IDS 와 같다. */
const TYPE_IDS = ['12', '14', '15', '28']
/** 데이터랩(법정동 코드): 제주 50, 제주시 50110, 서귀포시 50130 */
const SIGNGU_CDS = ['50110', '50130']

/**
 * 규칙으로는 못 잇는, 이름이 아예 다른 같은 장소. TourAPI 제목 → 데이터랩 이름.
 * 확실한 것만 적는다(애매한 건 틀린 추천이 되느니 빼는 게 낫다).
 */
const ALIASES = {
  '넥슨컴퓨터박물관': '넥슨뮤지엄',
  '유민 아르누보 뮤지엄': '유민미술관',
  '제주 항파두리 항몽 유적': '항몽유적지',
  '도치돌 알파카목장': '도치돌목장',
  '우도산호해변 홍조단괴 서빈백사': '산호해수욕장',
  '대포주상절리': '주상절리대',
  '거슨새미오름': '거슨세미오름',
  '낙타트레킹': '낙타트래킹',
  '제주 무지개해안도로': '도두동무지개해안도로',
  '평대해변': '평대리해수욕장',
  '서우봉둘레길': '서우봉',
  '우도천진항': '천진항',
  '천주교 대정성지': '대정성지',
}

/** 규칙상 이어지지만 실제로는 다른 곳이라 막아둔 데이터랩 이름. */
const NOT_MATCHED = new Set([
  '렛츠런파크/제주', // → "말 테마파크 골프장 (렛츠런파크 제주)"는 골프장이다
])

/**
 * 한쪽 이름이 다른 쪽 이름 + 이 꼬리말이면 같은 곳으로 본다("노리매" / "노리매공원").
 * 꼬리말을 넓게 잡을수록 엉뚱한 곳이 이어지니, 장소 성격이 바뀌지 않는 것만.
 * ("둘레길"은 뺐다 — "한라산둘레길"이 산 자체인 "한라산"으로 이어져 버린다.)
 */
const SAME_PLACE_SUFFIXES = ['공원', '테마공원', '해안', '해안도로', '도로', '국립공원', '테마파크', '계곡']

function loadKey() {
  const env = readFileSync(resolve(ROOT, '.env.local'), 'utf8')
  const line = env.split(/\r?\n/).find((l) => l.startsWith('TOUR_API_SERVICE_KEY='))
  const key = line?.slice('TOUR_API_SERVICE_KEY='.length).trim()
  if (!key) throw new Error('.env.local 에 TOUR_API_SERVICE_KEY 가 없습니다.')
  return key
}
const KEY = loadKey()

async function getJson(service, op, params) {
  const q = new URLSearchParams({
    ...params,
    serviceKey: KEY,
    MobileOS: 'ETC',
    MobileApp: 'ColoringJeju',
    _type: 'json',
  })
  const res = await fetch(`https://apis.data.go.kr/B551011/${service}/${op}?${q}`)
  const text = await res.text()
  let json
  try {
    json = JSON.parse(text)
  } catch {
    throw new Error(`${service}/${op} 응답이 JSON 이 아닙니다: ${text.slice(0, 200)}`)
  }
  const code = json?.response?.header?.resultCode
  if (code !== '0000') throw new Error(`${service}/${op} 실패: ${JSON.stringify(json).slice(0, 200)}`)
  const body = json.response.body
  const item = body.items && typeof body.items === 'object' ? body.items.item : null
  return { total: Number(body.totalCount) || 0, items: Array.isArray(item) ? item : item ? [item] : [] }
}

/** 페이지를 끝까지 넘기며 전부 받는다. */
async function getAll(service, op, params) {
  const rows = []
  for (let page = 1; ; page++) {
    const { total, items } = await getJson(service, op, { ...params, numOfRows: '1000', pageNo: String(page) })
    rows.push(...items)
    if (items.length === 0 || rows.length >= total) return rows
  }
}

function recentMonths(count) {
  const now = new Date()
  return Array.from({ length: count }, (_, i) => {
    const d = new Date(now.getFullYear(), now.getMonth() - 1 - i, 1)
    return `${d.getFullYear()}${String(d.getMonth() + 1).padStart(2, '0')}`
  })
}

const toHttps = (url) => (url ? url.replace(/^http:\/\//, 'https://') : null)

/* ───────────── 이름 맞추기 ───────────── */

/** "제주 서귀포 산방산"처럼 여러 겹 붙는 지역 접두어. 벗겨낸 이름도 비교 후보로 쓴다. */
const PREFIXES = ['제주특별자치도', '제주특별자치', '제주시', '서귀포시', '제주', '서귀포']

function withoutPrefixes(k) {
  const out = [k]
  for (let i = 0; i < out.length; i++) {
    for (const p of PREFIXES) {
      const rest = out[i].slice(p.length)
      if (out[i].startsWith(p) && rest.length >= 2 && !out.includes(rest)) out.push(rest)
    }
  }
  return out
}

/** 비교용으로 다듬은 이름 — 공백·문장부호를 없애고, "해수욕장"은 "해변"으로 통일한다. */
function squash(s) {
  return s
    .toLowerCase()
    .replace(/[\s·・.:/,&'’\-_~]/g, '')
    .replace(/해수욕장$/, '해변')
    .replace(/(.)리해변$/, '$1해변')
}

/**
 * 한 장소가 가질 수 있는 비교용 이름들.
 * primary: 본 이름에서 나온 것 / alt: 괄호 속 다른 이름("종달항(두문포항)")에서 나온 것.
 */
function nameKeys(raw) {
  const primary = new Set()
  const alt = new Set()
  const bracketed = [...raw.matchAll(/[[(（【](.*?)[\])）】]/g)].map((m) => m[1])
  const main = raw.replace(/[[(（【].*?[\])）】]/g, '').trim()
  const add = (set, s) => {
    // "아쿠아플라넷/제주"는 통째로, "한라산둘레길/사려니숲길"은 조각마다 후보로 — 다만 "제주"나
    // "성읍점" 같은 지점 표시는 그 자체로 장소 이름이 아니니 조각 후보에서 뺀다.
    const parts = s.split('/').filter((p) => !/^(제주|.*점)$/.test(p.trim()))
    for (const part of [s, ...parts]) {
      const k = squash(part)
      if (k.length < 2) continue
      for (const v of withoutPrefixes(k)) {
        set.add(v)
        if (v.endsWith('제주') && v.length > 4) set.add(v.slice(0, -2))
      }
    }
  }
  add(primary, main)
  for (const b of bracketed) add(alt, b)
  return { primary, alt }
}

function suffixMatch(a, b) {
  const [short, long] = a.length <= b.length ? [a, b] : [b, a]
  if (short.length < 2 || !long.startsWith(short)) return false
  return SAME_PLACE_SUFFIXES.includes(long.slice(short.length))
}

/* ───────────── 받기 ───────────── */

console.log('데이터 있는 최신 달 찾는 중…')
let baseYm = null
for (const ym of recentMonths(6)) {
  const { total } = await getJson('TarRlteTarService1', 'areaBasedList1', {
    numOfRows: '1',
    pageNo: '1',
    baseYm: ym,
    areaCd: '50',
    signguCd: SIGNGU_CDS[0],
  })
  if (total > 0) {
    baseYm = ym
    break
  }
}
if (!baseYm) throw new Error('최근 6개월 안에 연관 관광지 데이터가 없습니다.')
console.log(`  → ${baseYm}`)

const rlteRows = []
for (const signguCd of SIGNGU_CDS) {
  rlteRows.push(
    ...(await getAll('TarRlteTarService1', 'areaBasedList1', { baseYm, areaCd: '50', signguCd })),
  )
}

const tourPlaces = []
for (const contentTypeId of TYPE_IDS) {
  // KorService2 는 옛 areaCode 가 빈 장소가 많다 — 법정동 코드(lDongRegnCd)로 받아야 제주 전체가 온다.
  const items = await getAll('KorService2', 'areaBasedList2', { lDongRegnCd: '50', contentTypeId })
  for (const i of items) {
    const lat = Number(i.mapy)
    const lng = Number(i.mapx)
    if (!lat || !lng) continue // 좌표가 없으면 앱이 지도에 못 올려서 담을 수도 없다.
    tourPlaces.push({
      contentId: i.contentid,
      contentTypeId: i.contenttypeid,
      title: i.title,
      addr1: i.addr1 ?? '',
      lat,
      lng,
      image: toHttps(i.firstimage),
      thumbnail: toHttps(i.firstimage2),
    })
  }
}
console.log(`데이터랩 ${rlteRows.length}줄, TourAPI 제주 관광지 ${tourPlaces.length}곳`)

/* ───────────── 잇기 ───────────── */

const byPrimary = new Map()
const byAlt = new Map()
const push = (map, k, v) => map.set(k, [...(map.get(k) ?? []), v])
for (const p of tourPlaces) {
  const { primary, alt } = nameKeys(p.title)
  const aliased = ALIASES[p.title]
  if (aliased) for (const k of nameKeys(aliased).primary) primary.add(k)
  p.keys = primary
  for (const k of primary) push(byPrimary, k, p)
  for (const k of alt) push(byAlt, k, p)
}

/** 같은 곳이 여러 번(유형만 다르게) 올라온 경우 대표 하나 — 관광지(12) > 사진 있음 > 작은 ID. */
function best(places) {
  return [...places].sort(
    (a, b) =>
      (a.contentTypeId === '12' ? 0 : 1) - (b.contentTypeId === '12' ? 0 : 1) ||
      (a.image ? 0 : 1) - (b.image ? 0 : 1) ||
      Number(a.contentId) - Number(b.contentId),
  )
}

const suffixLog = []
const fuzzyLog = []
const plain = (s) => squash(s.replace(/[[(（【].*?[\])）】]/g, ''))
/** 데이터랩 이름 → 이어지는 TourAPI 장소들(없으면 빈 배열). */
function matchTour(name) {
  if (NOT_MATCHED.has(name)) return []
  const { primary } = nameKeys(name)
  const exact = new Set()
  for (const k of primary) for (const p of byPrimary.get(k) ?? []) exact.add(p)
  if (exact.size) {
    const list = best(exact)
    if (plain(list[0].title) !== plain(name)) fuzzyLog.push(`${name} ↔ ${list[0].title}`)
    return list
  }
  // 괄호 속 이름은 겹치는 게 많아(예: "제주도 국가지질공원") 딱 한 곳일 때만 믿는다.
  for (const k of primary) {
    const hits = byAlt.get(k) ?? []
    if (hits.length === 1) {
      fuzzyLog.push(`${name} ↔ ${hits[0].title}  (괄호 속 이름)`)
      return hits
    }
  }
  const loose = new Set()
  for (const k of primary) for (const p of tourPlaces) if ([...p.keys].some((pk) => suffixMatch(k, pk))) loose.add(p)
  if (loose.size) suffixLog.push(`${name} ↔ ${[...loose].map((p) => p.title).join(' / ')}`)
  return best(loose)
}

const isAttraction = (r) =>
  r.rlteCtgryLclsNm === '관광지' && r.rlteCtgryMclsNm !== '쇼핑' && !r.rlteCtgrySclsNm.includes('교통')

const rowsByBase = new Map()
for (const r of rlteRows) push(rowsByBase, r.tAtsNm, r)

const matchCache = new Map()
const matchOnce = (name) => {
  if (!matchCache.has(name)) matchCache.set(name, matchTour(name))
  return matchCache.get(name)
}

const related = {}
/** 어느 기준 장소의 목록을 붙였는지 — 이름이 정확히 같은 기준 장소가 우선이다. */
const relatedFromExact = {}
const unmatchedBases = []
let basesWithAttractions = 0

for (const [baseName, rows] of rowsByBase) {
  const goodRows = rows.filter(isAttraction).sort((a, b) => Number(a.rlteRank) - Number(b.rlteRank))
  const baseIsAttraction = rlteRows.some((r) => r.rlteTatsNm === baseName && isAttraction(r))
  if (baseIsAttraction) basesWithAttractions++
  const basePlaces = matchOnce(baseName)
  if (basePlaces.length === 0) {
    if (baseIsAttraction) unmatchedBases.push(baseName)
    continue
  }
  const baseIds = new Set(basePlaces.map((p) => p.contentId))

  const list = []
  for (const r of goodRows) {
    const target = matchOnce(r.rlteTatsNm)[0]
    if (!target || baseIds.has(target.contentId) || list.includes(target.contentId)) continue
    list.push(target.contentId)
    // 분류·지역은 추천받는 장소 자체의 속성이라 장소 쪽에 한 번만 적어둔다.
    target.category ??= r.rlteCtgrySclsNm.replace(/\(.*?\)/g, '').trim()
    target.region ??= r.rlteSignguNm
    if (list.length >= TOP_N) break
  }
  if (list.length === 0) continue

  // "제주항/제4부두", "제주항/제6부두"… 처럼 여러 기준 장소가 한 곳으로 이어질 수 있다.
  // 이름이 딱 같은 기준 장소가 있으면 그걸 쓰고, 아니면 먼저 붙은 걸 그대로 둔다.
  for (const p of basePlaces) {
    const exact = plain(p.title) === plain(baseName)
    if (related[p.contentId] && (relatedFromExact[p.contentId] || !exact)) continue
    related[p.contentId] = list
    relatedFromExact[p.contentId] = exact
  }
}

// 최종 목록에 실제로 남은 장소만 싣는다(덮어써진 목록의 장소는 빠진다).
const placesOut = {}
for (const list of Object.values(related)) {
  for (const id of list) {
    const p = tourPlaces.find((t) => t.contentId === id)
    placesOut[id] = {
      contentTypeId: p.contentTypeId,
      title: p.title,
      addr1: p.addr1,
      lat: p.lat,
      lng: p.lng,
      image: p.image,
      thumbnail: p.thumbnail,
      category: p.category,
      region: p.region,
    }
  }
}

writeFileSync(
  OUT,
  JSON.stringify({ baseYm, places: placesOut, related }, null, 0) + '\n',
)

/* ───────────── 요약 ───────────── */

const byType = {}
for (const p of tourPlaces) byType[p.contentTypeId] = (byType[p.contentTypeId] ?? 0) + 1
const withSheet = tourPlaces.filter((p) => related[p.contentId])
const withSheetByType = {}
for (const p of withSheet) withSheetByType[p.contentTypeId] = (withSheetByType[p.contentTypeId] ?? 0) + 1

console.log(`\n저장: ${OUT}`)
console.log(`\n앱 관광지(TourAPI 제주) 중 추천 시트가 뜰 수 있는 곳: ${withSheet.length} / ${tourPlaces.length}`)
for (const t of TYPE_IDS) console.log(`  type ${t}: ${withSheetByType[t] ?? 0} / ${byType[t] ?? 0}`)
console.log(`\n데이터랩 기준 장소 ${rowsByBase.size}곳 (그중 관광지로 확인된 곳 ${basesWithAttractions})`)
console.log(`관광지인데 앱 관광지와 못 이은 곳 ${unmatchedBases.length}: ${unmatchedBases.join(', ')}`)
console.log(`\n[이름이 달라도 같은 곳으로 이은 것 — 틀린 게 없는지 확인]`)
for (const l of fuzzyLog) console.log(`  ${l}`)
console.log(`\n[꼬리말 규칙으로 이은 것]`)
for (const l of suffixLog) console.log(`  ${l}`)
