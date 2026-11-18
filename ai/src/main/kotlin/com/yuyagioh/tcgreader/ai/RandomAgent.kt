package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.rules.GameRules
import kotlin.random.Random

class RandomAgent(private val random: Random = Random.Default) : AiAgent {

    override val labelPt: String = "Aleatório"

    override fun chooseMove(state: GameState, rules: GameRules, me: PlayerId): AiMove {
        val meActor = rules.nextActor(state) ?: me
        val moves = rules.legalActions(state, meActor)
        require(moves.isNotEmpty()) { "Não há jogadas legais disponíveis." }
        val chosen = moves[random.nextInt(moves.size)]
        return AiMove(
            action = chosen,
            explanation = "Jogada aleatória entre ${moves.size} opções legais: ${rules.describe(chosen, state)}",
        )
    }
}
