package com.samyak2403.scratchview.utils



import android.graphics.Bitmap
import java.nio.ByteBuffer

object BitmapUtils {

    /**
     * Finds the percentage of pixels that are transparent in the given bitmap.
     *
     * @param bitmap Input bitmap
     * @return A value between 0.0 to 1.0 representing the percentage of transparent pixels.
     *         Returns 0.0 if the bitmap is null.
     */
    fun getTransparentPixelPercent(bitmap: Bitmap?): Float {
        if (bitmap == null) {
            return 0f
        }

        val buffer = ByteBuffer.allocate(bitmap.height * bitmap.rowBytes)
        bitmap.copyPixelsToBuffer(buffer)

        val array = buffer.array()
        val len = array.size
        var count = 0

        for (b in array) {
            if (b.toInt() == 0) {
                count++
            }
        }

        return count.toFloat() / len
    }
}
