package com.example.coloringjeju.core.network.model

/**
 * The 6 categories TourAPI's `contenttypeid` collapses down to for display/filtering.
 * `contenttypeid` 39 (음식점) doesn't distinguish cafés from restaurants itself, so [from] falls
 * back to a title keyword check for that one case.
 */
enum class TourCategory(val label: String) {
    NATURE("자연"),
    CULTURE("문화"),
    LEISURE_SPORTS("레포츠"),
    SHOPPING("쇼핑"),
    CAFE("카페"),
    RESTAURANT("맛집"),
    ;

    companion object {
        private val CAFE_KEYWORDS = listOf("카페", "커피", "베이커리", "디저트")

        fun from(contentTypeId: String, title: String): TourCategory = when (contentTypeId) {
            "12" -> NATURE
            "14", "15" -> CULTURE
            "28" -> LEISURE_SPORTS
            "38" -> SHOPPING
            "39" -> if (CAFE_KEYWORDS.any { title.contains(it) }) CAFE else RESTAURANT
            else -> RESTAURANT
        }
    }
}
