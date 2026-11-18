package com.yuyagioh.tcgreader.core.action

import com.yuyagioh.tcgreader.core.model.PlayerId

sealed interface GameAction {
    data class PlayCard(
        val cardUid: Long,
        val faceDown: Boolean = false,
        val defensePosition: Boolean = false,
        val targetCardUid: Long? = null,
        val targetPlayer: PlayerId? = null,
        val tributeUids: List<Long> = emptyList(),
    ) : GameAction

    data class Attack(
        val cardUid: Long,
        val targetPlayer: PlayerId? = null,
        val targetCardUid: Long? = null,
    ) : GameAction

    data class Block(
        val blockerUid: Long,
        val attackerUid: Long,
    ) : GameAction

    data object EnterBattle : GameAction

    data object CommitBattle : GameAction

    data class Discard(val cardUid: Long) : GameAction

    data object EndPhase : GameAction
}
