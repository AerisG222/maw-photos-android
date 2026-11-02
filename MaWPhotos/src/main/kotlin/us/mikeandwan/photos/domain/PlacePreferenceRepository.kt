package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.MediaFeedOptions
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.PlacePreference

@Singleton
class PlacePreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getPlacePreference() = dataStore.select { it.place }

        fun getFeedOptions() = dataStore.select { MediaFeedOptions(it.place.feedFilters, it.place.feedShuffled) }

        suspend fun setShowCategoryYear(show: Boolean) {
            setPreference { it.copy(showCategoryYear = show) }
        }

        suspend fun setShowCategoryTitle(show: Boolean) {
            setPreference { it.copy(showCategoryTitle = show) }
        }

        suspend fun setFeedFilters(filters: MediaFilters) {
            setPreference { it.copy(feedFilters = filters) }
        }

        suspend fun setFeedShuffled(shuffled: Boolean) {
            setPreference { it.copy(feedShuffled = shuffled) }
        }

        private suspend fun setPreference(update: (pref: PlacePreference) -> PlacePreference) {
            dataStore.updateData { it.copy(place = update(it.place)) }
        }
    }
