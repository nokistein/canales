package es.verifirx.app.di

import android.content.Context
import es.verifirx.app.data.SessionRepository
import es.verifirx.app.ocr.BarcodeAdapter
import es.verifirx.app.ocr.DocumentImageProcessor
import es.verifirx.app.ocr.TextRecognitionAdapter

/**
 * Small hand-rolled DI container. The app is intentionally simple enough (one
 * process, a handful of singletons) that pulling in Hilt/Dagger would add build
 * complexity without a real benefit.
 */
class ServiceLocator(context: Context) {
    val sessionRepository = SessionRepository(context.applicationContext)

    private val textRecognitionAdapter = TextRecognitionAdapter()
    private val barcodeAdapter = BarcodeAdapter()

    val documentImageProcessor = DocumentImageProcessor(textRecognitionAdapter, barcodeAdapter)
}
