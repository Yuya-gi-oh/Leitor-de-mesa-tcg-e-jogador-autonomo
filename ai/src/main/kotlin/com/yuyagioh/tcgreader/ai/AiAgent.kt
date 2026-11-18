package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.rules.GameRules

data class AiMove(
    val action: GameAction,
    val explanation: String,
)

interface AiAgent {
    val labelPt: String

    fun chooseMove(state: GameState, rules: GameRules, me: PlayerId): AiMove
}
