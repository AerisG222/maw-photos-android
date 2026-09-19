package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.MediaPreferenceDao
import us.mikeandwan.photos.domain.models.MediaPreference

@Singleton
class MediaPreferenceRepository
    @Inject
    constructor(
        private val dao: MediaPreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getSlideshowIntervalSeconds() =
            dao
                .getPhotoPreference(PREFERENCE_ID)
                .map { it.slideshowIntervalSeconds }

        fun getMediaPreference() =
            dao
                .getPhotoPreference(PREFERENCE_ID)
                .map { it.toDomainPhotoPreference() }

        suspend fun setSlideshowIntervalSeconds(seconds: Int) {
            setPreference { it.copy(slideshowIntervalSeconds = seconds) }
        }

        suspend fun setShowFaceHighlights(show: Boolean) {
            setPreference { it.copy(showFaceHighlights = show) }
        }

        private fun getPhotoPreferences() =
            dao
                .getPhotoPreference(PREFERENCE_ID)
                .map { it.toDomainPhotoPreference() }

        private suspend fun setPhotoPreferences(pref: MediaPreference) {
            val dbPref = us.mikeandwan.photos.database.MediaPreference(
                PREFERENCE_ID,
                pref.slideshowIntervalSeconds,
                pref.showFaceHighlights,
            )

            dao.setPhotoPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: MediaPreference) -> MediaPreference) {
            val pref = getPhotoPreferences().first()

            setPhotoPreferences(update(pref))
        }
    }
