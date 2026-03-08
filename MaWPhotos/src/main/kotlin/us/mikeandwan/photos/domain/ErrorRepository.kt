package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import us.mikeandwan.photos.database.DeveloperLog
import us.mikeandwan.photos.database.DeveloperLogDao
import us.mikeandwan.photos.domain.models.ErrorMessage

@Singleton
class ErrorRepository
    @Inject
    constructor(
        private val developerLogDao: DeveloperLogDao,
    ) {
        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

        private val _error = MutableStateFlow<ErrorMessage>(ErrorMessage.DoNotDisplay)
        val error = _error.asStateFlow()

        private val _isDeveloperMode = MutableStateFlow(false)
        val isDeveloperMode = _isDeveloperMode.asStateFlow()

        val developerLogs = developerLogDao.getRecentLogs()

        fun showError(message: String) {
            _error.value = ErrorMessage.Display(message)
        }

        suspend fun showThenClearError(message: String) {
            _error.value = ErrorMessage.Display(message)
            delay(3000)
            _error.value = ErrorMessage.DoNotDisplay
        }

        fun toggleDeveloperMode(code: String): Boolean {
            if (code == "mawsome") {
                _isDeveloperMode.value = !_isDeveloperMode.value
                return true
            }
            return false
        }

        fun logError(
            message: String,
            throwable: Throwable? = null,
        ) {
            Timber.e(throwable, message)
            scope.launch {
                developerLogDao.insert(
                    DeveloperLog(
                        message = message,
                        timestamp = Clock.System.now(),
                        level = "ERROR",
                        throwable = throwable?.stackTraceToString(),
                    ),
                )
                developerLogDao.pruneOldLogs()
            }
        }

        fun logInfo(message: String) {
            Timber.i(message)
            scope.launch {
                developerLogDao.insert(
                    DeveloperLog(
                        message = message,
                        timestamp = Clock.System.now(),
                        level = "INFO",
                    ),
                )
                developerLogDao.pruneOldLogs()
            }
        }

        fun clearLogs() {
            scope.launch {
                developerLogDao.clearAll()
            }
        }
    }
