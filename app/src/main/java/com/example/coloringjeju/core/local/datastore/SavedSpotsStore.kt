package com.example.coloringjeju.core.local.datastore

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

/** One spot the traveler saved to MY 지도 — there's no backend/account yet, so this is the only record. */
data class SavedSpot(
    val contentId: String,
    val title: String,
    val image: String?,
    val category: String,
    val lat: Double,
    val lng: Double,
    val addedAt: Long,
)

/**
 * Local, on-device "MY 지도" store — a small JSON array in [android.content.SharedPreferences],
 * keyed by [SavedSpot.contentId]. Plain reads/writes (no coroutines): the list is tiny and this
 * only ever touches disk on a save/remove tap, not on every recomposition.
 */
class SavedSpotsStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getAll(): List<SavedSpot> {
        val raw = prefs.getString(KEY_SPOTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getJSONObject(it).toSavedSpot() }
        }.getOrDefault(emptyList())
    }

    fun isSaved(contentId: String): Boolean = getAll().any { it.contentId == contentId }

    fun add(spot: SavedSpot) {
        val updated = getAll().filterNot { it.contentId == spot.contentId } + spot
        persist(updated)
    }

    fun remove(contentId: String) {
        persist(getAll().filterNot { it.contentId == contentId })
    }

    private fun persist(spots: List<SavedSpot>) {
        val array = JSONArray()
        spots.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SPOTS, array.toString()).apply()
    }

    private fun JSONObject.toSavedSpot() = SavedSpot(
        contentId = getString("contentId"),
        title = getString("title"),
        image = optString("image").ifBlank { null },
        category = getString("category"),
        lat = getDouble("lat"),
        lng = getDouble("lng"),
        addedAt = optLong("addedAt"),
    )

    private fun SavedSpot.toJson() = JSONObject().apply {
        put("contentId", contentId)
        put("title", title)
        put("image", image ?: "")
        put("category", category)
        put("lat", lat)
        put("lng", lng)
        put("addedAt", addedAt)
    }

    private companion object {
        const val PREFS_NAME = "saved_spots"
        const val KEY_SPOTS = "spots"
    }
}
