package com.yuyagioh.tcgreader.vision

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageFormat
import android.graphics.Rect
import android.graphics.YuvImage
import androidx.camera.core.ImageProxy
import java.io.ByteArrayOutputStream

object YuvUtils {

    fun imageProxyToBitmap(image: ImageProxy): Bitmap? = runCatching {
        val nv21 = imageProxyToNv21(image)
        val yuvImage = YuvImage(nv21, ImageFormat.NV21, image.width, image.height, null)
        val out = ByteArrayOutputStream()
        yuvImage.compressToJpeg(Rect(0, 0, image.width, image.height), 90, out)
        val bytes = out.toByteArray()
        BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
    }.getOrNull()

    private fun imageProxyToNv21(image: ImageProxy): ByteArray {
        val width = image.width
        val height = image.height
        val yPlane = image.planes[0].buffer
        val uPlane = image.planes[1].buffer
        val vPlane = image.planes[2].buffer
        val yRowStride = image.planes[0].rowStride
        val uRowStride = image.planes[1].rowStride
        val vRowStride = image.planes[2].rowStride
        val uPixelStride = image.planes[1].pixelStride
        val vPixelStride = image.planes[2].pixelStride

        val nv21 = ByteArray(width * height + 2 * (width / 2) * (height / 2))

        var copyIndex = 0
        for (row in 0 until height) {
            yPlane.position(row * yRowStride)
            yPlane.get(nv21, copyIndex, width)
            copyIndex += width
        }

        val chromaHeight = height / 2
        val chromaWidth = width / 2
        for (row in 0 until chromaHeight) {
            for (col in 0 until chromaWidth) {
                val vIndex = row * vRowStride + col * vPixelStride
                val uIndex = row * uRowStride + col * uPixelStride
                nv21[copyIndex++] = vPlane.get(vIndex)
                nv21[copyIndex++] = uPlane.get(uIndex)
            }
        }
        return nv21
    }
}
