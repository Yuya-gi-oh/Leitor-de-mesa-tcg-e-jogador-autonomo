package com.yuyagioh.tcgreader.core.model

data class CardInfo(
    val id: String,
    val name: String,
    val game: GameId,
    val type: String = CardType.UNKNOWN,
    val manaCost: Int? = null,
    val level: Int? = null,
    val attack: Int? = null,
    val defense: Int? = null,
    val text: String = "",
    val imageUrl: String? = null,
)
