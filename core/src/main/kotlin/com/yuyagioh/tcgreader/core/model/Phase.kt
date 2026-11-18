package com.yuyagioh.tcgreader.core.model

enum class Phase(val displayNamePt: String) {
    UNTAP("Untap"),
    UPKEEP("Manutenção"),
    DRAW("Comprar"),
    MAIN_ONE("Principal 1"),
    BATTLE("Combate"),
    MAIN_TWO("Principal 2"),
    END("Final"),
    ;
}
