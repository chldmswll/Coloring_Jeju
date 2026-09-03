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
    val category: TourCategory get() = TourCategory.from(contentTypeId, title)
}
