package es.verifirx.app.ui.results

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.verifirx.app.data.SessionRecord
import es.verifirx.app.data.SessionRepository
import es.verifirx.app.data.StoredVerdict
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ResultsViewModel(
    private val sessionId: String,
    private val sessionRepository: SessionRepository,
) : ViewModel() {

    private val _session = MutableStateFlow<SessionRecord?>(null)
    val session: StateFlow<SessionRecord?> = _session.asStateFlow()

    init {
        viewModelScope.launch { _session.value = sessionRepository.get(sessionId) }
    }

    fun setManualVerdict(rowIndex: Int, verdict: StoredVerdict, note: String) {
        val current = _session.value ?: return
        val updated = current.copy(
            rows = current.rows.map { row ->
                if (row.rowIndex == rowIndex) row.copy(manualVerdict = verdict, manualNote = note) else row
            },
        )
        _session.value = updated
        viewModelScope.launch { sessionRepository.update(updated) }
    }
}
