package com.example.coloringjeju.core.network.model

/**
 * One place returned by TourAPI (KorService2) — `areaBasedList2`, `searchKeyword2`, and
 * `detailCommon2` all map their `item` entries to this. Field names mirror the API's own
 * (lowercase) naming 1:1 except [lat]/[lng], which come from `mapy`/`mapx`.
 */
data class TourSpot(
    val contentId: String,
    val contentTypeId: String,
    val title: String,
    val addr1: String,
    val addr2: String,
    val tel: String,
    val lat: Double?,
    val lng: Double?,
    val image: String?,
    val thumbnail: String?,
    val homepage: String?,
    val overview: String?,
) {
    /** null이면 관광지가 아니라는 뜻 (쇼핑·음식점·숙박) — [com.example.coloringjeju.core.network.TourRepository]가 목록에서 걸러낸다. */
    val category: TourCategory? get() = TourCategory.from(contentTypeId)
}
