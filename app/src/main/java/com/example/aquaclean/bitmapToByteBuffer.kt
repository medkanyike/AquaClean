package com.example.aquaclean

import android.graphics.Bitmap
import java.nio.ByteBuffer
import java.nio.ByteOrder

fun bitmapToByteBuffer(bitmap: Bitmap): ByteBuffer {
    val byteBuffer = ByteBuffer.allocateDirect(4 * bitmap.width * bitmap.height * 3)
    byteBuffer.order(ByteOrder.nativeOrder())
    val intValues = IntArray(bitmap.width * bitmap.height)
    bitmap.getPixels(intValues, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)

    var pixel = 0
    for (i in 0 until bitmap.height) {
        for (j in 0 until bitmap.width) {
            val value = intValues[pixel++]

            byteBuffer.put((value shr 16 and 0xFF).toByte())
            byteBuffer.put((value shr 8 and 0xFF).toByte())
            byteBuffer.put((value and 0xFF).toByte())
        }
    }
    return byteBuffer
}
