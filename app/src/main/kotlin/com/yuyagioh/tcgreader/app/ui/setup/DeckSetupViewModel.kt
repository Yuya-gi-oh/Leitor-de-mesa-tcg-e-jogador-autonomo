package com.yuyagioh.tcgreader.app.ui.setup

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.yuyagioh.tcgreader.app.di.RulesRegistry
import com.yuyagioh.tcgreader.app.setup.DeckSelection
import com.yuyagioh.tcgreader.app.setup.SelectedDeck
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.Difficulty
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.rules.mtg.MtgSampleDecks
import com.yuyagioh.tcgreader.rules.ygo.YgoRules
import com.yuyagioh.tcgreader.rules.ygo.YgoSampleDecks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

@HiltViewModel
class DeckSetupViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val deckSelection: DeckSelection,
    rulesRegistry: RulesRegistry,
) : ViewModel() {

    val gameId: GameId = runCatching { GameId.valueOf(savedStateHandle.get<String>("gameId").orEmpty()) }
        .getOrDefault(GameId.MAGIC)

    val difficulty: Difficulty = runCatching { Difficulty.valueOf(savedStateHandle.get<String>("difficulty").orEmpty()) }
        .getOrDefault(Difficulty.MEDIUM)

    val format: GameFormat = runCatching { GameFormat.valueOf(savedStateHandle.get<String>("format").orEmpty()) }
        .getOrDefault(GameFormat.STANDARD)

    val cardCatalog: List<CardInfo> = catalogFor(gameId, format)

    val minimumDeckSize: Int =
        if (gameId == GameId.YUGIOH) {
            YgoRules(format).minimumDeckSize
        } else {
            MTG_MIN_DECK
        }

    private val _quantities = MutableStateFlow<Map<String, Int>>(emptyMap())
    val quantities: StateFlow<Map<String, Int>> = _quantities.asStateFlow()

    val totalCards: Int
        get() = _quantities.value.values.sum()

    val canStart: Boolean
        get() = totalCards >= minimumDeckSize

    init {
        useSampleDeck()
    }

    fun increment(card: CardInfo) {
        val max = maxCopies(card)
        val current = _quantities.value[card.id] ?: 0
        if (current < max) {
            _quantities.value = _quantities.value + (card.id to current + 1)
        }
    }

    fun decrement(card: CardInfo) {
        val current = _quantities.value[card.id] ?: 0
        val next = (current - 1).coerceAtLeast(0)
        _quantities.value = if (next == 0) _quantities.value - card.id else _quantities.value + (card.id to next)
    }

    fun useSampleDeck() {
        val sample = sampleDeck()
        val byId = cardCatalog.associateBy { it.id }
        val counts = sample.groupingBy { it.id }.eachCount()
        _quantities.value = byId.keys.associateWith { id -> counts[id] ?: 0 }.filterValues { it > 0 }
    }

    fun startGame() {
        if (!canStart) return
        val byId = cardCatalog.associateBy { it.id }
        val deck = _quantities.value.entries
            .sortedBy { it.key }
            .flatMap { (id, count) -> List(count) { byId.getValue(id) } }
        deckSelection.select(SelectedDeck(gameId, format, deck))
    }

    private fun prefill(catalog: List<CardInfo>, target: Int) {
        var remaining = target
        val counts = mutableMapOf<String, Int>()
        for (card in catalog) {
            if (remaining <= 0) break
            val take = minOf(maxCopies(card), remaining)
            counts[card.id] = take
            remaining -= take
        }
        _quantities.value = counts
    }

    private fun maxCopies(card: CardInfo): Int = cardCatalog.count { it.id == card.id }

    private fun sampleDeck(): List<CardInfo> = when (gameId) {
        GameId.MAGIC -> MtgSampleDecks.starterDeck()
        GameId.YUGIOH -> if (format == GameFormat.SPEED_DUEL) YgoSampleDecks.speedDuelDeck() else YgoSampleDecks.starterDeck()
    }

    private fun catalogFor(gameId: GameId, format: GameFormat): List<CardInfo> = when (gameId) {
        GameId.MAGIC -> {
            val sample = MtgSampleDecks.starterDeck()
            sample.distinctBy { it.id }
        }
        GameId.YUGIOH -> {
            val sample = if (format == GameFormat.SPEED_DUEL) YgoSampleDecks.speedDuelDeck() else YgoSampleDecks.starterDeck()
            sample.distinctBy { it.id }
        }
    }

    private companion object {
        const val MTG_MIN_DECK = 60
    }
}
