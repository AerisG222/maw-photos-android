package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.SearchPreference

@Singleton
class SearchPreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getSearchesToSaveCount() = dataStore.select { it.search.recentQueryCountToSave }

        fun getSearchDisplayType() = dataStore.select { it.search.displayType }

        fun getSearchPreference() = dataStore.select { it.search }

        suspend fun setSearchesToSaveCount(count: Int) {
            setPreference { it.copy(recentQueryCountToSave = count) }
        }

        suspend fun setSearchDisplayType(mode: CategoryDisplayType) {
            setPreference { it.copy(displayType = mode) }
        }

        private suspend fun setPreference(update: (pref: SearchPreference) -> SearchPreference) {
            dataStore.updateData { it.copy(search = update(it.search)) }
        }
    }
