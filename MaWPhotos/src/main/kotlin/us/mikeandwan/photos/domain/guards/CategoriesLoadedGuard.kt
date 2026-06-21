package us.mikeandwan.photos.domain.guards

import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.ErrorRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus

class CategoriesLoadedGuard
    @Inject
    constructor(
        private val categoryRepository: CategoryRepository,
        private val errorRepository: ErrorRepository,
    ) : IGuard {
        private val coroutineScope = CoroutineScope(Dispatchers.IO)
        private val initialized = AtomicBoolean(false)
        private val _status = MutableStateFlow<GuardStatus>(GuardStatus.NotInitialized)
        override val status = _status.asStateFlow()

        // Called from the CategoriesViewModel combine lambda which fires on every upstream
        // emission while status is NotInitialized. Guard with AtomicBoolean so we only launch
        // one coroutine regardless of how many emissions arrive before the first result.
        override fun initializeGuard() {
            if (!initialized.compareAndSet(false, true)) return

            coroutineScope.launch {
                val year = categoryRepository.getMostRecentYear().firstOrNull()

                if (year != null && year > 0) {
                    _status.update { GuardStatus.Passed }
                    return@launch
                }

                // No local data — fetch from the network and collect the result.
                categoryRepository.getUpdatedCategories().collect { result ->
                    when (result) {
                        is ExternalCallStatus.Success -> _status.update { GuardStatus.Passed }
                        is ExternalCallStatus.Error -> {
                            errorRepository.showError("Unable to load categories")
                            _status.update { GuardStatus.Failed }
                        }
                        else -> { }
                    }
                }
            }
        }
    }
