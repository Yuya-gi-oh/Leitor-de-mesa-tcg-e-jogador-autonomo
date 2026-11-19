package com.yuyagioh.tcgreader.app.ui.home

import androidx.lifecycle.ViewModel
import com.yuyagioh.tcgreader.core.model.Difficulty
import com.yuyagioh.tcgreader.core.model.GameFormat
import com.yuyagioh.tcgreader.core.model.GameId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor() : ViewModel() {

    private val _selectedGame = MutableStateFlow(GameId.MAGIC)
    val selectedGame: StateFlow<GameId> = _selectedGame

    private val _selectedDifficulty = MutableStateFlow(Difficulty.MEDIUM)
    val selectedDifficulty: StateFlow<Difficulty> = _selectedDifficulty

    private val _selectedFormat = MutableStateFlow(GameFormat.STANDARD)
    val selectedFormat: StateFlow<GameFormat> = _selectedFormat

    fun selectGame(gameId: GameId) {
        _selectedGame.value = gameId
    }

    fun selectDifficulty(difficulty: Difficulty) {
        _selectedDifficulty.value = difficulty
    }

    fun selectFormat(format: GameFormat) {
        _selectedFormat.value = format
    }
}
