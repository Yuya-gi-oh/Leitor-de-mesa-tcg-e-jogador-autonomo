package com.yuyagioh.tcgreader.core.model

fun GameState.withPlayer(id: PlayerId, transform: (PlayerState) -> PlayerState): GameState =
    copy(players = players + (id to transform(players.getValue(id))))

fun GameState.findCard(uid: Long): CardInstance? =
    players.values.any { p ->
        (p.hand + p.field + p.supportZone + p.graveyard + p.deck).any { it.uid == uid }
    }.let { if (it) players.values.flatMap { p -> p.hand + p.field + p.supportZone + p.graveyard + p.deck }.find { c -> c.uid == uid } else null }
