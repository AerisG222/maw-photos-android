package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlin.time.Clock
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Person

/**
 * The people the recognition pipeline has identified, held whole.
 *
 * The API deliberately does not page this - it is a few hundred rows at most - so the list is
 * fetched once and filtered locally, which is what lets the picker respond to typing without a
 * round trip per keystroke.
 */
class PeopleRepository
    @Inject
    constructor(
        private val api: FaceApiClient,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_PEOPLE = "Unable to load people at this time.  Please try again later."
            private const val ERR_MSG_SET_FAVORITE =
                "Unable to update the favorite at this time.  Please try again later."

            // long enough that moving between the grid and a person's media never refetches, short
            // enough that a newly published person turns up without restarting the app
            private val CACHE_DURATION = 15.minutes
        }

        private val _people = MutableStateFlow<List<Person>>(emptyList())
        val people = _people.asStateFlow()

        private var loadedAt: Instant? = null

        fun getPeople(forceRefresh: Boolean = false) =
            flow {
                if (!forceRefresh && isCacheValid()) {
                    emit(ExternalCallStatus.Success(_people.value))

                    return@flow
                }

                emit(ExternalCallStatus.Loading)

                when (val result = api.getPeople()) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_PEOPLE))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_PEOPLE))
                    }

                    is ApiResult.Success -> {
                        val people = result.result.map { it.toDomainPerson() }

                        _people.update { people }
                        loadedAt = Clock.System.now()

                        emit(ExternalCallStatus.Success(people))
                    }
                }
            }

        /**
         * Marks a person, answering with the flag that ended up in effect - the requested one when
         * the call landed, the one they already had when it did not.
         *
         * The API returns the updated person, but the only field that can have moved is the one
         * just sent, so this patches the cached entry rather than refetching the list.
         */
        suspend fun setIsFavorite(
            person: Person,
            isFavorite: Boolean,
        ): Boolean =
            when (val result = api.setPersonFavorite(person.id, isFavorite)) {
                is ApiResult.Success -> {
                    updatePerson(result.result.toDomainPerson())

                    result.result.isFavorite
                }

                is ApiResult.Error -> {
                    apiErrorHandler.handleError(result, ERR_MSG_SET_FAVORITE)

                    person.isFavorite
                }

                is ApiResult.Empty -> {
                    apiErrorHandler.handleEmpty(result, ERR_MSG_SET_FAVORITE)

                    person.isFavorite
                }
            }

        private fun updatePerson(updated: Person) {
            _people.update { currentList ->
                val index = currentList.indexOfFirst { it.id == updated.id }

                if (index < 0) {
                    currentList
                } else {
                    currentList.toMutableList().also { it[index] = updated }
                }
            }
        }

        private fun isCacheValid(): Boolean {
            val loaded = loadedAt ?: return false

            return _people.value.isNotEmpty() && Clock.System.now() - loaded < CACHE_DURATION
        }
    }
