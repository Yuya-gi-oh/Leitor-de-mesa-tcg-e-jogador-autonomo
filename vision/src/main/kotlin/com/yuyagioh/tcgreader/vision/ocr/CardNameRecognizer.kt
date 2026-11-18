package com.yuyagioh.tcgreader.vision.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

class CardNameRecognizer {

    private val recognizer by lazy {
        TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    }

    suspend fun readLines(bitmap: Bitmap): List<String> =
        suspendCoroutine { continuation ->
            recognizer.process(InputImage.fromBitmap(bitmap, 0))
                .addOnSuccessListener { text ->
                    val lines = text.textBlocks
                        .flatMap { block -> block.lines }
                        .map { it.text.trim() }
                        .filter { it.length >= MIN_LINE_LENGTH }
                    continuation.resume(lines)
                }
                .addOnFailureListener { error -> continuation.resumeWithException(error) }
        }

    fun close() {
        recognizer.close()
    }

    companion object {
        private const val MIN_LINE_LENGTH = 3
    }
}
