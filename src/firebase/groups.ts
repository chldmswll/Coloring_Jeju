import {
  arrayRemove,
  arrayUnion,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  onSnapshot,
  query,
  serverTimestamp,
  setDoc,
  updateDoc,
  where,
  type DocumentData,
  type QueryDocumentSnapshot,
} from 'firebase/firestore'
import { db } from './app'

/**
 * 그룹 지도 — 여러 사람이 같은 목록을 보고 같이 채우는 곳.
 *
 * 안드로이드의 GroupRepository 와 **완전히 같은 Firestore 구조**를 쓴다. 그래서 앱에서 만든
 * 그룹이 웹에도 그대로 보이고, 반대도 마찬가지다.
 *
 *   groups/{code}
 *     name, ownerUid, memberUids: string[], members: { [uid]: name }, createdAt
 *     spots/{contentId}
 *       contentId, title, image, category, lat, lng, addedAt, addedByUid,
 *       headline, description, verifiedColor
 *
 * ⚠️ verifiedColor 형식이 두 클라이언트에서 다르다. 안드로이드는 ARGB 정수(-1543350),
 * 웹은 "#e8734a" 문자열. 어느 쪽이 썼는지 모르므로 읽을 때 양쪽을 모두 받아 문자열로 맞춘다.
 * 쓸 때는 항상 문자열로 쓴다 — 사람이 읽을 수 있고 CSS 에 그대로 넣을 수 있어서다.
 */

export interface TravelGroup {
  code: string
  name: string
  ownerUid: string
  memberUids: string[]
  members: Record<string, string>
  createdAt: number
}

export interface GroupSpot {
  contentId: string
  title: string
  image: string | null
  category: string
  lat: number
  lng: number
  addedAt: number
  addedByUid: string
  headline: string
  description: string | null
  verifiedColor: string | null
}

const groupsRef = () => collection(db, 'groups')
const spotsRef = (code: string) => collection(db, 'groups', code, 'spots')

/** 안드로이드가 남긴 ARGB 정수도, 웹이 남긴 "#rrggbb" 도 모두 CSS 색 문자열로 맞춘다. */
function toCssColor(value: unknown): string | null {
  if (typeof value === 'string' && value.trim()) return value
  if (typeof value === 'number') {
    const rgb = (value >>> 0) & 0xffffff
    return '#' + rgb.toString(16).padStart(6, '0')
  }
  return null
}

function toGroup(snap: QueryDocumentSnapshot<DocumentData>): TravelGroup {
  const d = snap.data()
  return {
    code: snap.id,
    name: d.name ?? '',
    ownerUid: d.ownerUid ?? '',
    memberUids: Array.isArray(d.memberUids) ? d.memberUids : [],
    members: d.members ?? {},
    // serverTimestamp 는 쓰기 직후 잠깐 null 로 읽힌다 — 정렬이 튀지 않게 0으로 받는다.
    createdAt: d.createdAt?.toMillis?.() ?? 0,
  }
}

function toSpot(snap: QueryDocumentSnapshot<DocumentData>): GroupSpot {
  const d = snap.data()
  return {
    contentId: snap.id,
    title: d.title ?? '',
    image: d.image ?? null,
    category: d.category ?? '',
    lat: Number(d.lat),
    lng: Number(d.lng),
    addedAt: Number(d.addedAt) || 0,
    addedByUid: d.addedByUid ?? '',
    headline: d.headline ?? '',
    description: d.description ?? null,
    verifiedColor: toCssColor(d.verifiedColor),
  }
}

/** 내가 속한 그룹을 실시간으로 구독한다. */
export function watchMyGroups(uid: string, onChange: (groups: TravelGroup[]) => void) {
  const q = query(groupsRef(), where('memberUids', 'array-contains', uid))
  return onSnapshot(q, (snap) => onChange(snap.docs.map(toGroup)))
}

/** 그룹 하나의 장소 목록을 실시간으로 구독한다 — 누가 담거나 인증하면 바로 반영된다. */
export function watchGroupSpots(code: string, onChange: (spots: GroupSpot[]) => void) {
  return onSnapshot(spotsRef(code), (snap) => onChange(snap.docs.map(toSpot)))
}

/**
 * 읽어주기 좋은 초대 코드. 0/O, 1/I 를 빼서 말로 불러주거나 받아 적을 때 헷갈리지 않게 한다
 * (안드로이드와 같은 문자 집합).
 */
function randomCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  return Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

export async function createGroup(name: string, ownerUid: string, ownerName: string) {
  // 6자리 조합이 10억 가지라 충돌은 드물지만, 겹치면 남의 그룹을 덮어쓰므로 빈 코드를 찾아 쓴다.
  for (let attempt = 0; attempt < 5; attempt++) {
    const code = randomCode()
    const ref = doc(groupsRef(), code)
    if ((await getDoc(ref)).exists()) continue
    await setDoc(ref, {
      name: name.trim(),
      ownerUid,
      memberUids: [ownerUid],
      members: { [ownerUid]: ownerName },
      createdAt: serverTimestamp(),
    })
    return code
  }
  throw new Error('그룹 코드를 만들지 못했어요. 다시 시도해주세요.')
}

export async function joinGroup(code: string, uid: string, displayName: string) {
  const normalized = code.trim().toUpperCase()
  const ref = doc(groupsRef(), normalized)
  if (!(await getDoc(ref)).exists()) throw new Error('그 코드의 그룹을 찾지 못했어요.')
  await updateDoc(ref, {
    memberUids: arrayUnion(uid),
    [`members.${uid}`]: displayName,
  })
  return normalized
}

export async function leaveGroup(code: string, uid: string) {
  await updateDoc(doc(groupsRef(), code), {
    memberUids: arrayRemove(uid),
    [`members.${uid}`]: deleteField(),
  })
}

export async function addGroupSpot(code: string, spot: GroupSpot) {
  await setDoc(doc(spotsRef(code), spot.contentId), {
    contentId: spot.contentId,
    title: spot.title,
    image: spot.image,
    category: spot.category,
    lat: spot.lat,
    lng: spot.lng,
    addedAt: spot.addedAt,
    addedByUid: spot.addedByUid,
    headline: spot.headline,
    description: spot.description,
    verifiedColor: spot.verifiedColor,
  })
}

export async function removeGroupSpot(code: string, contentId: string) {
  await deleteDoc(doc(spotsRef(code), contentId))
}

/** 그룹 지도에서도 색을 기록한다 — MY 지도와 같은 의미의 "인증". */
export async function markGroupSpotVerified(code: string, contentId: string, color: string) {
  await updateDoc(doc(spotsRef(code), contentId), { verifiedColor: color })
}
