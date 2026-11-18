package com.yuyagioh.tcgreader.core.model

data class BattleContext(
    val attackers: Map<Long, PlayerId> = emptyMap(),
    val blocks: Map<Long, Long> = emptyMap(),
    val attackersCommitted: Boolean = false,
)
