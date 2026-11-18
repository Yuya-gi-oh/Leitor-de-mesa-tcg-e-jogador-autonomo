package com.yuyagioh.tcgreader.app.di

import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.rules.GameRules

class RulesRegistry(private val rules: Map<GameId, GameRules>) {

    fun get(gameId: GameId): GameRules = rules.getValue(gameId)

    fun availableGames(): List<GameId> = rules.keys.sortedBy { it.displayNamePt }
}
