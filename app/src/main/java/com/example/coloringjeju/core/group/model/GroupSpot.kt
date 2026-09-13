package com.example.coloringjeju.core.group.model

import com.example.coloringjeju.core.local.datastore.SavedSpot

/**
 * One place on a [TravelGroup]'s shared map — the same shape as
 * [com.example.coloringjeju.core.local.datastore.SavedSpot] plus [addedByUid], since it lives in
 * Firestore instead of on-device and more than one traveler can see/verify it.
 */
data class GroupSpot(
    val contentId: String,
    val title: String,
    val image: String?,
    val category: String,
    val lat: Double,
    val lng: Double,
    val addedAt: Long,
    val addedByUid: String,
    val headline: String = "",
    val description: String? = null,
    val verifiedColor: Int? = null,
) {
    val isVerified: Boolean get() = verifiedColor != null
}

/**
 * Reuses every screen that already renders a [SavedSpot] (map pins, 스탬프 rows, the detail sheet)
 * for a group spot too — only who gets to write it back (MY 지도's local store vs.
 * [com.example.coloringjeju.core.group.GroupRepository]) differs, not how it's displayed.
 */
fun GroupSpot.toSavedSpot() = SavedSpot(
    contentId = contentId,
    title = title,
    image = image,
    category = category,
    lat = lat,
    lng = lng,
    addedAt = addedAt,
    headline = headline,
    description = description,
    verifiedColor = verifiedColor,
)

/** The reverse of [toSavedSpot] — pushing a MY 지도 (or 추천 지도) place onto a group's shared map. */
fun SavedSpot.toGroupSpot(addedByUid: String) = GroupSpot(
    contentId = contentId,
    title = title,
    image = image,
    category = category,
    lat = lat,
    lng = lng,
    addedAt = addedAt,
    addedByUid = addedByUid,
    headline = headline,
    description = description,
    verifiedColor = verifiedColor,
)
