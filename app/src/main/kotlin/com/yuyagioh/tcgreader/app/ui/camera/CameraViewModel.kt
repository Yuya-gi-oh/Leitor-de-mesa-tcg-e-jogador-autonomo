package com.yuyagioh.tcgreader.app.ui.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.vision.YuvUtils
import com.yuyagioh.tcgreader.vision.scan.CardScanner
import com.yuyagioh.tcgreader.vision.scan.ScannedCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject

@HiltViewModel
class CameraViewModel @Inject constructor(
    private val scanner: CardScanner,
) : ViewModel() {

    @Volatile
    private var game: GameId = GameId.MAGIC

    private val _scans = MutableStateFlow<List<ScannedCard>>(emptyList())
    val scans: StateFlow<List<ScannedCard>> = _scans.asStateFlow()

    private val _selectedCard = MutableStateFlow<CardInfo?>(null)
    val selectedCard: StateFlow<CardInfo?> = _selectedCard.asStateFlow()

    private val busy = AtomicBoolean(false)

    fun bind(gameId: GameId) {
        game = gameId
    }

    fun onAnalyzerFrame(image: ImageProxy) {
        if (!busy.compareAndSet(false, true)) {
            image.close()
            return
        }
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val bitmap = YuvUtils.imageProxyToBitmap(image) ?: return@launch
                val found = runCatching { scanner.scanFrame(bitmap, game) }.getOrDefault(emptyList())
                _scans.value = found.distinctBy { it.rawText.lowercase() }
            } finally {
                image.close()
                busy.set(false)
            }
        }
    }

    fun selectCandidate(card: CardInfo) {
        _selectedCard.value = card
    }

    fun consumeSelection() {
        _selectedCard.value = null
        _scans.value = emptyList()
    }
}
