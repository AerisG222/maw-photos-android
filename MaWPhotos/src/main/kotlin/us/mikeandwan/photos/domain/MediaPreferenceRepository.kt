package us.mikeandwan.photos.domain

import androidx.datastore.core.DataStore
import javax.inject.Inject
import javax.inject.Singleton
import us.mikeandwan.photos.datastore.UserPreferences
import us.mikeandwan.photos.datastore.select
import us.mikeandwan.photos.domain.models.MediaPreference

@Singleton
class MediaPreferenceRepository
    @Inject
    constructor(
        private val dataStore: DataStore<UserPreferences>,
    ) {
        fun getSlideshowIntervalSeconds() = dataStore.select { it.media.slideshowIntervalSeconds }

        fun getMediaPreference() = dataStore.select { it.media }

        suspend fun setSlideshowIntervalSeconds(seconds: Int) {
            setPreference { it.copy(slideshowIntervalSeconds = seconds) }
        }

        suspend fun setShowFaceHighlights(show: Boolean) {
            setPreference { it.copy(showFaceHighlights = show) }
        }

        private suspend fun setPreference(update: (pref: MediaPreference) -> MediaPreference) {
            dataStore.updateData { it.copy(media = update(it.media)) }
        }
    }
