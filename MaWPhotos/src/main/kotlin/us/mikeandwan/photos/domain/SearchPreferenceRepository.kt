package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.SearchPreferenceDao
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.SearchPreference

@Singleton
class SearchPreferenceRepository
    @Inject
    constructor(
        private val dao: SearchPreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getSearchesToSaveCount() =
            dao
                .getSearchPreference(PREFERENCE_ID)
                .map { it.recentQueryCount }

        fun getSearchDisplayType() =
            dao
                .getSearchPreference(PREFERENCE_ID)
                .map { it.displayType }

        fun getSearchPreference() =
            dao
                .getSearchPreference(PREFERENCE_ID)
                .map { it.toDomainSearchPreference() }

        suspend fun setSearchesToSaveCount(count: Int) {
            setPreference { it.copy(recentQueryCountToSave = count) }
        }

        suspend fun setSearchDisplayType(mode: CategoryDisplayType) {
            setPreference { it.copy(displayType = mode) }
        }

        private fun getSearchPreferences() =
            dao
                .getSearchPreference(PREFERENCE_ID)
                .map { it.toDomainSearchPreference() }

        private suspend fun setSearchPreferences(pref: SearchPreference) {
            val dbPref = us.mikeandwan.photos.database.SearchPreference(
                PREFERENCE_ID,
                pref.recentQueryCountToSave,
                pref.displayType,
            )

            dao.setSearchPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: SearchPreference) -> SearchPreference) {
            val pref = getSearchPreferences().first()

            setSearchPreferences(update(pref))
        }
    }
