package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.PersonSort

@Singleton
class PeoplePreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getPeoplePreference() = dataStore.select { it.people }

        suspend fun setSortBy(sortBy: PersonSort) {
            setPreference { it.copy(sortBy = sortBy) }
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

        suspend fun setShowCategoryYear(show: Boolean) {
            setPreference { it.copy(showCategoryYear = show) }
        }

        suspend fun setShowCategoryTitle(show: Boolean) {
            setPreference { it.copy(showCategoryTitle = show) }
        }

        private suspend fun setPreference(update: (pref: PeoplePreference) -> PeoplePreference) {
            dataStore.updateData { it.copy(people = update(it.people)) }
        }
    }
