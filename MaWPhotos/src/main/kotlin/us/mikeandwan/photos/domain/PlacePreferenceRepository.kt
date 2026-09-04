package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.PlacePreferenceDao
import us.mikeandwan.photos.domain.models.PlacePreference

class PlacePreferenceRepository
    @Inject
    constructor(
        private val dao: PlacePreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getPlacePreference() =
            dao
                .getPlacePreference(PREFERENCE_ID)
                .map { it.toDomainPlacePreference() }

        suspend fun setShowCategoryYear(show: Boolean) {
            setPreference { it.copy(showCategoryYear = show) }
        }

        suspend fun setShowCategoryTitle(show: Boolean) {
            setPreference { it.copy(showCategoryTitle = show) }
        }

        private suspend fun setPreference(update: (pref: PlacePreference) -> PlacePreference) {
            val pref = update(getPlacePreference().first())

            dao.setPlacePreference(
                us.mikeandwan.photos.database.PlacePreference(
                    PREFERENCE_ID,
                    pref.showCategoryYear,
                    pref.showCategoryTitle,
                ),
            )
        }
    }
