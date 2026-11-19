package com.yuyagioh.tcgreader.app.setup

import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SelectedDeck(
    val gameId: GameId,
    val format: GameFormat,
    val cards: List<CardInfo>,
)

@Singleton
class DeckSelection @Inject constructor() {

    private val _selected = MutableStateFlow<SelectedDeck?>(null)
    val selected: StateFlow<SelectedDeck?> = _selected.asStateFlow()

    fun select(deck: SelectedDeck) {
        _selected.value = deck
    }

    fun deckFor(gameId: GameId): List<CardInfo>? =
        _selected.value
            ?.takeIf { it.gameId == gameId && it.cards.isNotEmpty() }
            ?.cards
}
