package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.RandomPreferenceDao
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.RandomPreference

class RandomPreferenceRepository
    @Inject
    constructor(
        private val dao: RandomPreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getRandomPreferences() =
            dao
                .getRandomPreference(PREFERENCE_ID)
                .map { it.toDomainRandomPreference() }

        fun getSlideshowIntervalSeconds() =
            dao
                .getRandomPreference(PREFERENCE_ID)
                .map { it.slideshowIntervalSeconds }

        fun getPhotoGridItemSize() =
            dao
                .getRandomPreference(PREFERENCE_ID)
                .map { it.gridThumbnailSize }

        suspend fun setSlideshowIntervalSeconds(seconds: Int) {
            setPreference { it.copy(slideshowIntervalSeconds = seconds) }
        }

        suspend fun setPhotoGridItemSize(size: GridThumbnailSize) {
            setPreference { it.copy(gridThumbnailSize = size) }
        }

        suspend fun setShowMediaTypeIndicator(show: Boolean) {
            setPreference { it.copy(showMediaTypeIndicator = show) }
        }

        suspend fun setShowFavoriteIndicator(show: Boolean) {
            setPreference { it.copy(showFavoriteIndicator = show) }
        }

        suspend fun setShowWidgetInfo(show: Boolean) {
            setPreference { it.copy(showWidgetInfo = show) }
        }

        private suspend fun setRandomPreferences(pref: RandomPreference) {
            val dbPref = us.mikeandwan.photos.database.RandomPreference(
                id = PREFERENCE_ID,
                slideshowIntervalSeconds = pref.slideshowIntervalSeconds,
                gridThumbnailSize = pref.gridThumbnailSize,
                showMediaTypeIndicator = pref.showMediaTypeIndicator,
                showFavoriteIndicator = pref.showFavoriteIndicator,
                showWidgetInfo = pref.showWidgetInfo,
            )

            dao.setRandomPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: RandomPreference) -> RandomPreference) {
            val pref = getRandomPreferences().first()

            setRandomPreferences(update(pref))
        }
    }
