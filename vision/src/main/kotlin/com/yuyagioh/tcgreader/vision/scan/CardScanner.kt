package com.yuyagioh.tcgreader.vision.scan

import android.graphics.Bitmap
import android.graphics.Rect
import com.yuyagioh.tcgreader.core.model.CardInfo
import com.yuyagioh.tcgreader.core.model.GameId
import com.yuyagioh.tcgreader.core.util.TextMatcher
import com.yuyagioh.tcgreader.data.repository.CardRepository
import com.yuyagioh.tcgreader.vision.ocr.CardNameRecognizer
import com.yuyagioh.tcgreader.vision.segmentation.CardRegionDetector
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class ScannedCard(
    val rawText: String,
    val candidates: List<CardInfo>,
    val region: Rect?,
)

class CardScanner(
    private val repository: CardRepository,
) {

    private val recognizer = CardNameRecognizer()
    private val regionDetector = CardRegionDetector()

    suspend fun scanFrame(bitmap: Bitmap, game: GameId, limitPerRegion: Int = 3): List<ScannedCard> =
        withContext(Dispatchers.Default) {
            val results = mutableListOf<ScannedCard>()
            val regions = regionDetector.detect(bitmap)

            val candidates: List<Pair<Rect?, Bitmap>> = if (regions.isEmpty()) {
                listOf(null to bitmap)
            } else {
                regions.map { region -> region to Bitmap.createBitmap(bitmap, region.left, region.top, region.width().coerceAtMost(bitmap.width - region.left), region.height().coerceAtMost(bitmap.height - region.top)) }
            }

            for ((region, regionBitmap) in candidates) {
                val lines = runCatching { recognizer.readLines(regionBitmap) }.getOrNull() ?: continue
                for (line in lines) {
                    val matches = repository.searchByName(game, line, limitPerRegion)
                        .filter { TextMatcher.isSimilar(line, it.name, maxDistance = 2) }
                    if (matches.isNotEmpty()) {
                        results += ScannedCard(rawText = line, candidates = matches, region = region)
                        break
                    }
                }
            }
            results
        }

    fun close() {
        recognizer.close()
    }
}
