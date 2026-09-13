package com.example.coloringjeju.core.group

import com.example.coloringjeju.core.group.model.GroupSpot
import com.example.coloringjeju.core.group.model.TravelGroup
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlin.random.Random

/** Result of a Firestore group operation, with the failure message already ready to show. */
sealed interface GroupResult<out T> {
    data class Success<T>(val data: T) : GroupResult<T>
    data class Error(val message: String) : GroupResult<Nothing>
}

/**
 * Firestore-backed 그룹 지도 — the one place cross-device group data goes through, mirroring how
 * [com.example.coloringjeju.core.local.datastore.SavedSpotsStore] is the one place MY 지도 goes
 * through. `groups/{code}` is the group itself, its own document id doubling as the shareable
 * invite code (so joining is a plain doc lookup, no query needed); `groups/{code}/spots/{contentId}`
 * is its shared map, one [GroupSpot] per document.
 *
 * Requires Cloud Firestore to be enabled on the Firebase project with security rules that let a
 * signed-in user read any group (to preview one before joining by code), but only a member read/write
 * its `spots` subcollection, and only touch `memberUids`/`members` on update — see the rules shared
 * alongside this feature.
 */
object GroupRepository {
    private val db get() = FirebaseFirestore.getInstance()
    private fun groupsRef() = db.collection("groups")
    private fun spotsRef(code: String) = groupsRef().document(code).collection("spots")

    /** Live list of groups [uid] belongs to, newest-created first. */
    fun myGroups(uid: String): Flow<List<TravelGroup>> = callbackFlow {
        val registration = groupsRef()
            .whereArrayContains("memberUids", uid)
            .addSnapshotListener { snapshot, _ ->
                trySend(snapshot?.documents.orEmpty().mapNotNull { it.toTravelGroup() }.sortedByDescending { it.createdAt })
            }
        awaitClose { registration.remove() }
    }

    /** Live shared map for one group, newest-added first. */
    fun groupSpots(code: String): Flow<List<GroupSpot>> = callbackFlow {
        val registration = spotsRef(code).addSnapshotListener { snapshot, _ ->
            trySend(snapshot?.documents.orEmpty().mapNotNull { it.toGroupSpot() }.sortedByDescending { it.addedAt })
        }
        awaitClose { registration.remove() }
    }

    suspend fun createGroup(name: String, ownerUid: String, ownerName: String): GroupResult<TravelGroup> = runCatching {
        var created: TravelGroup? = null
        // A freshly-rolled 6-char code colliding with an existing group is very unlikely — a
        // handful of retries is just cheap insurance, not something that should ever actually loop.
        for (attempt in 1..5) {
            val code = randomCode()
            val doc = groupsRef().document(code)
            if (!doc.get().await().exists()) {
                val group = TravelGroup(
                    code = code,
                    name = name,
                    ownerUid = ownerUid,
                    memberUids = listOf(ownerUid),
                    members = mapOf(ownerUid to ownerName),
                    createdAt = System.currentTimeMillis(),
                )
                doc.set(group.toMap()).await()
                created = group
                break
            }
        }
        created ?: error("코드 생성에 실패했어요. 다시 시도해주세요.")
    }.fold(onSuccess = { GroupResult.Success(it) }, onFailure = { GroupResult.Error(it.toGroupErrorMessage()) })

    suspend fun joinGroup(code: String, uid: String, displayName: String): GroupResult<TravelGroup> = runCatching {
        val doc = groupsRef().document(code.trim().uppercase())
        val snapshot = doc.get().await()
        if (!snapshot.exists()) error("존재하지 않는 그룹 코드예요.")
        val group = snapshot.toTravelGroup() ?: error("그룹 정보를 불러오지 못했어요.")
        if (uid !in group.memberUids) {
            doc.update(mapOf("memberUids" to FieldValue.arrayUnion(uid), "members.$uid" to displayName)).await()
        }
        group.copy(memberUids = group.memberUids + uid, members = group.members + (uid to displayName))
    }.fold(onSuccess = { GroupResult.Success(it) }, onFailure = { GroupResult.Error(it.toGroupErrorMessage()) })

    suspend fun leaveGroup(code: String, uid: String): GroupResult<Unit> = runCatching {
        groupsRef().document(code)
            .update(mapOf("memberUids" to FieldValue.arrayRemove(uid), "members.$uid" to FieldValue.delete()))
            .await()
    }.fold(onSuccess = { GroupResult.Success(Unit) }, onFailure = { GroupResult.Error(it.toGroupErrorMessage()) })

    suspend fun addSpot(code: String, spot: GroupSpot): GroupResult<Unit> = runCatching {
        spotsRef(code).document(spot.contentId).set(spot.toMap()).await()
    }.fold(onSuccess = { GroupResult.Success(Unit) }, onFailure = { GroupResult.Error(it.toGroupErrorMessage()) })

    suspend fun removeSpot(code: String, contentId: String): GroupResult<Unit> = runCatching {
        spotsRef(code).document(contentId).delete().await()
    }.fold(onSuccess = { GroupResult.Success(Unit) }, onFailure = { GroupResult.Error(it.toGroupErrorMessage()) })

    suspend fun markSpotVerified(code: String, contentId: String, colorArgb: Int): GroupResult<Unit> = runCatching {
        spotsRef(code).document(contentId).update("verifiedColor", colorArgb).await()
    }.fold(onSuccess = { GroupResult.Success(Unit) }, onFailure = { GroupResult.Error(it.toGroupErrorMessage()) })

    private fun randomCode(): String {
        val chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789" // no 0/O/1/I — easy to read aloud and retype
        return (1..6).map { chars.random(Random) }.joinToString("")
    }

    private fun Throwable.toGroupErrorMessage(): String = message ?: "그룹 처리 중 오류가 발생했어요."

    private fun TravelGroup.toMap(): Map<String, Any?> = mapOf(
        "name" to name,
        "ownerUid" to ownerUid,
        "memberUids" to memberUids,
        "members" to members,
        "createdAt" to createdAt,
    )

    private fun DocumentSnapshot.toTravelGroup(): TravelGroup? = runCatching {
        TravelGroup(
            code = id,
            name = getString("name") ?: return null,
            ownerUid = getString("ownerUid") ?: "",
            memberUids = (get("memberUids") as? List<*>)?.mapNotNull { it as? String } ?: emptyList(),
            members = (get("members") as? Map<*, *>)?.entries
                ?.mapNotNull { (k, v) -> (k as? String)?.let { key -> (v as? String)?.let { value -> key to value } } }
                ?.toMap() ?: emptyMap(),
            createdAt = getLong("createdAt") ?: 0L,
        )
    }.getOrNull()

    private fun GroupSpot.toMap(): Map<String, Any?> = mapOf(
        "contentId" to contentId,
        "title" to title,
        "image" to image,
        "category" to category,
        "lat" to lat,
        "lng" to lng,
        "addedAt" to addedAt,
        "addedByUid" to addedByUid,
        "headline" to headline,
        "description" to description,
        "verifiedColor" to verifiedColor,
    )

    private fun DocumentSnapshot.toGroupSpot(): GroupSpot? = runCatching {
        GroupSpot(
            contentId = getString("contentId") ?: id,
            title = getString("title") ?: return null,
            image = getString("image"),
            category = getString("category") ?: "",
            lat = getDouble("lat") ?: 0.0,
            lng = getDouble("lng") ?: 0.0,
            addedAt = getLong("addedAt") ?: 0L,
            addedByUid = getString("addedByUid") ?: "",
            headline = getString("headline") ?: "",
            description = getString("description"),
            verifiedColor = getLong("verifiedColor")?.toInt(),
        )
    }.getOrNull()
}
