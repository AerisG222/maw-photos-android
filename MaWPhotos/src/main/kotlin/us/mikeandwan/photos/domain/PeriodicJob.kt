package us.mikeandwan.photos.domain

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PeriodicJob<T>(
    doJob: Boolean = false,
    intervalMillis: Long = 3000,
    private val func: () -> Flow<T>,
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var job: Job? = null
    private val _doJob = MutableStateFlow(doJob)
    private val _intervalMillis = MutableStateFlow(intervalMillis)

    val isRunning = _doJob.asStateFlow()

    fun start() {
        _doJob.update { true }
    }

    fun stop() {
        _doJob.update { false }
    }

    fun setIntervalMillis(millis: Long) {
        _intervalMillis.update { millis }
    }

    init {
        scope.launch {
            combine(_doJob, _intervalMillis) { run, interval -> run to interval }
                .distinctUntilChanged()
                .collect { (run, interval) ->
                    job?.cancel()
                    if (run) {
                        job = launch {
                            while (isActive) {
                                delay(interval)
                                func().collect { }
                            }
                        }
                    }
                }
        }
    }
}
