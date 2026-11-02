package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.MediaFilters

@Singleton
class CategoryPreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getCategoryPreference() = dataStore.select { it.category }

        fun getCategoryDisplayType() = dataStore.select { it.category.displayType }

        suspend fun setCategoryDisplayType(displayType: CategoryDisplayType) {
            setPreference { it.copy(displayType = displayType) }
        }

        suspend fun setFilters(filters: MediaFilters) {
            setPreference { it.copy(filters = filters) }
        }

        private suspend fun setPreference(update: (pref: CategoryPreference) -> CategoryPreference) {
            dataStore.updateData { it.copy(category = update(it.category)) }
        }
    }
