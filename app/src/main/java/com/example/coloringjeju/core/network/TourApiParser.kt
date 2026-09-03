package com.example.coloringjeju.core.network

import com.example.coloringjeju.core.network.model.TourSpot
import org.json.JSONArray
import org.json.JSONObject

/** Result of a TourAPI call — success with parsed data, or a human-readable Korean [message]. */
sealed interface TourApiResult<out T> {
    data class Success<T>(val data: T) : TourApiResult<T>
    data class Error(val message: String) : TourApiResult<Nothing>
}

/**
 * Parses TourAPI (KorService2) responses. Works on raw text rather than through a JSON converter,
 * because the shape is too irregular for a strict schema:
 * - `response.body.items` is `""` (not `{}` or `[]`) when there are no results.
 * - `items.item` is a single object when there's exactly one result, an array otherwise.
 * - An invalid/not-yet-activated service key makes the endpoint ignore `_type=json` and return
 *   XML instead — detected here so the app can say so rather than crash on `JSONObject(raw)`.
 */
object TourApiParser {

    fun parseList(raw: String): TourApiResult<List<TourSpot>> {
        if (looksLikeXml(raw)) return TourApiResult.Error(XML_FALLBACK_MESSAGE)
        return try {
            val header = raw.responseHeader()
            val resultCode = header.optString("resultCode")
            if (resultCode != "0000") {
                return TourApiResult.Error(header.optString("resultMsg", "알 수 없는 오류가 발생했어요."))
            }
            TourApiResult.Success(raw.itemObjects().map { it.toTourSpot() })
        } catch (e: Exception) {
            TourApiResult.Error("응답을 해석하지 못했어요 (${e.message ?: e::class.simpleName}).")
        }
    }

    fun parseDetail(raw: String): TourApiResult<TourSpot> = when (val result = parseList(raw)) {
        is TourApiResult.Success -> result.data.firstOrNull()
            ?.let { TourApiResult.Success(it) }
            ?: TourApiResult.Error("해당 장소 정보를 찾을 수 없어요.")
        is TourApiResult.Error -> result
    }

    /** `detailImage2`'s items don't map to [TourSpot] — just each entry's `originimgurl`. */
    fun parseImageUrls(raw: String): TourApiResult<List<String>> {
        if (looksLikeXml(raw)) return TourApiResult.Error(XML_FALLBACK_MESSAGE)
        return try {
            val header = raw.responseHeader()
            if (header.optString("resultCode") != "0000") {
                return TourApiResult.Error(header.optString("resultMsg", "알 수 없는 오류가 발생했어요."))
            }
            TourApiResult.Success(raw.itemObjects().mapNotNull { it.optString("originimgurl").blankToNull() })
        } catch (e: Exception) {
            TourApiResult.Error("이미지 응답을 해석하지 못했어요 (${e.message ?: e::class.simpleName}).")
        }
    }

    private fun looksLikeXml(raw: String) = raw.trimStart().startsWith("<")

    private const val XML_FALLBACK_MESSAGE =
        "TourAPI가 JSON 대신 XML을 반환했어요 — 서비스키 문제(미승인/오타)일 수 있어요."

    private fun String.responseHeader(): JSONObject = JSONObject(this).getJSONObject("response").getJSONObject("header")

    private fun String.itemObjects(): List<JSONObject> {
        val body = JSONObject(this).getJSONObject("response").optJSONObject("body")
        return body?.opt("items").toJsonObjectList()
    }

    /** `""` (no results) and any other unexpected shape both just mean "nothing to show". */
    private fun Any?.toJsonObjectList(): List<JSONObject> = when (val items = this) {
        is JSONObject -> when (val item = items.opt("item")) {
            is JSONArray -> (0 until item.length()).map { item.getJSONObject(it) }
            is JSONObject -> listOf(item)
            else -> emptyList()
        }
        else -> emptyList()
    }

    private fun String.blankToNull(): String? = ifBlank { null }

    private fun JSONObject.toTourSpot() = TourSpot(
        contentId = optString("contentid"),
        contentTypeId = optString("contenttypeid"),
        title = optString("title"),
        addr1 = optString("addr1"),
        addr2 = optString("addr2"),
        tel = optString("tel"),
        lat = optString("mapy").toDoubleOrNull(),
        lng = optString("mapx").toDoubleOrNull(),
        image = optString("firstimage").blankToNull(),
        thumbnail = optString("firstimage2").blankToNull(),
        homepage = optString("homepage").blankToNull()?.stripHtmlTags(),
        overview = optString("overview").blankToNull(),
    )

    private fun String.stripHtmlTags(): String = replace(Regex("<[^>]*>"), "").trim()
}
