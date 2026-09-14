import {
  arrayRemove,
  collection,
  deleteDoc,
  deleteField,
  doc,
  getDoc,
  getDocs,
  limit,
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
import type { Trip, TripKind, TripSpot } from '../types'

/**
 * 여행(앨범) — 지도 핀·스탬프·조각모음이 전부 이 단위로 묶인다.
 *
 * `groups/{code}` (안드로이드 앱과 공유) 와는 완전히 별개인 웹 전용 컬렉션이다.
 *
 *   trips/{tripId}
 *     name, kind('personal'|'group'), startDate, endDate, ownerUid,
 *     memberUids: string[], members: { [uid]: name }, inviteCode(group 만), createdAt
 *     spots/{contentId}
 *       contentId, title, image, category, lat, lng, addedAt, addedByUid,
 *       headline, description, verifiedColor, photo
 */

const tripsRef = () => collection(db, 'trips')
const spotsRef = (tripId: string) => collection(db, 'trips', tripId, 'spots')

/** 읽어주기 좋은 초대 코드. 0/O, 1/I 를 빼서 말로 불러주거나 받아 적을 때 헷갈리지 않게 한다. */
function randomCode(): string {
  const chars = 'ABCDEFGHJKLMNPQRSTUVWXYZ23456789'
  return Array.from({ length: 6 }, () => chars[Math.floor(Math.random() * chars.length)]).join('')
}

function toTrip(snap: QueryDocumentSnapshot<DocumentData>): Trip {
  const d = snap.data()
  return {
    id: snap.id,
    name: d.name ?? '',
    kind: d.kind === 'group' ? 'group' : 'personal',
    startDate: d.startDate ?? '',
    endDate: d.endDate ?? '',
    ownerUid: d.ownerUid ?? '',
    memberUids: Array.isArray(d.memberUids) ? d.memberUids : [],
    members: d.members ?? {},
    inviteCode: d.inviteCode ?? null,
    // serverTimestamp 는 쓰기 직후 잠깐 null 로 읽힌다 — 정렬이 튀지 않게 0으로 받는다.
    createdAt: d.createdAt?.toMillis?.() ?? 0,
  }
}

function toTripSpot(snap: QueryDocumentSnapshot<DocumentData>): TripSpot {
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
    verifiedColor: d.verifiedColor ?? null,
    photo: d.photo ?? null,
    caption: d.caption ?? null,
    verifiedAt: d.verifiedAt ?? null,
  }
}

export type TripStatus = 'upcoming' | 'ongoing' | 'past'

/** 오늘 날짜 기준으로 진행중/예정/다녀옴을 가른다. 날짜만 비교하므로 문자열 비교로 충분하다. */
export function tripStatus(trip: Trip): TripStatus {
  const today = new Date().toISOString().slice(0, 10)
  if (today < trip.startDate) return 'upcoming'
  if (today > trip.endDate) return 'past'
  return 'ongoing'
}

export async function createTrip(input: {
  name: string
  kind: TripKind
  startDate: string
  endDate: string
  ownerUid: string
  ownerName: string
}): Promise<Trip> {
  const ref = doc(tripsRef())
  const inviteCode = input.kind === 'group' ? await uniqueInviteCode() : null
  const data = {
    name: input.name.trim(),
    kind: input.kind,
    startDate: input.startDate,
    endDate: input.endDate,
    ownerUid: input.ownerUid,
    memberUids: [input.ownerUid],
    members: { [input.ownerUid]: input.ownerName },
    inviteCode,
    createdAt: serverTimestamp(),
  }
  await setDoc(ref, data)
  return {
    id: ref.id,
    name: data.name,
    kind: data.kind,
    startDate: data.startDate,
    endDate: data.endDate,
    ownerUid: data.ownerUid,
    memberUids: data.memberUids,
    members: data.members,
    inviteCode: data.inviteCode,
    createdAt: Date.now(),
  }
}

/** 6자리 조합이 10억 가지라 충돌은 드물지만, 겹치면 남의 여행에 잘못 들어가므로 빈 코드를 찾아 쓴다. */
async function uniqueInviteCode(): Promise<string> {
  for (let attempt = 0; attempt < 5; attempt++) {
    const code = randomCode()
    const q = query(tripsRef(), where('inviteCode', '==', code), limit(1))
    const existing = await getDocs(q)
    if (existing.empty) return code
  }
  throw new Error('초대 코드를 만들지 못했어요. 다시 시도해주세요.')
}

export async function joinTripByCode(code: string, uid: string, displayName: string): Promise<Trip> {
  const normalized = code.trim().toUpperCase()
  const q = query(tripsRef(), where('inviteCode', '==', normalized), limit(1))
  const found = await getDocs(q)
  if (found.empty) throw new Error('그 코드의 여행을 찾지 못했어요.')
  const snap = found.docs[0]
  await updateDoc(snap.ref, {
    memberUids: [...new Set([...(snap.data().memberUids ?? []), uid])],
    [`members.${uid}`]: displayName,
  })
  return toTrip(snap)
}

/**
 * 여행에서 나간다. 개인 여행은 멤버가 소유자 1명뿐이라 사실상 삭제와 같다 — 그래서 소유자면
 * 문서 자체를 지우고, 그룹 여행에서 멤버가 나가는 경우엔 목록에서만 뺀다.
 */
export async function leaveTrip(tripId: string, uid: string) {
  const ref = doc(tripsRef(), tripId)
  const snap = await getDoc(ref)
  if (snap.exists() && snap.data().ownerUid === uid) {
    await deleteDoc(ref)
    return
  }
  await updateDoc(ref, {
    memberUids: arrayRemove(uid),
    [`members.${uid}`]: deleteField(),
  })
}

/** 내가 속한 여행을 실시간으로 구독한다. */
export function watchMyTrips(uid: string, onChange: (trips: Trip[]) => void) {
  const q = query(tripsRef(), where('memberUids', 'array-contains', uid))
  return onSnapshot(q, (snap) => onChange(snap.docs.map(toTrip)))
}

/** 여행 하나의 장소 목록을 실시간으로 구독한다 — 누가 담거나 인증하면 바로 반영된다. */
export function watchTripSpots(tripId: string, onChange: (spots: TripSpot[]) => void) {
  return onSnapshot(spotsRef(tripId), (snap) => onChange(snap.docs.map(toTripSpot)))
}

/**
 * 여러 여행의 장소를 한 번에 구독한다 (마이페이지 통계·조각모음 앨범뷰용).
 * tripIds 가 바뀌면 이전 구독을 정리하고 새로 건다.
 */
export function watchAllTripSpots(
  tripIds: string[],
  onChange: (spotsByTrip: Record<string, TripSpot[]>) => void,
) {
  const state: Record<string, TripSpot[]> = {}
  const unsubs = tripIds.map((tripId) =>
    watchTripSpots(tripId, (spots) => {
      state[tripId] = spots
      onChange({ ...state })
    }),
  )
  return () => unsubs.forEach((u) => u())
}

export async function addTripSpot(tripId: string, spot: TripSpot) {
  await setDoc(doc(spotsRef(tripId), spot.contentId), {
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
    photo: spot.photo,
    caption: spot.caption,
    verifiedAt: spot.verifiedAt,
  })
}

export async function removeTripSpot(tripId: string, contentId: string) {
  await deleteDoc(doc(spotsRef(tripId), contentId))
}

/**
 * 인증 — 색·사진·문구·시각을 한 번에 기록한다. 스탬프 완료, 지도 마커 컬러화, 무지개·조각모음
 * 타임라인이 전부 이 값에서 파생된다.
 */
export async function markTripSpotVerified(
  tripId: string,
  contentId: string,
  color: string,
  photo: string,
  caption: string | null,
) {
  await updateDoc(doc(spotsRef(tripId), contentId), {
    verifiedColor: color,
    photo,
    caption,
    verifiedAt: Date.now(),
  })
}
