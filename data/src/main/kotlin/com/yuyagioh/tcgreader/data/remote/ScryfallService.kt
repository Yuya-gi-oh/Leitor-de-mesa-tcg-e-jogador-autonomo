package com.yuyagioh.tcgreader.data.remote

import retrofit2.http.GET
import retrofit2.http.Query

data class ScryfallImageUris(
    val small: String? = null,
    val normal: String? = null,
)

data class ScryfallCardDto(
    val id: String? = null,
    val name: String? = null,
    val mana_cost: String? = null,
    val type_line: String? = null,
    val oracle_text: String? = null,
    val power: String? = null,
    val toughness: String? = null,
    val image_uris: ScryfallImageUris? = null,
)

data class ScryfallSearchResponse(
    val total_cards: Int? = null,
    val data: List<ScryfallCardDto> = emptyList(),
)

interface ScryfallService {

    @GET("cards/search")
    suspend fun search(
        @Query("q") query: String,
        @Query("page") page: Int = 1,
    ): ScryfallSearchResponse

    @GET("cards/named")
    suspend fun fuzzyNamed(@Query("fuzzy") name: String): ScryfallCardDto
}
