package es.verifirx.app.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.verifirx.app.data.SessionRecord
import es.verifirx.app.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HistoryViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val _sessions = MutableStateFlow<List<SessionRecord>>(emptyList())
    val sessions: StateFlow<List<SessionRecord>> = _sessions.asStateFlow()

    init {
        viewModelScope.launch { _sessions.value = sessionRepository.list() }
    }
}
