package com.yuyagioh.tcgreader.rules.ygo

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class YgoRulesTest {

    private lateinit var rules: YgoRules

    private val celtic = CardInfo("celtic", "Celtic Guardian", GameId.YUGIOH, CardType.MONSTER, attack = 1400, defense = 1200, level = 4)
    private val summonedSkull = CardInfo("skull", "Summoned Skull", GameId.YUGIOH, CardType.MONSTER, attack = 2500, defense = 1200, level = 6)

    @Before
    fun setUp() {
        rules = YgoRules()
    }

    private fun endlessCelticDeck(size: Int = 10): List<CardInfo> = List(size) { celtic }

    private fun choose(actions: List<GameAction>, preferPlay: Boolean): GameAction {
        val endPhase = actions.filterIsInstance<GameAction.EndPhase>().firstOrNull()
        val play = actions.filterIsInstance<GameAction.PlayCard>()
            .firstOrNull { !it.faceDown && !it.defensePosition }
        val fallback = actions.first()
        return when {
            preferPlay && play != null -> play
            endPhase != null -> endPhase
            else -> fallback
        }
    }

    private fun advanceUntilYouAreActive(state: GameState, opponentPlays: Boolean = false): GameState {
        var current = state
        var guard = 0
        while (rules.nextActor(current) != PlayerId.YOU && guard++ < 40) {
            val actor = rules.nextActor(current) ?: break
            val actions = rules.legalActions(current, actor)
            if (actions.isEmpty()) break
            current = rules.applyAction(current, choose(actions, opponentPlays))
        }
        return current
    }

    private fun endYouTurn(state: GameState): GameState {
        var current = state
        var guard = 0
        while (rules.nextActor(current) == PlayerId.YOU && guard++ < 40) {
            val actions = rules.legalActions(current, PlayerId.YOU)
            if (actions.isEmpty()) break
            current = rules.applyAction(current, choose(actions, preferPlay = false))
        }
        return current
    }

    @Test
    fun onlyOneNormalSummonPerTurn() {
        val state = rules.initialState(endlessCelticDeck(), endlessCelticDeck())
        val summons = rules.legalActions(state, PlayerId.YOU).filterIsInstance<GameAction.PlayCard>()
        assertTrue(summons.filter { !it.faceDown && !it.defensePosition }.isNotEmpty())

        val afterSummon = rules.applyAction(state, summons.first { !it.faceDown && !it.defensePosition })
        val secondSummons = rules.legalActions(afterSummon, PlayerId.YOU)
            .filterIsInstance<GameAction.PlayCard>()
        assertTrue(secondSummons.isEmpty())
    }

    @Test
    fun battlePhaseIsUnavailableOnTheFirstTurn() {
        val state = rules.initialState(endlessCelticDeck(), endlessCelticDeck())
        val enterBattle = rules.legalActions(state, PlayerId.YOU)
            .filterIsInstance<GameAction.EnterBattle>()
        assertTrue(enterBattle.isEmpty())
    }

    @Test
    fun highLevelSummonConsumesOneTribute() {
        val deck = List(5) { celtic } + List(4) { summonedSkull } + List(1) { celtic }
        var state = rules.initialState(deck, endlessCelticDeck())
        val guardianInHand = state.player(PlayerId.YOU).hand.first { it.card.level == 4 }
        val skullInHand = state.player(PlayerId.YOU).hand.first { it.card.level == 6 }

        val guardianSummon = rules.legalActions(state, PlayerId.YOU)
            .filterIsInstance<GameAction.PlayCard>()
            .first {
                !it.faceDown && !it.defensePosition && it.tributeUids.isEmpty() && it.cardUid == guardianInHand.uid
            }
        state = rules.applyAction(state, guardianSummon)

        val skullSummons = rules.legalActions(state, PlayerId.YOU)
            .filterIsInstance<GameAction.PlayCard>()
            .filter { it.cardUid == skullInHand.uid }
        assertTrue(skullSummons.none { it.tributeUids.isEmpty() })
        val withTribute = skullSummons.first { it.tributeUids.size == 1 }
        assertEquals(guardianInHand.uid, withTribute.tributeUids.first())

        val afterSkull = rules.applyAction(state, withTribute)
        val me = afterSkull.player(PlayerId.YOU)
        assertTrue(me.field.any { it.uid == skullInHand.uid && !it.faceDown })
        assertTrue(me.graveyard.any { it.uid == guardianInHand.uid })
    }

    @Test
    fun attackDestroysWeakerAttackPositionMonsterAndDealsDifference() {
        val deck = List(6) { celtic } + List(4) { summonedSkull }
        var state = rules.initialState(deck, endlessCelticDeck())

        val skull = state.player(PlayerId.YOU).hand.first { it.card.level == 6 }
        val guardian = state.player(PlayerId.YOU).hand.first { it.card.level == 4 }
        state = rules.applyAction(state, GameAction.PlayCard(guardian.uid))
        state = rules.applyAction(state, GameAction.PlayCard(skull.uid, tributeUids = listOf(guardian.uid)))
        state = endYouTurn(state)
        state = advanceUntilYouAreActive(state, opponentPlays = true)
        assertEquals(3, state.turnNumber)
        assertTrue(state.player(PlayerId.OPPONENT).field.isNotEmpty())

        val attacks = rules.legalActions(state, PlayerId.YOU).filterIsInstance<GameAction.Attack>()
        assertTrue(attacks.none { it.targetPlayer != null })
        val chosen = attacks.first { it.targetCardUid != null }

        val afterBattle = rules.applyAction(state, chosen)
        assertEquals(8000 - (2500 - 1400), afterBattle.player(PlayerId.OPPONENT).life)
        assertTrue(afterBattle.player(PlayerId.OPPONENT).field.isEmpty())
    }

    @Test
    fun directAttackIsPossibleOnlyWithEmptyOpponentField_AndThenDealsFullDamage() {
        val deck = List(6) { celtic } + List(4) { summonedSkull }
        var state = rules.initialState(deck, endlessCelticDeck())

        val skull = state.player(PlayerId.YOU).hand.first { it.card.level == 6 }
        val guardian = state.player(PlayerId.YOU).hand.first { it.card.level == 4 }
        state = rules.applyAction(state, GameAction.PlayCard(guardian.uid))
        state = rules.applyAction(state, GameAction.PlayCard(skull.uid, tributeUids = listOf(guardian.uid)))
        state = endYouTurn(state)
        state = advanceUntilYouAreActive(state, opponentPlays = false)
        assertTrue(state.player(PlayerId.OPPONENT).field.isEmpty())

        val attacks = rules.legalActions(state, PlayerId.YOU).filterIsInstance<GameAction.Attack>()
        assertTrue(attacks.none { it.targetCardUid != null })
        val direct = attacks.first { it.targetPlayer != null }

        val afterBattle = rules.applyAction(state, direct)
        assertEquals(8000 - 2500, afterBattle.player(PlayerId.OPPONENT).life)
    }

    @Test
    fun deckOutLosesTheGame() {
        val tinyDeck = List(5) { celtic }
        var state = rules.initialState(tinyDeck, endlessCelticDeck())
        var guard = 0
        while (rules.winner(state) == null && guard++ < 40) {
            val actor = rules.nextActor(state) ?: break
            val actions = rules.legalActions(state, actor)
            if (actions.isEmpty()) break
            val chosen = actions.filterIsInstance<GameAction.EndPhase>().firstOrNull() ?: actions.first()
            state = rules.applyAction(state, chosen)
        }
        assertEquals(PlayerId.OPPONENT, rules.winner(state))
    }
}
