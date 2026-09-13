package com.example.coloringjeju.core.group.model

/**
 * A shared trip group other members join by [code] — [code] doubles as the Firestore document id
 * itself (see [com.example.coloringjeju.core.group.GroupRepository]), so joining is a plain
 * document lookup rather than a query.
 *
 * [members] mirrors [memberUids] as uid → display name (or email, snapshotted at join time), since
 * the client SDK can't look another user's profile up by uid — [com.example.coloringjeju.presentation.Group.GroupDetailScreen]
 * reads names from here instead.
 */
data class TravelGroup(
    val code: String,
    val name: String,
    val ownerUid: String,
    val memberUids: List<String> = emptyList(),
    val members: Map<String, String> = emptyMap(),
    val createdAt: Long = 0L,
)
