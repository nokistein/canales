package es.verifirx.app.ocr

import android.graphics.Bitmap
import es.verifirx.matching.DispensationVerifier
import es.verifirx.matching.VerificationResult
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

/** Orchestrates OCR + barcode decoding + row comparison for one captured sheet image. */
class DocumentImageProcessor(
    private val textRecognitionAdapter: TextRecognitionAdapter,
    private val barcodeAdapter: BarcodeAdapter,
    private val verifier: DispensationVerifier = DispensationVerifier(),
) {

    suspend fun process(bitmap: Bitmap): VerificationResult = coroutineScope {
        val blocksDeferred = async { textRecognitionAdapter.recognize(bitmap) }
        val barcodesDeferred = async { barcodeAdapter.scan(bitmap) }

        val blocks = blocksDeferred.await()
        val barcodes = barcodesDeferred.await()

        verifier.verify(blocks = blocks, pageWidth = bitmap.width, barcodes = barcodes)
    }
}
