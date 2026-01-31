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
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class PeriodicJob<T>(
    doJob: Boolean = false,
    intervalMillis: Long = 3000,
    private val func: () -> Flow<T>,
) {
    private var scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var nextJob: Job? = null
    private val _doJob = MutableStateFlow(doJob)
    private val _intervalMillis = MutableStateFlow(intervalMillis)

    val doJob = _doJob.asStateFlow()

    fun start() {
        _doJob.value = true
    }

    fun stop() {
        _doJob.value = false
    }

    fun setIntervalMillis(millis: Long) {
        _intervalMillis.value = millis
    }

    private fun cancelNextJob() {
        nextJob?.cancel()
        nextJob = null
    }

    private fun scheduleNextJob(
        scope: CoroutineScope,
        interval: Long,
    ) {
        nextJob = scope.launch {
            while (isActive) {
                delay(interval)
                func().collect { }
            }
        }
    }

    init {
        scope.launch {
            combine(
                _doJob,
                _intervalMillis,
            ) { doJob, interval -> Pair(doJob, interval) }
                .onEach { (doJob, interval) ->
                    if (doJob) {
                        if (nextJob != null) {
                            cancelNextJob()
                        }

                        scheduleNextJob(this, interval)
                    } else {
                        cancelNextJob()
                    }
                }.launchIn(this)
        }
    }
}
