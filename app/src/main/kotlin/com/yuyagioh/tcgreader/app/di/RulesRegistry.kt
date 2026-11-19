package com.yuyagioh.tcgreader.app.di

import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.rules.GameRules

class RulesRegistry(
    private val rules: Map<GameId, GameRules>,
    private val ygoByFormat: Map<GameFormat, GameRules> = emptyMap(),
) {

    fun get(gameId: GameId, format: GameFormat = GameFormat.STANDARD): GameRules =
        if (gameId == GameId.YUGIOH) {
            ygoByFormat[format] ?: rules.getValue(gameId)
        } else {
            rules.getValue(gameId)
        }

    fun availableGames(): List<GameId> = rules.keys.sortedBy { it.displayNamePt }
}
