package es.verifirx.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import es.verifirx.app.data.SessionRecord
import es.verifirx.app.data.SessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val sessionRepository: SessionRepository) : ViewModel() {

    private val _recentSessions = MutableStateFlow<List<SessionRecord>>(emptyList())
    val recentSessions: StateFlow<List<SessionRecord>> = _recentSessions.asStateFlow()

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            _recentSessions.value = sessionRepository.list().take(5)
        }
    }
}
