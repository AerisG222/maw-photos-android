package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.RandomPreferenceDao
import us.mikeandwan.photos.domain.models.RandomPreference

@Singleton
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

        suspend fun setSlideshowIntervalSeconds(seconds: Int) {
            setPreference { it.copy(slideshowIntervalSeconds = seconds) }
        }

        suspend fun setShowWidgetInfo(show: Boolean) {
            setPreference { it.copy(showWidgetInfo = show) }
        }

        private suspend fun setRandomPreferences(pref: RandomPreference) {
            val dbPref = us.mikeandwan.photos.database.RandomPreference(
                id = PREFERENCE_ID,
                slideshowIntervalSeconds = pref.slideshowIntervalSeconds,
                showWidgetInfo = pref.showWidgetInfo,
            )

            dao.setRandomPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: RandomPreference) -> RandomPreference) {
            val pref = getRandomPreferences().first()

            setRandomPreferences(update(pref))
        }
    }
