package com.yuyagioh.tcgreader.data.db

import androidx.room.Entity

@Entity(tableName = "cards", primaryKeys = ["game", "cardId"])
data class CardEntity(
    val game: String,
    val cardId: String,
    val name: String,
    val type: String,
    val manaCost: Int? = null,
    val level: Int? = null,
    val attack: Int? = null,
    val defense: Int? = null,
    val text: String = "",
    val imageUrl: String? = null,
    val cachedAtMillis: Long,
)
