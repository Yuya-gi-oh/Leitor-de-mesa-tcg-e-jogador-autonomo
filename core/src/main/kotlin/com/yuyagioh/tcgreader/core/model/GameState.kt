package com.yuyagioh.tcgreader.core.model

data class GameState(
    val game: GameId,
    val turnNumber: Int,
    val phase: Phase,
    val activePlayer: PlayerId,
    val tappedCardUids: Set<Long> = emptySet(),
    val battle: BattleContext = BattleContext(),
    val onePerTurnPlayUsed: Boolean = false,
    val forcedDiscards: Int = 0,
    val result: PlayerId? = null,
    val players: Map<PlayerId, PlayerState>,
) {
    fun player(id: PlayerId): PlayerState = players.getValue(id)
}
