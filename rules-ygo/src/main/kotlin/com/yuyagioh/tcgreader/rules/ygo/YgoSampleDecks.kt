package com.yuyagioh.tcgreader.rules.ygo

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameId

object YgoSampleDecks {

    private fun card(id: String, name: String, type: String, atk: Int? = null, def: Int? = null, level: Int? = null) =
        CardInfo(
            id = "ygo-sample-$id",
            name = name,
            game = GameId.YUGIOH,
            type = type,
            attack = atk,
            defense = def,
            level = level,
        )

    val celticGuardian = card("celtic", "Celtic Guardian", CardType.MONSTER, atk = 1400, def = 1200, level = 4)
    val mysticElf = card("mystic-elf", "Mystic Elf", CardType.MONSTER, atk = 800, def = 2000, level = 4)
    val giantSoldierOfStone = card("giant-stone", "Giant Soldier of Stone", CardType.MONSTER, atk = 1300, def = 2000, level = 3)
    val summonedSkull = card("summoned-skull", "Summoned Skull", CardType.MONSTER, atk = 2500, def = 1200, level = 6)
    val blueEyesWhiteDragon = card("blue-eyes", "Blue-Eyes White Dragon", CardType.MONSTER, atk = 3000, def = 2500, level = 8)
    val mysticalSpaceTyphoon = card("mst", "Mystical Space Typhoon", CardType.SPELL)

    fun starterDeck(): List<CardInfo> = buildList {
        repeat(4) { add(blueEyesWhiteDragon) }
        repeat(6) { add(summonedSkull) }
        repeat(16) { add(celticGuardian) }
        repeat(8) { add(mysticElf) }
        repeat(4) { add(giantSoldierOfStone) }
        repeat(2) { add(mysticalSpaceTyphoon) }
    }

    fun speedDuelDeck(): List<CardInfo> = buildList {
        repeat(2) { add(blueEyesWhiteDragon) }
        repeat(3) { add(summonedSkull) }
        repeat(8) { add(celticGuardian) }
        repeat(4) { add(mysticElf) }
        repeat(2) { add(giantSoldierOfStone) }
        repeat(1) { add(mysticalSpaceTyphoon) }
    }
}
