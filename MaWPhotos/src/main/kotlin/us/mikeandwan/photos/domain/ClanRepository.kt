package us.mikeandwan.photos.domain

import java.net.HttpURLConnection
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.update
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.FaceApiClient
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.ClanResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus

/**
 * The caller's clans - saved selections of people, private to whoever made them.
 *
 * Writes patch the cached list rather than refetching it: the API answers every write with the
 * whole clan, so the reply is already the new state of the only row that moved.
 */
class ClanRepository
    @Inject
    constructor(
        private val api: FaceApiClient,
        private val apiErrorHandler: ApiErrorHandler,
    ) {
        companion object {
            private const val ERR_MSG_LOAD_CLANS = "Unable to load clans at this time.  Please try again later."
            private const val ERR_MSG_SAVE_CLAN = "Unable to save the clan at this time.  Please try again later."
            private const val ERR_MSG_DELETE_CLAN = "Unable to delete the clan at this time.  Please try again later."
        }

        private val _clans = MutableStateFlow<List<Clan>>(emptyList())
        val clans = _clans.asStateFlow()

        private var isLoaded = false

        fun getClans(forceRefresh: Boolean = false) =
            flow {
                if (!forceRefresh && isLoaded) {
                    emit(ExternalCallStatus.Success(_clans.value))

                    return@flow
                }

                emit(ExternalCallStatus.Loading)

                when (val result = api.getClans()) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_CLANS))
                    }

                    // a caller with no clans yet is the normal starting state rather than a failure
                    is ApiResult.Empty -> {
                        isLoaded = true

                        emit(ExternalCallStatus.Success(emptyList()))
                    }

                    is ApiResult.Success -> {
                        val clans = result.result.map { it.toDomainClan() }

                        _clans.update { clans.sortedBy { clan -> clan.name } }
                        isLoaded = true

                        emit(ExternalCallStatus.Success(clans))
                    }
                }
            }

        suspend fun createClan(
            name: String,
            personIds: List<Uuid>,
        ): ClanResult = write { api.createClan(name, personIds) }

        suspend fun renameClan(
            clanId: Uuid,
            name: String,
        ): ClanResult = write { api.renameClan(clanId, name) }

        suspend fun setClanPeople(
            clanId: Uuid,
            personIds: List<Uuid>,
        ): ClanResult = write { api.setClanPeople(clanId, personIds) }

        suspend fun deleteClan(clanId: Uuid): Boolean =
            when (val result = api.deleteClan(clanId)) {
                // 204 carries no body, which arrives here as Empty.  for a delete that is the
                // success, not a missing answer.
                is ApiResult.Empty, is ApiResult.Success -> {
                    _clans.update { clans -> clans.filterNot { it.id == clanId } }

                    true
                }

                is ApiResult.Error -> {
                    apiErrorHandler.handleError(result, ERR_MSG_DELETE_CLAN)

                    false
                }
            }

        private suspend fun write(call: suspend () -> ApiResult<us.mikeandwan.photos.api.Clan>): ClanResult =
            when (val result = call()) {
                is ApiResult.Success -> {
                    val clan = result.result.toDomainClan()

                    upsertClan(clan)

                    ClanResult.Success(clan)
                }

                is ApiResult.Empty -> {
                    apiErrorHandler.handleEmpty(result, ERR_MSG_SAVE_CLAN)

                    ClanResult.Failed
                }

                is ApiResult.Error -> {
                    // a name collision and a refused request are both answers the caller can act
                    // on, and the dialog that asked for them says so in place.  passing no message
                    // logs them without also raising the app wide error snackbar.
                    val expected = result.errorCode == HttpURLConnection.HTTP_CONFLICT ||
                        result.errorCode == HttpURLConnection.HTTP_BAD_REQUEST

                    apiErrorHandler.handleError(result, if (expected) null else ERR_MSG_SAVE_CLAN)

                    when (result.errorCode) {
                        HttpURLConnection.HTTP_CONFLICT -> ClanResult.DuplicateName
                        HttpURLConnection.HTTP_BAD_REQUEST -> ClanResult.Invalid
                        else -> ClanResult.Failed
                    }
                }
            }

        // the API orders clans by name, so the same rule is applied here and a renamed or new clan
        // lands where the next fetch would have put it
        private fun upsertClan(updated: Clan) {
            _clans.update { currentList ->
                val index = currentList.indexOfFirst { it.id == updated.id }
                val next = currentList.toMutableList()

                if (index < 0) {
                    next.add(updated)
                } else {
                    next[index] = updated
                }

                next.sortedBy { it.name }
            }
        }
    }
