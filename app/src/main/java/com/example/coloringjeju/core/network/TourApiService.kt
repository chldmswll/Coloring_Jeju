package com.example.coloringjeju.core.network

import retrofit2.http.GET
import retrofit2.http.Query

/**
 * 한국관광공사 TourAPI (KorService2) — called directly from the app, no backend in between.
 * Every method returns the raw JSON body as a [String] (via a scalars converter) instead of a
 * parsed type: the schema is too irregular for a strict converter (`items` is `""`, a single
 * object, or an array depending on result count) — see [TourApiParser].
 *
 * `serviceKey` must be the *decoded* key ([com.example.coloringjeju.BuildConfig.TOUR_API_SERVICE_KEY]);
 * `@Query` URL-encodes it by default, which is exactly what TourAPI expects — never add
 * `encoded = true` here, that double-decodes it server-side and breaks auth.
 */
interface TourApiService {

    /** 목록 조회 — area 39 (제주) attractions, paged. */
    @GET("areaBasedList2")
    suspend fun areaBasedList(
        @Query("serviceKey") serviceKey: String,
        @Query("MobileOS") mobileOs: String = "ETC",
        @Query("MobileApp") mobileApp: String,
        @Query("_type") type: String = "json",
        @Query("areaCode") areaCode: Int = JEJU_AREA_CODE,
        @Query("arrange") arrange: String = "O",
        @Query("pageNo") pageNo: Int = 1,
        @Query("numOfRows") numOfRows: Int = 20,
        @Query("contentTypeId") contentTypeId: String? = null,
    ): String

    /**
     * 키워드 검색 — deliberately has no `areaCode` param: combining it drops otherwise-valid
     * results (e.g. 월정리해변, 천지연폭포). Callers should filter the result's `addr1` for "제주"
     * themselves instead (see [TourRepository.searchKeyword]).
     */
    @GET("searchKeyword2")
    suspend fun searchKeyword(
        @Query("serviceKey") serviceKey: String,
        @Query("MobileOS") mobileOs: String = "ETC",
        @Query("MobileApp") mobileApp: String,
        @Query("_type") type: String = "json",
        @Query("keyword") keyword: String,
        @Query("arrange") arrange: String = "O",
        @Query("numOfRows") numOfRows: Int = 20,
    ): String

    /** 상세 조회 — `contentId` only; adding `contentTypeId` or any `xxxYN` flag 400s. */
    @GET("detailCommon2")
    suspend fun detailCommon(
        @Query("serviceKey") serviceKey: String,
        @Query("MobileOS") mobileOs: String = "ETC",
        @Query("MobileApp") mobileApp: String,
        @Query("_type") type: String = "json",
        @Query("contentId") contentId: String,
    ): String

    /** 상세 이미지 목록 — each item's `originimgurl` is a full-size photo URL. */
    @GET("detailImage2")
    suspend fun detailImage(
        @Query("serviceKey") serviceKey: String,
        @Query("MobileOS") mobileOs: String = "ETC",
        @Query("MobileApp") mobileApp: String,
        @Query("_type") type: String = "json",
        @Query("contentId") contentId: String,
        @Query("imageYN") imageYn: String = "Y",
    ): String

    companion object {
        const val BASE_URL = "https://apis.data.go.kr/B551011/KorService2/"
        const val JEJU_AREA_CODE = 39
    }
}
