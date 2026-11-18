package com.yuyagioh.tcgreader.core.rules

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId

interface GameRules {
    val game: GameId

    fun initialState(deckYou: List<CardInfo>, deckOpponent: List<CardInfo>): GameState

    fun nextActor(state: GameState): PlayerId?

    fun legalActions(state: GameState, playerId: PlayerId): List<GameAction>

    fun applyAction(state: GameState, action: GameAction): GameState

    fun isTerminal(state: GameState): Boolean = winner(state) != null

    fun winner(state: GameState): PlayerId?

    fun evaluate(state: GameState, playerId: PlayerId): Double = 0.0

    fun describe(action: GameAction, state: GameState): String
}
