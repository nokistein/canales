package es.verifirx.app.ocr

import android.graphics.Bitmap
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import es.verifirx.matching.BarcodeHit
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Decodes the barcode printed on each cupón precinto. Spanish cupón precinto
 * stickers most commonly carry a Code 128 or Code 39 symbol encoding the CN, so
 * we scan for those plus EAN-13 as a fallback for older/alternate labelling.
 */
class BarcodeAdapter {

    private val scanner = BarcodeScanning.getClient(
        BarcodeScannerOptions.Builder()
            .setBarcodeFormats(
                Barcode.FORMAT_CODE_128,
                Barcode.FORMAT_CODE_39,
                Barcode.FORMAT_EAN_13,
                Barcode.FORMAT_DATA_MATRIX,
            )
            .build(),
    )

    suspend fun scan(bitmap: Bitmap): List<BarcodeHit> {
        val image = InputImage.fromBitmap(bitmap, 0)
        val barcodes = suspendCancellableCoroutine { continuation ->
            scanner.process(image)
                .addOnSuccessListener { continuation.resume(it) }
                .addOnFailureListener { continuation.resumeWithException(it) }
        }

        return barcodes.mapNotNull { barcode ->
            val value = barcode.rawValue ?: return@mapNotNull null
            val box = barcode.boundingBox ?: return@mapNotNull null
            BarcodeHit(value = value, left = box.left, top = box.top, right = box.right, bottom = box.bottom)
        }
    }

    fun close() = scanner.close()
}
