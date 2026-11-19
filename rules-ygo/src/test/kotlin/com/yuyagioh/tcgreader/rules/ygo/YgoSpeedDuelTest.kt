package com.yuyagioh.tcgreader.rules.ygo

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YgoSpeedDuelTest {

    private val rules = YgoRules(GameFormat.SPEED_DUEL)

    private val celtic = CardInfo("celtic", "Celtic Guardian", GameId.YUGIOH, CardType.MONSTER, attack = 1400, defense = 1200, level = 4)

    @Test
    fun speedDuelStartsWithFourCards() {
        val deck = List(20) { celtic }
        val state = rules.initialState(deck, deck)
        assertEquals(4, state.player(com.yuyagioh.tcgreader.core.model.PlayerId.YOU).hand.size)
        assertEquals(16, state.player(com.yuyagioh.tcgreader.core.model.PlayerId.YOU).deck.size)
        assertEquals(8000, state.player(com.yuyagioh.tcgreader.core.model.PlayerId.YOU).life)
    }

    @Test
    fun speedDuelKeepsSummonAndBattleRules() {
        val deck = List(20) { celtic }
        var state = rules.initialState(deck, deck)
        val summons = rules.legalActions(state, com.yuyagioh.tcgreader.core.model.PlayerId.YOU)
            .filterIsInstance<com.yuyagioh.tcgreader.core.action.GameAction.PlayCard>()
            .filter { !it.faceDown && !it.defensePosition }
        assertTrue(summons.isNotEmpty())
        state = rules.applyAction(state, summons.first())
        val enterBattle = rules.legalActions(state, com.yuyagioh.tcgreader.core.model.PlayerId.YOU)
            .filterIsInstance<com.yuyagioh.tcgreader.core.action.GameAction.EnterBattle>()
        assertTrue(enterBattle.isEmpty() == (state.turnNumber == 1))
    }

    @Test
    fun speedDuelHasTwentyCardSampleDeck() {
        assertEquals(20, YgoSampleDecks.speedDuelDeck().size)
    }
}
