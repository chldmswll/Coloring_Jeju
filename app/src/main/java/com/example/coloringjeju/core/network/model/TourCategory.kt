package com.example.coloringjeju.core.network.model

/**
 * The kinds of place this app puts on the map, mapped from TourAPI's `contenttypeid`.
 *
 * Only sightseeing types are listed. 쇼핑(38), 음식점/카페(39) and 숙박(32) are deliberately absent:
 * this is a "제주의 색을 모으는" map, and a mart or a franchise café is not a place you go to
 * capture a color. [from] returns null for those, and [com.example.coloringjeju.core.network.TourRepository]
 * drops them before the list ever reaches the UI — so search results, the category filter, and
 * 추천 지도 all agree on what counts as a 관광지.
 *
 * 여행코스(25) is left out too: it is a route rather than a single place, so it has no meaningful
 * pin on the map.
 */
enum class TourCategory(val label: String, internal val contentTypeIds: Set<String>) {
    NATURE("자연", setOf("12")),
    CULTURE("문화", setOf("14", "15")),
    LEISURE_SPORTS("레포츠", setOf("28")),
    ;

    companion object {
        /** null이면 관광지가 아니라는 뜻 — 목록에서 제외된다. */
        fun from(contentTypeId: String): TourCategory? =
            entries.firstOrNull { contentTypeId in it.contentTypeIds }
    }
}
