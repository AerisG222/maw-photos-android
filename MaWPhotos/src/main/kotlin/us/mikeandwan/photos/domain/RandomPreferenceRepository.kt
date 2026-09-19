package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.RandomPreference

@Singleton
class RandomPreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getRandomPreferences() = dataStore.select { it.random }

        fun getSlideshowIntervalSeconds() = dataStore.select { it.random.slideshowIntervalSeconds }

        suspend fun setSlideshowIntervalSeconds(seconds: Int) {
            setPreference { it.copy(slideshowIntervalSeconds = seconds) }
        }

        suspend fun setShowWidgetInfo(show: Boolean) {
            setPreference { it.copy(showWidgetInfo = show) }
        }

        private suspend fun setPreference(update: (pref: RandomPreference) -> RandomPreference) {
            dataStore.updateData { it.copy(random = update(it.random)) }
        }
    }
