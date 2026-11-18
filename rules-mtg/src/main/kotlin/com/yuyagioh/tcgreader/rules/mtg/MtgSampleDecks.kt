package com.yuyagioh.tcgreader.rules.mtg

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameId

object MtgSampleDecks {

    private fun card(id: String, name: String, type: String, cost: Int = 0, atk: Int? = null, def: Int? = null) =
        CardInfo(
            id = "mtg-sample-$id",
            name = name,
            game = GameId.MAGIC,
            type = type,
            manaCost = cost,
            attack = atk,
            defense = def,
        )

    val plains = card("plains", "Plains", CardType.LAND)
    val island = card("island", "Island", CardType.LAND)
    val grizzlyBears = card("bears", "Grizzly Bears", CardType.CREATURE, cost = 2, atk = 2, def = 2)
    val hillGiant = card("hill-giant", "Hill Giant", CardType.CREATURE, cost = 4, atk = 3, def = 3)
    val shivanDragon = card("shivan-dragon", "Shivan Dragon", CardType.CREATURE, cost = 6, atk = 5, def = 5)
    val shock = card("shock", "Shock", CardType.SORCERY, cost = 1, atk = 2)
    val lightningBolt = card("lightning-bolt", "Lightning Bolt", CardType.SORCERY, cost = 1, atk = 3)

    fun starterDeck(): List<CardInfo> = buildList {
        repeat(20) { add(plains) }
        repeat(8) { add(island) }
        repeat(14) { add(grizzlyBears) }
        repeat(8) { add(hillGiant) }
        repeat(4) { add(shivanDragon) }
        repeat(4) { add(shock) }
        repeat(6) { add(lightningBolt) }
    }
}
