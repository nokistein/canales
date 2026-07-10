package es.verifirx.app.ui.capture

import android.content.Context
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.verifirx.app.data.SessionRepository
import es.verifirx.app.ocr.DocumentImageProcessor
import es.verifirx.app.util.BitmapLoader
import java.io.File
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class CaptureErrorReason { NO_ROWS_DETECTED, PROCESSING_FAILED }

sealed interface CaptureUiState {
    data object Ready : CaptureUiState
    data object Processing : CaptureUiState
    data class Error(val reason: CaptureErrorReason) : CaptureUiState
}

sealed interface CaptureEvent {
    data class NavigateToResults(val sessionId: String) : CaptureEvent
}

class CaptureViewModel(
    private val documentImageProcessor: DocumentImageProcessor,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<CaptureUiState>(CaptureUiState.Ready)
    val uiState: StateFlow<CaptureUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<CaptureEvent>()
    val events: SharedFlow<CaptureEvent> = _events.asSharedFlow()

    /** Creates a private, FileProvider-backed destination for a fresh CameraX capture. */
    fun createCaptureOutputFile(context: Context): Pair<File, Uri> {
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "capture_${System.currentTimeMillis()}.jpg")
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        return file to uri
    }

    fun onImageReady(context: Context, uri: Uri) {
        if (_uiState.value is CaptureUiState.Processing) return
        viewModelScope.launch {
            _uiState.value = CaptureUiState.Processing
            try {
                val bitmap = BitmapLoader.load(context, uri)
                val result = documentImageProcessor.process(bitmap)
                if (result.rows.isEmpty()) {
                    _uiState.value = CaptureUiState.Error(CaptureErrorReason.NO_ROWS_DETECTED)
                    return@launch
                }
                val session = sessionRepository.save(bitmap, result)
                _uiState.value = CaptureUiState.Ready
                _events.emit(CaptureEvent.NavigateToResults(session.id))
            } catch (t: Throwable) {
                _uiState.value = CaptureUiState.Error(CaptureErrorReason.PROCESSING_FAILED)
            }
        }
    }

    fun dismissError() {
        _uiState.value = CaptureUiState.Ready
    }
}
