package com.yuyagioh.tcgreader.app.ui.game

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuyagioh.tcgreader.ai.AiAgent
import com.yuyagioh.tcgreader.ai.AgentFactory
import com.yuyagioh.tcgreader.app.di.RulesRegistry
import com.yuyagioh.tcgreader.app.setup.DeckSelection
import com.yuyagioh.tcgreader.core.action.GameAction
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.CardInstance
import com.yuyagioh.tcgreader.core.model.Difficulty
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.model.GameState
import com.yuyagioh.tcgreader.core.model.PlayerId
import com.yuyagioh.tcgreader.core.model.withPlayer
import com.yuyagioh.tcgreader.core.rules.GameRules
import com.yuyagioh.tcgreader.rules.mtg.MtgSampleDecks
import com.yuyagioh.tcgreader.rules.ygo.YgoSampleDecks
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ManualZone(val displayNamePt: String) {
    FIELD("campo"),
    HAND("mão"),
    GRAVEYARD("cemitério"),
    ;
}

@HiltViewModel
class GameViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val rulesRegistry: RulesRegistry,
    private val deckSelection: DeckSelection,
) : ViewModel() {

    val gameId: GameId = runCatching { GameId.valueOf(savedStateHandle.get<String>("gameId").orEmpty()) }
        .getOrDefault(GameId.MAGIC)

    val difficulty: Difficulty = runCatching { Difficulty.valueOf(savedStateHandle.get<String>("difficulty").orEmpty()) }
        .getOrDefault(Difficulty.MEDIUM)

    val format: GameFormat = runCatching { GameFormat.valueOf(savedStateHandle.get<String>("format").orEmpty()) }
        .getOrDefault(GameFormat.STANDARD)

    val rules: GameRules = rulesRegistry.get(gameId, format)

    private val agent: AiAgent = AgentFactory.byDifficulty(difficulty)

    private val _state = MutableStateFlow<GameState?>(null)
    val state: StateFlow<GameState?> = _state.asStateFlow()

    private val _log = MutableStateFlow<List<String>>(emptyList())
    val log: StateFlow<List<String>> = _log.asStateFlow()

    private val _thinking = MutableStateFlow(false)
    val thinking: StateFlow<Boolean> = _thinking.asStateFlow()

    private var aiJob: Job? = null

    fun startGameIfIdle() {
        if (_state.value == null) startGame()
    }

    fun startGame() {
        aiJob?.cancel()
        _log.value = emptyList()
        val yourDeck = deckSelection.deckFor(gameId) ?: defaultDeck()
        val deckSize = yourDeck.size
        _state.value = rules.initialState(yourDeck.shuffled(), defaultDeck().shuffled())
        pushLog(
            "Partida de ${gameId.displayNamePt} (${format.displayNamePt}) iniciada " +
                "(IA: ${difficulty.displayNamePt}, baralho com $deckSize cartas).",
        )
        runAiIfNeeded()
    }

    fun submit(action: GameAction) {
        val current = _state.value ?: return
        if (current.result != null) return
        val actor = rules.nextActor(current) ?: return
        if (actor != PlayerId.YOU) return
        if (!rules.legalActions(current, PlayerId.YOU).contains(action)) {
            pushLog("Jogada ilegal ignorada.")
            return
        }
        _state.value = rules.applyAction(current, action)
        pushLog("Tu: ${rules.describe(action, current)}")
        runAiIfNeeded()
    }

    fun addCardToZone(card: CardInfo, zone: ManualZone) {
        val current = _state.value ?: return
        val nextUid = current.players.values
            .flatMap { it.field + it.hand + it.deck + it.graveyard }
            .maxOfOrNull { it.uid }
            ?.plus(1) ?: 1L
        val instance = CardInstance(
            card = card.copy(game = gameId),
            uid = nextUid,
            enteredTurn = current.turnNumber,
        )
        _state.value = current.withPlayer(PlayerId.YOU) { player ->
            when (zone) {
                ManualZone.FIELD -> player.copy(field = player.field + instance)
                ManualZone.HAND -> player.copy(hand = player.hand + instance)
                ManualZone.GRAVEYARD -> player.copy(graveyard = player.graveyard + instance)
            }
        }
        pushLog("Correção manual: ${card.name} → ${zone.displayNamePt}.")
    }

    private fun defaultDeck(): List<CardInfo> = when (gameId) {
        GameId.MAGIC -> MtgSampleDecks.starterDeck()
        GameId.YUGIOH -> if (format == GameFormat.SPEED_DUEL) YgoSampleDecks.speedDuelDeck() else YgoSampleDecks.starterDeck()
    }

    private fun runAiIfNeeded() {
        val current = _state.value ?: return
        if (current.result != null) return
        if (rules.nextActor(current) != PlayerId.OPPONENT) return
        aiJob?.cancel()
        aiJob = viewModelScope.launch(Dispatchers.Default) {
            var localState = _state.value ?: return@launch
            var guard = 0
            while (guard++ < 60) {
                if (localState.result != null) break
                val actor = rules.nextActor(localState) ?: break
                if (actor != PlayerId.OPPONENT) break
                _thinking.value = true
                val move = agent.chooseMove(localState, rules, PlayerId.OPPONENT)
                localState = rules.applyAction(localState, move.action)
                _log.value = listOf(move.explanation) + _log.value
                _state.value = localState
                delay(450)
            }
            _thinking.value = false
        }
    }

    private fun pushLog(message: String) {
        _log.value = listOf(message) + _log.value
    }
}
