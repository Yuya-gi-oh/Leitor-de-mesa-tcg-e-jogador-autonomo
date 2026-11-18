package com.yuyagioh.tcgreader.core.setup

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardInstance

data class DealtDeck(val deck: List<CardInstance>, val hand: List<CardInstance>)

object GameSetup {

    fun deal(deck: List<CardInfo>, handSize: Int): DealtDeck {
        require(handSize <= deck.size) { "Baralho sem cartas suficientes para a mão inicial" }
        var uid = 0L
        val instances = deck.map { card ->
            uid += 1
            CardInstance(card = card, uid = uid)
        }
        val deckPart = instances.dropLast(handSize)
        val hand = instances.takeLast(handSize)
        return DealtDeck(deck = deckPart, hand = hand)
    }
}
