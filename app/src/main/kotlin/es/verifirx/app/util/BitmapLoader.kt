package es.verifirx.app.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Loads a [Bitmap] from a content Uri (camera capture or gallery pick),
 * correcting EXIF rotation and downscaling to a size that's plenty for OCR
 * while keeping memory use and processing time bounded on lower-end devices.
 */
object BitmapLoader {

    private const val MAX_DIMENSION = 2200

    suspend fun load(context: Context, uri: Uri): Bitmap = withContext(Dispatchers.IO) {
        val sampleSize = context.contentResolver.openInputStream(uri)?.use { stream ->
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(stream, null, bounds)
            calculateSampleSize(bounds.outWidth, bounds.outHeight, MAX_DIMENSION)
        } ?: 1

        val decoded = context.contentResolver.openInputStream(uri)?.use { stream ->
            val options = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            BitmapFactory.decodeStream(stream, null, options)
        } ?: error("No se pudo decodificar la imagen: $uri")

        val rotationDegrees = context.contentResolver.openInputStream(uri)?.use { stream ->
            ExifInterface(stream).rotationDegrees
        } ?: 0

        if (rotationDegrees == 0) decoded else rotate(decoded, rotationDegrees)
    }

    private fun rotate(bitmap: Bitmap, degrees: Int): Bitmap {
        val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
        val rotated = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (rotated !== bitmap) bitmap.recycle()
        return rotated
    }

    private fun calculateSampleSize(width: Int, height: Int, maxDimension: Int): Int {
        var sampleSize = 1
        var longestSide = maxOf(width, height)
        while (longestSide / (sampleSize * 2) >= maxDimension) {
            sampleSize *= 2
        }
        return sampleSize
    }
}
