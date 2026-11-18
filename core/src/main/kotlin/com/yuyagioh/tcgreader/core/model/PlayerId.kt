package com.yuyagioh.tcgreader.core.model

enum class PlayerId {
    YOU,
    OPPONENT,
    ;

    val opponent: PlayerId
        get() = if (this == YOU) OPPONENT else YOU
}
