package com.yuyagioh.tcgreader.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class YgoCardImage(
    val image_url_small: String? = null,
    val image_url: String? = null,
)

data class YgoCardDto(
    val id: Long? = null,
    val name: String? = null,
    val type: String? = null,
    val atk: Int? = null,
    val def: Int? = null,
    val level: Int? = null,
    val desc: String? = null,
    val card_images: List<YgoCardImage> = emptyList(),
)

data class YgoCardInfoResponse(
    val data: List<YgoCardDto> = emptyList(),
)

interface YgoProDeckService {

    @GET("api/v7/cardinfo.php")
    suspend fun searchByNamePrefix(@Query("fname") namePrefix: String): YgoCardInfoResponse

    @GET("api/v7/cardinfo.php")
    suspend fun byExactName(@Query("name") name: String): YgoCardInfoResponse
}
