package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.PeoplePreferenceDao
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PersonSort

class PeoplePreferenceRepository
    @Inject
    constructor(
        private val dao: PeoplePreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getPeoplePreference() =
            dao
                .getPeoplePreference(PREFERENCE_ID)
                .map { it.toDomainPeoplePreference() }

        suspend fun setSortBy(sortBy: PersonSort) {
            setPreference { it.copy(sortBy = sortBy) }
        }

        suspend fun setPeopleGridItemSize(size: GridThumbnailSize) {
            setPreference { it.copy(gridThumbnailSize = size) }
        }

        suspend fun setShowNames(show: Boolean) {
            setPreference { it.copy(showNames = show) }
        }

        suspend fun setShowMediaCounts(show: Boolean) {
            setPreference { it.copy(showMediaCounts = show) }
        }

        suspend fun setShowClans(show: Boolean) {
            setPreference { it.copy(showClans = show) }
        }

        private suspend fun setPeoplePreference(pref: PeoplePreference) {
            val dbPref = us.mikeandwan.photos.database.PeoplePreference(
                PREFERENCE_ID,
                pref.sortBy,
                pref.gridThumbnailSize,
                pref.showNames,
                pref.showMediaCounts,
                pref.showClans,
            )

            dao.setPeoplePreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: PeoplePreference) -> PeoplePreference) {
            val pref = getPeoplePreference().first()

            setPeoplePreference(update(pref))
        }
    }
