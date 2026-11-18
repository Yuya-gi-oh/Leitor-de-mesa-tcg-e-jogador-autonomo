package com.yuyagioh.tcgreader.core.model

data class PlayerState(
    val id: PlayerId,
    val life: Int,
    val mana: Int = 0,
    val deck: List<CardInstance> = emptyList(),
    val hand: List<CardInstance> = emptyList(),
    val field: List<CardInstance> = emptyList(),
    val supportZone: List<CardInstance> = emptyList(),
    val graveyard: List<CardInstance> = emptyList(),
) {
    val creatures: List<CardInstance>
        get() = field.filter { it.card.type == CardType.CREATURE }

    val monsters: List<CardInstance>
        get() = field.filter { it.card.type == CardType.MONSTER }
}
