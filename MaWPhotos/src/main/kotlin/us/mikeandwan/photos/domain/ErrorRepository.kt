package us.mikeandwan.photos.domain

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber
import us.mikeandwan.photos.domain.models.ErrorMessage

class ErrorRepository {
    private val _error = MutableStateFlow<ErrorMessage>(ErrorMessage.DoNotDisplay)
    val error = _error.asStateFlow()

    private val _isDeveloperMode = MutableStateFlow(false)
    val isDeveloperMode = _isDeveloperMode.asStateFlow()

    private val _developerLogs = MutableStateFlow<List<String>>(emptyList())
    val developerLogs = _developerLogs.asStateFlow()

    fun showError(message: String) {
        _error.value = ErrorMessage.Display(message)
    }

    suspend fun showThenClearError(message: String) {
        _error.value = ErrorMessage.Display(message)
        // Adjust delay to be visible to user if it's meant to be a transient message
        delay(3000)
        _error.value = ErrorMessage.DoNotDisplay
    }

    fun toggleDeveloperMode(code: String): Boolean {
        if (code == "mawsome") {
            _isDeveloperMode.value = !_isDeveloperMode.value
            if (!_isDeveloperMode.value) {
                clearLogs()
            }
            return true
        }
        return false
    }

    fun logError(
        message: String,
        throwable: Throwable? = null,
    ) {
        Timber.e(throwable, message)
        if (_isDeveloperMode.value) {
            val logEntry = if (throwable != null) {
                "[$message]\n${throwable.stackTraceToString()}"
            } else {
                "[$message]"
            }
            _developerLogs.value = (listOf(logEntry) + _developerLogs.value).take(50)
        }
    }

    fun logInfo(message: String) {
        Timber.i(message)
        if (_isDeveloperMode.value) {
            _developerLogs.value = (listOf(message) + _developerLogs.value).take(50)
        }
    }

    fun clearLogs() {
        _developerLogs.value = emptyList()
    }
}
