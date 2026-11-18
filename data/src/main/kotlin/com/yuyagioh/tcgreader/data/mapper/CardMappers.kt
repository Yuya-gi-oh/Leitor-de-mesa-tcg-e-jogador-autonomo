package com.yuyagioh.tcgreader.data.mapper

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.data.db.CardEntity
import com.yuyagioh.tcgreader.data.remote.ScryfallCardDto
import com.yuyagioh.tcgreader.data.remote.YgoCardDto

private val manaSymbolRegex = Regex("\\{([^}]+)\\}")

fun parseConvertedManaCost(manaCost: String?): Int? {
    if (manaCost.isNullOrBlank()) return null
    var total = 0
    var found = 0
    manaSymbolRegex.findAll(manaCost).forEach { match ->
        found++
        val symbol = match.groupValues[1]
        total += when {
            symbol.all { it.isDigit() } -> symbol.toInt()
            symbol.all { it in "0123456789" } -> symbol.toInt()
            symbol == "X" || symbol == "S" -> 0
            else -> 1
        }
    }
    return if (found == 0) null else total
}

fun normalizeMtgType(typeLine: String?): String {
    if (typeLine.isNullOrBlank()) return CardType.UNKNOWN
    return when {
        typeLine.contains("Land", ignoreCase = true) -> CardType.LAND
        typeLine.contains("Creature", ignoreCase = true) -> CardType.CREATURE
        typeLine.contains("Instant", ignoreCase = true) -> CardType.SORCERY
        typeLine.contains("Sorcery", ignoreCase = true) -> CardType.SORCERY
        else -> CardType.UNKNOWN
    }
}

fun normalizeYgoType(type: String?): String {
    if (type.isNullOrBlank()) return CardType.UNKNOWN
    return when {
        type.contains("Monster", ignoreCase = true) -> CardType.MONSTER
        type.contains("Spell", ignoreCase = true) -> CardType.SPELL
        type.contains("Trap", ignoreCase = true) -> CardType.TRAP
        else -> CardType.UNKNOWN
    }
}

fun ScryfallCardDto.toCardInfo(): CardInfo? {
    val name = name ?: return null
    val type = normalizeMtgType(type_line)
    val manaCostValue = parseConvertedManaCost(mana_cost)
    return CardInfo(
        id = id ?: name,
        name = name,
        game = com.yuyagioh.tcgreader.core.model.GameId.MAGIC,
        type = type,
        manaCost = manaCostValue,
        attack = power?.toIntOrNull(),
        defense = toughness?.toIntOrNull(),
        text = oracle_text.orEmpty(),
        imageUrl = image_uris?.small ?: image_uris?.normal,
    )
}

fun YgoCardDto.toCardInfo(): CardInfo? {
    val name = name ?: return null
    return CardInfo(
        id = id?.toString() ?: name,
        name = name,
        game = com.yuyagioh.tcgreader.core.model.GameId.YUGIOH,
        type = normalizeYgoType(type),
        level = level,
        attack = atk,
        defense = def,
        text = desc.orEmpty(),
        imageUrl = card_images.firstOrNull()?.image_url_small ?: card_images.firstOrNull()?.image_url,
    )
}

fun CardEntity.toCardInfo(): CardInfo = CardInfo(
    id = cardId,
    name = name,
    game = com.yuyagioh.tcgreader.core.model.GameId.entries.first { it.name == game },
    type = type,
    manaCost = manaCost,
    level = level,
    attack = attack,
    defense = defense,
    text = text,
    imageUrl = imageUrl,
)

fun CardInfo.toEntity(cachedAtMillis: Long): CardEntity = CardEntity(
    game = game.name,
    cardId = id,
    name = name,
    type = type,
    manaCost = manaCost,
    level = level,
    attack = attack,
    defense = defense,
    text = text,
    imageUrl = imageUrl,
    cachedAtMillis = cachedAtMillis,
)
