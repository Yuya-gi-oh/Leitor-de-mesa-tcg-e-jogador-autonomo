package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.rules.GameRules
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sqrt
import kotlin.random.Random

class MctsAgent(
    val iterations: Int = 300,
    val rolloutDepth: Int = 8,
    seed: Long = 7L,
) : AiAgent {

    private val random = Random(seed)

    override val labelPt: String = "Monte Carlo Tree Search"

    private class Node(
        val state: GameState,
        val parent: Node?,
        val move: GameAction?,
        val actor: PlayerId,
    ) {
        val children = mutableListOf<Node>()
        val untried = mutableListOf<GameAction>()
        var visits = 0
        var reward = 0.0
    }

    override fun chooseMove(state: GameState, rules: GameRules, me: PlayerId): AiMove {
        val rootMoves = rules.legalActions(state, me)
        require(rootMoves.isNotEmpty()) { "Não há jogadas legais disponíveis." }
        val root = Node(state, parent = null, move = null, actor = me)
        root.untried += rootMoves

        repeat(iterations) {
            var node = root
            var current = state
            var actor = me

            while (node.untried.isEmpty() && node.children.isNotEmpty()) {
                node = selectChild(node)
                current = rules.applyAction(current, node.move!!)
            }
            if (node.untried.isNotEmpty()) {
                val action = node.untried.removeAt(random.nextInt(node.untried.size))
                val childState = runCatching { rules.applyAction(current, action) }.getOrNull() ?: current
                actor = rules.nextActor(childState) ?: actor.opponent
                val child = Node(childState, parent = node, move = action, actor = actor)
                node.children += child
                node = child
            }

            val reward = rollout(current, rules, actor, me)

            var backprop: Node? = node
            while (backprop != null) {
                backprop.visits += 1
                backprop.reward += reward
                backprop = backprop.parent
            }
        }

        val best = root.children.maxByOrNull { it.reward / it.visits.coerceAtLeast(1) }
        val chosen = best?.move ?: rootMoves.first()
        val description = rules.describe(chosen, state)
        val confidence = best
            ?.let { "%.0f%%".format(100.0 * it.reward / it.visits.coerceAtLeast(1)) }
            ?: "?"
        return AiMove(
            action = chosen,
            explanation = "MCTS ($iterations simulações) escolheu \"$description\" com valor estimado $confidence.",
        )
    }

    private fun selectChild(node: Node): Node {
        val logVisits = ln(node.visits.coerceAtLeast(1).toDouble())
        return node.children.maxBy { child ->
            val mean = child.reward / child.visits.coerceAtLeast(1)
            val uct = if (child.actor == node.actor) mean else -mean
            uct + EXPLORATION * sqrt(logVisits / child.visits.coerceAtLeast(1))
        }
    }

    private fun rollout(state: GameState, rules: GameRules, actor: PlayerId, me: PlayerId): Double {
        var current = state
        var guard = 0
        while (guard++ < rolloutDepth) {
            val winner = rules.winner(current)
            if (winner != null) return if (winner == me) 1.0 else 0.0
            val who = rules.nextActor(current) ?: return sigmoid(rules.evaluate(current, me))
            val moves = rules.legalActions(current, who)
            if (moves.isEmpty()) return sigmoid(rules.evaluate(current, me))
            val action = moves[random.nextInt(moves.size)]
            current = runCatching { rules.applyAction(current, action) }.getOrNull()
                ?: return sigmoid(rules.evaluate(current, me))
        }
        return sigmoid(rules.evaluate(current, me))
    }

    private fun sigmoid(value: Double): Double = 1.0 / (1.0 + exp(-value / 10.0))

    companion object {
        private const val EXPLORATION = 1.4
    }
}
