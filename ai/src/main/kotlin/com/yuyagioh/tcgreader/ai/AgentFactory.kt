package com.yuyagioh.tcgreader.ai

import com.yuyagioh.tcgreader.core.model.Difficulty

object AgentFactory {

    fun byDifficulty(difficulty: Difficulty, seed: Long = System.nanoTime()): AiAgent = when (difficulty) {
        Difficulty.EASY -> RandomAgent(kotlin.random.Random(seed))
        Difficulty.MEDIUM -> HeuristicAgent(kotlin.random.Random(seed))
        Difficulty.HARD -> SearchAgent(depth = 2, breadth = 6)
        Difficulty.MASTER -> MctsAgent(iterations = 400, rolloutDepth = 10, seed = seed)
    }
}
