package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.rules.GameRules

class SearchAgent(
    val depth: Int,
    val breadth: Int,
) : AiAgent {

    override val labelPt: String = "Minimax alfa-beta"

    override fun chooseMove(state: GameState, rules: GameRules, me: PlayerId): AiMove {
        val options = rules.legalActions(state, me)
        require(options.isNotEmpty()) { "Não há jogadas legais disponíveis." }
        val scored = options
            .map { action -> action to rules.applyAction(state, action) }
            .map { (action, child) ->
                val score = search(child, rules, me, depth - 1, Double.NEGATIVE_INFINITY, Double.POSITIVE_INFINITY)
                action to score
            }
        val best = scored.maxBy { it.second }
        val description = rules.describe(best.first, state)
        return AiMove(
            action = best.first,
            explanation = "Busca em profundidade $depth escolheu \"$description\" (valor ${"%.1f".format(best.second)}).",
        )
    }

    private fun search(
        state: GameState,
        rules: GameRules,
        me: PlayerId,
        depth: Int,
        alpha: Double,
        beta: Double,
    ): Double {
        if (rules.winner(state) != null || depth == 0) return rules.evaluate(state, me)
        val actor = rules.nextActor(state) ?: return rules.evaluate(state, me)
        val moves = orderedMoves(state, rules, actor, me)
        if (moves.isEmpty()) return rules.evaluate(state, me)
        var bestScore = if (actor == me) Double.NEGATIVE_INFINITY else Double.POSITIVE_INFINITY
        var alpha = alpha
        var beta = beta
        for (action in moves) {
            val child = runCatching { rules.applyAction(state, action) }.getOrNull() ?: continue
            val score = search(child, rules, me, depth - 1, alpha, beta)
            if (actor == me && score > bestScore) bestScore = score
            if (actor != me && score < bestScore) bestScore = score
            if (actor == me) alpha = maxOf(alpha, bestScore) else beta = minOf(beta, bestScore)
            if (beta <= alpha) break
        }
        return bestScore
    }

    private fun orderedMoves(
        state: GameState,
        rules: GameRules,
        actor: PlayerId,
        me: PlayerId,
    ): List<GameAction> {
        val moves = rules.legalActions(state, actor)
        if (moves.size <= breadth) return moves
        return moves
            .map { action ->
                val child = runCatching { rules.applyAction(state, action) }.getOrNull()
                action to (child?.let { rules.evaluate(it, actor) } ?: Double.NEGATIVE_INFINITY)
            }
            .sortedByDescending { it.second }
            .take(breadth)
            .map { it.first }
    }
}
