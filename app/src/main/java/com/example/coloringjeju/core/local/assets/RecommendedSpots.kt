package com.example.coloringjeju.core.local.assets

import android.content.Context
import org.json.JSONArray

/**
 * One place on 추천 지도, as it sits in `assets/recommended_spots.json`.
 *
 * [rank] is 한국관광공사's own 중심관광지 ranking (`hubRank`) — how strongly this place connects to
 * other places in its 기초지자체, derived from TMAP navigation logs. It is why the place is on the
 * recommendation list at all, so it travels with the record rather than being recomputed here.
 */
data class RecommendedSpot(
    val contentId: String,
    val title: String,
    val category: String,
    val emoji: String,
    val lat: Double,
    val lng: Double,
    val imageUrl: String?,
    val headline: String,
    val description: String,
    val rank: Int,
    val signgu: String,
)

/**
 * 추천 지도's fixed place list, shipped as an asset rather than fetched at runtime.
 *
 * The list is built offline by joining two 한국관광공사 sources: 기초지자체 중심 관광지
 * (`LocgoHubTarService1`, which supplies the ranking and coordinates) against TourAPI KorService2
 * (which supplies `contentId`, the photo and the description). That join needs name matching and
 * hand-correction — roughly a third of the 중심관광지 have no clean TourAPI counterpart — so it is
 * done once at build time and committed, not attempted on device.
 *
 * Baking it in also keeps 추천 지도 instant and costs no API quota: the ranking data only changes
 * monthly, and the 중심관광지 API's 개발계정 allows 1,000 calls a day across all users.
 *
 * Read once and cached — the file is ~32 KB and parsing it on the first composition is cheaper than
 * carrying a loading state on the map's first frame.
 */
object RecommendedSpots {
    private const val ASSET_NAME = "recommended_spots.json"

    @Volatile
    private var cached: List<RecommendedSpot>? = null

    fun load(context: Context): List<RecommendedSpot> =
        cached ?: synchronized(this) { cached ?: read(context).also { cached = it } }

    private fun read(context: Context): List<RecommendedSpot> = runCatching {
        val raw = context.applicationContext.assets.open(ASSET_NAME)
            .bufferedReader()
            .use { it.readText() }
        val array = JSONArray(raw)
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            RecommendedSpot(
                contentId = o.getString("contentId"),
                title = o.getString("title"),
                category = o.optString("category"),
                emoji = o.optString("emoji"),
                lat = o.getDouble("lat"),
                lng = o.getDouble("lng"),
                imageUrl = o.optString("imageUrl").ifBlank { null },
                headline = o.optString("headline"),
                description = o.optString("description"),
                rank = o.optInt("rank"),
                signgu = o.optString("signgu"),
            )
        }.sortedBy { it.rank }
    }.getOrDefault(emptyList())
}
