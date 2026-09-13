package com.example.coloringjeju.core.local.datastore

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * One spot the traveler saved to MY 지도 — there's no backend/account yet, so this is the only
 * record. Everything MY 지도 needs to draw the place lives here: [image] is the TourAPI photo shown
 * inside its map marker and at the top of its detail sheet, and [headline]/[description] back the
 * rest of that sheet (a place saved from 추천 지도 keeps its handwritten copy; one saved from
 * search leaves [description] null and the sheet fills it in from `detailCommon`).
 *
 * [verifiedColor] is the ARGB color the traveler captured at the place, or null while the color
 * mission is still unverified — which is exactly when the marker's photo is drawn in grayscale and
 * the place's 스탬프 row is still open to 인증하기.
 */
data class SavedSpot(
    val contentId: String,
    val title: String,
    val image: String?,
    val category: String,
    val lat: Double,
    val lng: Double,
    val addedAt: Long,
    val headline: String = "",
    val description: String? = null,
    val verifiedColor: Int? = null,
) {
    /** The color mission at this place is done. */
    val isVerified: Boolean get() = verifiedColor != null
}

/**
 * The one on-device record of MY 지도 — a small JSON array in [android.content.SharedPreferences],
 * keyed by [SavedSpot.contentId]. Reads/writes are plain and synchronous: the list is tiny and this
 * only ever touches disk on a save/remove/verify tap, not on every recomposition.
 *
 * **Every screen shares one instance** ([SavedSpotsStore.get]) and observes [spots], so MY 지도 and
 * 스탬프 are the same list by construction rather than by two screens agreeing to stay in sync:
 * adding a place makes its 스탬프 mission appear, and verifying that mission
 * ([markVerified]) colors its map marker — no tab switch or reload needed.
 *
 * Reads tolerate records written before [SavedSpot.headline]/[SavedSpot.description]/
 * [SavedSpot.verifiedColor] existed — those keys just come back as their defaults.
 */
class SavedSpotsStore private constructor(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _spots = MutableStateFlow(readFromDisk())

    /** MY 지도, newest-saved last. Collect this rather than calling [getAll] in a composable. */
    val spots: StateFlow<List<SavedSpot>> = _spots.asStateFlow()

    fun getAll(): List<SavedSpot> = _spots.value

    fun isSaved(contentId: String): Boolean = getAll().any { it.contentId == contentId }

    fun add(spot: SavedSpot) {
        // Keep whatever color was already earned here, so re-adding a place doesn't wipe its
        // verification and send the marker back to grayscale.
        val existingColor = getAll().firstOrNull { it.contentId == spot.contentId }?.verifiedColor
        persist(
            getAll().filterNot { it.contentId == spot.contentId } +
                spot.copy(verifiedColor = spot.verifiedColor ?: existingColor),
        )
    }

    fun remove(contentId: String) {
        persist(getAll().filterNot { it.contentId == contentId })
    }

    /**
     * Records the color captured at a place, flipping its 스탬프 row to 인증완료 and its map marker
     * from grayscale to full color. Matched on [SavedSpot.contentId] — the 스탬프 list is built
     * from these same records, so the id always lines up; a place that has since been removed from
     * MY 지도 is simply left alone.
     */
    fun markVerified(contentId: String, colorArgb: Int) {
        val all = getAll()
        if (all.none { it.contentId == contentId }) return
        persist(all.map { if (it.contentId == contentId) it.copy(verifiedColor = colorArgb) else it })
    }

    private fun persist(spots: List<SavedSpot>) {
        val array = JSONArray()
        spots.forEach { array.put(it.toJson()) }
        prefs.edit().putString(KEY_SPOTS, array.toString()).apply()
        _spots.value = spots
    }

    private fun readFromDisk(): List<SavedSpot> {
        val raw = prefs.getString(KEY_SPOTS, null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getJSONObject(it).toSavedSpot() }
        }.getOrDefault(emptyList())
    }

    private fun JSONObject.toSavedSpot() = SavedSpot(
        contentId = getString("contentId"),
        title = getString("title"),
        // Records saved before TourApiParser started upgrading image URLs still hold a plain
        // `http://` one, which Android blocks outright — heal them on the way out so an old save
        // isn't stuck with a photo that can never load.
        image = optString("image").ifBlank { null }?.toHttpsUrl(),
        category = getString("category"),
        lat = getDouble("lat"),
        lng = getDouble("lng"),
        addedAt = optLong("addedAt"),
        headline = optString("headline"),
        description = optString("description").ifBlank { null },
        verifiedColor = if (has("verifiedColor") && !isNull("verifiedColor")) getInt("verifiedColor") else null,
    )

    private fun SavedSpot.toJson() = JSONObject().apply {
        put("contentId", contentId)
        put("title", title)
        put("image", image ?: "")
        put("category", category)
        put("lat", lat)
        put("lng", lng)
        put("addedAt", addedAt)
        put("headline", headline)
        put("description", description ?: "")
        put("verifiedColor", verifiedColor ?: JSONObject.NULL)
    }

    private fun String.toHttpsUrl(): String =
        if (startsWith("http://")) "https://" + substring("http://".length) else this

    companion object {
        private const val PREFS_NAME = "saved_spots"
        private const val KEY_SPOTS = "spots"

        @Volatile
        private var instance: SavedSpotsStore? = null

        /** The process-wide store. Every screen must go through this — see the class doc. */
        fun get(context: Context): SavedSpotsStore =
            instance ?: synchronized(this) {
                instance ?: SavedSpotsStore(context).also { instance = it }
            }
    }
}
