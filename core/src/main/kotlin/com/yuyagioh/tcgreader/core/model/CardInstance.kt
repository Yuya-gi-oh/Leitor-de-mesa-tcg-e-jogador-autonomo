package com.yuyagioh.tcgreader.core.model

data class CardInstance(
    val card: CardInfo,
    val uid: Long,
    val enteredTurn: Int = 0,
    val faceDown: Boolean = false,
    val attackPosition: Boolean = true,
)
