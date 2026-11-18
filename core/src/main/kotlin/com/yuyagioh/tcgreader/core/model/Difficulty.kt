package com.yuyagioh.tcgreader.core.model

enum class Difficulty(val displayNamePt: String, val descriptionPt: String) {
    EASY("Fácil", "Jogadas legais aleatórias."),
    MEDIUM("Médio", "Avaliação heurística do estado."),
    HARD("Difícil", "Minimax com poda alfa-beta."),
    MASTER("Mestre", "Monte Carlo Tree Search."),
    ;
}
