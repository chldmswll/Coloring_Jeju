package com.example.coloringjeju.core.network

import com.example.coloringjeju.BuildConfig
import com.example.coloringjeju.core.network.model.TourSpot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Talks to TourAPI (KorService2) for Jeju spots — the app calls the public API directly, there's
 * no backend in between. Every function runs the request on [Dispatchers.IO] and returns a
 * [TourApiResult] rather than throwing, so callers never need a try/catch.
 */
object TourRepository {
    private const val MOBILE_APP = "ColoringJeju"

    private val service get() = TourApiClient.service
    private val serviceKey get() = BuildConfig.TOUR_API_SERVICE_KEY

    /** 목록 조회 — 제주(area 39) 관광지, [contentTypeId]로 좁힐 수 있음 (12 자연 / 14,15 문화 / 28 레포츠 / 38 쇼핑 / 39 음식점). */
    suspend fun areaBasedList(
        pageNo: Int = 1,
        numOfRows: Int = 20,
        contentTypeId: String? = null,
    ): TourApiResult<List<TourSpot>> = withContext(Dispatchers.IO) {
        runCatching {
            service.areaBasedList(
                serviceKey = serviceKey,
                mobileApp = MOBILE_APP,
                pageNo = pageNo,
                numOfRows = numOfRows,
                contentTypeId = contentTypeId,
            )
        }.fold(
            onSuccess = { TourApiParser.parseList(it) },
            onFailure = { it.toNetworkError() },
        )
    }

    /** 키워드 검색 — areaCode 없이 전체 검색한 뒤, addr1에 "제주"가 포함된 것만 걸러서 반환. */
    suspend fun searchKeyword(keyword: String, numOfRows: Int = 20): TourApiResult<List<TourSpot>> =
        withContext(Dispatchers.IO) {
            runCatching {
                service.searchKeyword(serviceKey = serviceKey, mobileApp = MOBILE_APP, keyword = keyword, numOfRows = numOfRows)
            }.fold(
                onSuccess = { raw ->
                    when (val result = TourApiParser.parseList(raw)) {
                        is TourApiResult.Success -> TourApiResult.Success(result.data.filter { JEJU_KEYWORD in it.addr1 })
                        is TourApiResult.Error -> result
                    }
                },
                onFailure = { it.toNetworkError() },
            )
        }

    /** 상세 조회 — contentId만으로 요청 (contentTypeId·xxxYN 플래그를 같이 보내면 400). */
    suspend fun detail(contentId: String): TourApiResult<TourSpot> = withContext(Dispatchers.IO) {
        runCatching {
            service.detailCommon(serviceKey = serviceKey, mobileApp = MOBILE_APP, contentId = contentId)
        }.fold(
            onSuccess = { TourApiParser.parseDetail(it) },
            onFailure = { it.toNetworkError() },
        )
    }

    /** 상세 이미지 목록 — 각 원본 이미지 URL. */
    suspend fun images(contentId: String): TourApiResult<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            service.detailImage(serviceKey = serviceKey, mobileApp = MOBILE_APP, contentId = contentId)
        }.fold(
            onSuccess = { TourApiParser.parseImageUrls(it) },
            onFailure = { it.toNetworkError() },
        )
    }

    private const val JEJU_KEYWORD = "제주"

    private fun <T> Throwable.toNetworkError(): TourApiResult<T> =
        TourApiResult.Error("네트워크 오류가 발생했어요 (${message ?: this::class.simpleName}).")
}
