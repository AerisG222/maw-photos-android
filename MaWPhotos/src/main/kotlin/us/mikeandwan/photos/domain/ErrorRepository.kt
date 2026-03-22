package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Clock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
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
        private val _error = MutableStateFlow<ErrorMessage>(ErrorMessage.DoNotDisplay)
        val error = _error.asStateFlow()

        private val _isDeveloperMode = MutableStateFlow(false)
        val isDeveloperMode = _isDeveloperMode.asStateFlow()

        val developerLogs = developerLogDao.getRecentLogs()

        fun showError(message: String) {
            _error.update { ErrorMessage.Display(message) }
        }

        suspend fun showThenClearError(message: String) {
            _error.update { ErrorMessage.Display(message) }
            delay(3000)
            _error.update { ErrorMessage.DoNotDisplay }
        }

        fun toggleDeveloperMode(code: String): Boolean {
            if (code == "mawsome") {
                _isDeveloperMode.update { !it }
                return true
            }
            return false
        }

        suspend fun logError(
            message: String,
            throwable: Throwable? = null,
        ) {
            Timber.e(throwable, message)
            withContext(Dispatchers.IO) {
                try {
                    developerLogDao.insert(
                        DeveloperLog(
                            message = message,
                            timestamp = Clock.System.now(),
                            level = "ERROR",
                            throwable = throwable?.stackTraceToString(),
                        ),
                    )
                    developerLogDao.pruneOldLogs()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to write error log to database")
                }
            }
        }

        suspend fun logInfo(message: String) {
            Timber.i(message)
            withContext(Dispatchers.IO) {
                try {
                    developerLogDao.insert(
                        DeveloperLog(
                            message = message,
                            timestamp = Clock.System.now(),
                            level = "INFO",
                        ),
                    )
                    developerLogDao.pruneOldLogs()
                } catch (e: Exception) {
                    Timber.e(e, "Failed to write info log to database")
                }
            }
        }

        suspend fun clearLogs() {
            withContext(Dispatchers.IO) {
                developerLogDao.clearAll()
            }
        }
    }
