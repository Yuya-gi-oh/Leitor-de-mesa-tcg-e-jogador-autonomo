package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.Phase
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.model.PlayerState
import com.yuyagioh.tcgreader.core.model.withPlayer
import com.yuyagioh.tcgreader.core.rules.GameRules
import com.yuyagioh.tcgreader.rules.mtg.MtgRules
import com.yuyagioh.tcgreader.rules.mtg.MtgSampleDecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DuelRules : GameRules {

    override val game: GameId = GameId.MAGIC

    override fun initialState(deckYou: List<CardInfo>, deckOpponent: List<CardInfo>): GameState = GameState(
        game = game,
        turnNumber = 1,
        phase = Phase.MAIN_ONE,
        activePlayer = PlayerId.YOU,
        players = mapOf(
            PlayerId.YOU to PlayerState(PlayerId.YOU, STARTING_LIFE),
            PlayerId.OPPONENT to PlayerState(PlayerId.OPPONENT, STARTING_LIFE),
        ),
    )

    override fun nextActor(state: GameState): PlayerId? =
        if (state.result != null) null else state.activePlayer

    override fun legalActions(state: GameState, playerId: PlayerId): List<GameAction> =
        if (playerId != state.activePlayer || state.result != null) {
            emptyList()
        } else {
            listOf(
                GameAction.PlayCard(STRIKE_UID),
                GameAction.PlayCard(PARRY_UID),
                GameAction.EndPhase,
            )
        }

    override fun applyAction(state: GameState, action: GameAction): GameState {
        val actor = state.activePlayer
        val next: GameState = when {
            action is GameAction.PlayCard && action.cardUid == STRIKE_UID ->
                state.withPlayer(actor.opponent) { it.copy(life = it.life - STRIKE_DAMAGE) }
            action is GameAction.PlayCard && action.cardUid == PARRY_UID ->
                state.withPlayer(actor) { it.copy(life = it.life + 1) }
            action is GameAction.EndPhase ->
                state
            else -> throw IllegalArgumentException("Ação ilegal no duelo.")
        }
        return next.copy(activePlayer = actor.opponent, turnNumber = state.turnNumber + 1)
    }

    override fun winner(state: GameState): PlayerId? {
        val dead = state.players.values.filter { it.life <= 0 }
        return if (dead.size == 1) dead.first().id.opponent else null
    }

    override fun evaluate(state: GameState, playerId: PlayerId): Double {
        when (winner(state)) {
            playerId -> return 1e9
            playerId.opponent -> return -1e9
        }
        return (state.player(playerId).life - state.player(playerId.opponent).life) * 10.0
    }

    override fun describe(action: GameAction, state: GameState): String = when {
        action is GameAction.PlayCard && action.cardUid == STRIKE_UID -> "Golpear"
        action is GameAction.PlayCard && action.cardUid == PARRY_UID -> "Parar"
        else -> "Passar"
    }

    companion object {
        const val STARTING_LIFE = 2
        const val STRIKE_UID = 1L
        const val PARRY_UID = 2L
        const val STRIKE_DAMAGE = 2
    }
}

class AiAgentsTest {

    private val duelRules = DuelRules()
    private val duelState = duelRules.initialState(emptyList(), emptyList())

    @Test
    fun heuristicPrefersStrikeOverParry() {
        val move = HeuristicAgent().chooseMove(duelState, duelRules, PlayerId.YOU)
        assertEquals(GameAction.PlayCard(DuelRules.STRIKE_UID), move.action)
    }

    @Test
    fun searchFindsTheImmediateKill() {
        val move = SearchAgent(depth = 2, breadth = 3).chooseMove(duelState, duelRules, PlayerId.YOU)
        assertEquals(GameAction.PlayCard(DuelRules.STRIKE_UID), move.action)
        assertTrue(move.explanation.isNotBlank())
    }

    @Test
    fun randomReturnsALegalMove() {
        val move = RandomAgent(kotlin.random.Random(1)).chooseMove(duelState, duelRules, PlayerId.YOU)
        assertTrue(
            DuelRules().legalActions(duelState, PlayerId.YOU).contains(move.action),
        )
    }

    @Test
    fun mctsReturnsALegalMove() {
        val move = MctsAgent(iterations = 60, rolloutDepth = 6, seed = 3)
            .chooseMove(duelState, duelRules, PlayerId.YOU)
        assertTrue(
            duelRules.legalActions(duelState, PlayerId.YOU).contains(move.action),
        )
        assertTrue(move.explanation.contains("MCTS"))
    }

    @Test
    fun allAgentsPlayLegallyOnMagicBoard() {
        val rules = MtgRules()
        val state = rules.initialState(MtgSampleDecks.starterDeck(), MtgSampleDecks.starterDeck())
        val agents: List<AiAgent> = listOf(
            RandomAgent(kotlin.random.Random(4)),
            HeuristicAgent(),
            SearchAgent(depth = 2, breadth = 4),
            MctsAgent(iterations = 40, rolloutDepth = 5, seed = 5),
        )
        for (agent in agents) {
            val move = agent.chooseMove(state, rules, PlayerId.YOU)
            val legal = rules.legalActions(state, PlayerId.YOU)
            assertTrue("Agente ${agent::class.simpleName} devolveu jogada ilegal", legal.contains(move.action))
        }
    }
}
