package com.yuyagioh.tcgreader.rules.mtg

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardType
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.findCard
import com.yuyagioh.tcgreader.core.model.Phase
import com.yuyagioh.tcgreader.core.model.PlayerId
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class MtgRulesTest {

    private lateinit var rules: MtgRules

    private val plains = CardInfo("plains", "Plains", GameId.MAGIC, CardType.LAND)
    private val bears = CardInfo("bears", "Grizzly Bears", GameId.MAGIC, CardType.CREATURE, manaCost = 2, attack = 2, defense = 2)
    private val bolt = CardInfo("bolt", "Lightning Bolt", GameId.MAGIC, CardType.SORCERY, manaCost = 1, attack = 3)

    @Before
    fun setUp() {
        rules = MtgRules()
    }

    private fun landDeck(size: Int = 15): List<CardInfo> = List(size) { plains }

    private fun handUid(state: GameState, type: String): Long =
        state.player(PlayerId.YOU).hand.first { it.card.type == type }.uid

    private fun landPlay(state: GameState): GameAction.PlayCard =
        rules.legalActions(state, PlayerId.YOU)
            .filterIsInstance<GameAction.PlayCard>()
            .first { candidate -> state.findCard(candidate.cardUid)?.card?.type == CardType.LAND }

    @Test
    fun onlyOneLandPerTurn() {
        val state = rules.initialState(landDeck(), landDeck())
        val landsInHand = state.player(PlayerId.YOU).hand.filter { it.card.type == CardType.LAND }
        val plays = rules.legalActions(state, PlayerId.YOU)
            .filterIsInstance<GameAction.PlayCard>()
            .filter { action -> landsInHand.any { it.uid == action.cardUid } }
        assertTrue(plays.isNotEmpty())

        val afterFirstLand = rules.applyAction(state, plays.first())
        val secondPlays = rules.legalActions(afterFirstLand, PlayerId.YOU)
            .filterIsInstance<GameAction.PlayCard>()
            .filter { action -> landsInHand.any { it.uid == action.cardUid } }
        assertTrue(secondPlays.isEmpty())
    }

    @Test
    fun creatureRequiresMana() {
        val state = rules.initialState(landDeck(), landDeck())
        val withMana = rules.applyAction(state, landPlay(state))
        assertEquals(1, withMana.player(PlayerId.YOU).mana)

        val creatureInHand = withMana.player(PlayerId.YOU).hand
            .firstOrNull { it.card.type == CardType.CREATURE && (it.card.manaCost ?: 0) <= 1 }
        if (creatureInHand != null) {
            val cast = rules.applyAction(withMana, GameAction.PlayCard(creatureInHand.uid))
            assertEquals(0, cast.player(PlayerId.YOU).mana)
        }
    }

    @Test
    fun creaturesCannotAttackOnTheTurnTheyArrive() {
        var state = rules.initialState(landDeck(), landDeck())
        state = rules.applyAction(state, landPlay(state))
        state = rules.applyAction(state, GameAction.PlayCard(handUid(state, CardType.CREATURE)))
        val attacks = rules.legalActions(state, PlayerId.YOU).filterIsInstance<GameAction.Attack>()
        assertTrue(attacks.isEmpty())
    }

    @Test
    fun unblockedCreatureDealsDamageToOpponent() {
        var state = rules.initialState(landDeck(), landDeck())
        state = rules.applyAction(state, landPlay(state))
        state = rules.applyAction(state, GameAction.PlayCard(handUid(state, CardType.CREATURE)))
        state = rules.applyAction(state, GameAction.EndPhase)
        state = rules.applyAction(state, GameAction.CommitBattle)
        state = rules.applyAction(state, GameAction.CommitBattle)
        assertEquals(Phase.MAIN_TWO, state.phase)
        assertEquals(18, state.player(PlayerId.OPPONENT).life)
    }

    @Test
    fun defenderCanBlockAndResolvesCombat() {
        var state = rules.initialState(landDeck(), landDeck())
        state = rules.applyAction(state, landPlay(state))
        state = rules.applyAction(state, GameAction.PlayCard(handUid(state, CardType.CREATURE)))
        state = rules.applyAction(state, GameAction.EndPhase)
        state = rules.applyAction(state, GameAction.CommitBattle)
        val defenderOptions = rules.legalActions(state, PlayerId.OPPONENT)
        assertTrue(defenderOptions.contains(GameAction.CommitBattle))
        state = rules.applyAction(state, GameAction.CommitBattle)
        assertEquals(Phase.MAIN_TWO, state.phase)
    }

    @Test
    fun burnSpellReducesOpponentLife() {
        var state = rules.initialState(List(11) { plains } + List(4) { bolt }, landDeck())
        val burnInHand = state.player(PlayerId.YOU).hand.firstOrNull { it.card.type == CardType.SORCERY }
            ?: error("O baralho de teste precisa de um feitiço na mão inicial")
        state = rules.applyAction(state, landPlay(state))
        val afterBurn = rules.applyAction(
            state,
            GameAction.PlayCard(burnInHand.uid, targetPlayer = PlayerId.OPPONENT),
        )
        assertEquals(20 - burnInHand.card.attack!!, afterBurn.player(PlayerId.OPPONENT).life)
    }

    @Test
    fun deckOutLosesTheGame() {
        var state = rules.initialState(List(8) { plains }, landDeck())
        var guard = 0
        while (rules.winner(state) == null && guard++ < 30) {
            val actor = rules.nextActor(state) ?: break
            val actions = rules.legalActions(state, actor)
            if (actions.isEmpty()) break
            val chosen = actions.filterIsInstance<GameAction.EndPhase>().firstOrNull()
                ?: actions.filterIsInstance<GameAction.CommitBattle>().firstOrNull()
                ?: actions.first()
            state = rules.applyAction(state, chosen)
        }
        assertEquals(PlayerId.OPPONENT, rules.winner(state))
    }

    @Test
    fun turnAdvancesAutomaticallyAfterEndPhase() {
        var state = rules.initialState(landDeck(), landDeck())
        state = rules.applyAction(state, landPlay(state))
        state = rules.applyAction(state, GameAction.EndPhase)
        state = rules.applyAction(state, GameAction.CommitBattle)
        state = rules.applyAction(state, GameAction.CommitBattle)
        state = rules.applyAction(state, GameAction.EndPhase)
        assertEquals(PlayerId.OPPONENT, state.activePlayer)
        assertEquals(2, state.turnNumber)
        assertEquals(Phase.MAIN_ONE, state.phase)
    }
}
