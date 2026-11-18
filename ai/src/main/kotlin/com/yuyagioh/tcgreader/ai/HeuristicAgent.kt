package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.rules.GameRules
import kotlin.random.Random

class HeuristicAgent(private val random: Random = Random.Default) : AiAgent {

    override val labelPt: String = "Heurístico"

    override fun chooseMove(state: GameState, rules: GameRules, me: PlayerId): AiMove {
        val options = rules.legalActions(state, me)
        require(options.isNotEmpty()) { "Não há jogadas legais disponíveis." }
        val scored = options.map { action ->
            val child = rules.applyAction(state, action)
            action to rules.evaluate(child, me)
        }
        val best = scored.maxBy { it.second }
        val ties = scored.count { it.second == best.second }
        val description = rules.describe(best.first, state)
        return AiMove(
            action = best.first,
            explanation = "Heurística escolheu \"$description\": avaliação ${format(best.second)}" +
                (if (ties > 1) ", empatada com ${ties - 1} alternativas" else "") +
                ".",
        )
    }

    private fun format(value: Double): String = "%.1f".format(value)
}
