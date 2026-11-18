package com.yuyagioh.tcgreader.vision.segmentation

import android.graphics.Bitmap
import android.graphics.Rect

class CardRegionDetector {

    fun detect(bitmap: Bitmap, maxRegions: Int = 6): List<Rect> {
        val targetWidth = 160
        val scale = targetWidth.toFloat() / bitmap.width
        val small = Bitmap.createScaledBitmap(bitmap, targetWidth, (bitmap.height * scale).toInt().coerceAtLeast(1), true)
        val width = small.width
        val height = small.height
        val pixels = IntArray(width * height)
        small.getPixels(pixels, 0, width, 0, 0, width, height)

        val luminance = FloatArray(width * height) { i ->
            val pixel = pixels[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF
            0.299f * r + 0.587f * g + 0.114f * b
        }

        val threshold = averageThreshold(luminance)
        val visited = BooleanArray(width * height)
        val regions = mutableListOf<Rect>()

        for (index in luminance.indices) {
            if (visited[index] || luminance[index] < threshold) continue
            val rect = floodFill(luminance, visited, width, height, index, threshold)
            if (rect != null) regions += rect
        }

        return regions
            .map { rect ->
                Rect(
                    (rect.left / scale).toInt(),
                    (rect.top / scale).toInt(),
                    (rect.right / scale).toInt(),
                    (rect.bottom / scale).toInt(),
                )
            }
            .sortedByDescending { rect -> rect.width() * rect.height() }
            .take(maxRegions)
    }

    private fun averageThreshold(luminance: FloatArray): Float {
        var sum = 0f
        for (v in luminance) sum += v
        return sum / luminance.size
    }

    private fun floodFill(
        luminance: FloatArray,
        visited: BooleanArray,
        width: Int,
        height: Int,
        startIndex: Int,
        threshold: Float,
    ): Rect? {
        val stack = ArrayDeque<Int>()
        stack.addLast(startIndex)
        visited[startIndex] = true
        var minX = Int.MAX_VALUE
        var minY = Int.MAX_VALUE
        var maxX = Int.MIN_VALUE
        var maxY = Int.MIN_VALUE
        var count = 0

        while (stack.isNotEmpty()) {
            val index = stack.removeLast()
            val x = index % width
            val y = index / width
            count++
            if (x < minX) minX = x
            if (y < minY) minY = y
            if (x > maxX) maxX = x
            if (y > maxY) maxY = y

            val neighbours = intArrayOf(
                index - 1, index + 1, index - width, index + width,
            )
            for (n in neighbours) {
                if (n < 0 || n >= luminance.size) continue
                if (n / width != y && abs(n - index) == 1) continue
                if (visited[n]) continue
                if (luminance[n] < threshold) continue
                visited[n] = true
                stack.addLast(n)
            }
        }

        if (count < MIN_COMPONENT_PIXELS) return null
        val w = maxX - minX
        val h = maxY - minY
        if (w <= 0 || h <= 0) return null
        val ratio = h.toFloat() / w
        if (ratio < MIN_ASPECT || ratio > MAX_ASPECT) return null
        return Rect(minX, minY, maxX + 1, maxY + 1)
    }

    private fun abs(value: Int): Int = if (value < 0) -value else value

    companion object {
        private const val MIN_COMPONENT_PIXELS = 80
        private const val MIN_ASPECT = 1.1f
        private const val MAX_ASPECT = 2.6f
    }
}
