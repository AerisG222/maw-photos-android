package us.mikeandwan.photos.domain

import javax.inject.Inject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.CategoryPreferenceDao
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.CategoryPreference
import us.mikeandwan.photos.domain.models.GridThumbnailSize

class CategoryPreferenceRepository
    @Inject
    constructor(
        private val dao: CategoryPreferenceDao,
    ) {
        companion object {
            private const val PREFERENCE_ID = 1
        }

        fun getCategoryPreference() =
            dao
                .getCategoryPreference(PREFERENCE_ID)
                .map { it.toDomainCategoryPreference() }

        fun getCategoryDisplayType() =
            dao
                .getCategoryPreference(PREFERENCE_ID)
                .map { it.displayType }

        fun getCategoryGridItemSize() =
            dao
                .getCategoryPreference(PREFERENCE_ID)
                .map { it.gridThumbnailSize }

        suspend fun setCategoryDisplayType(displayType: CategoryDisplayType) {
            setPreference { it.copy(displayType = displayType) }
        }

        suspend fun setCategoryGridItemSize(size: GridThumbnailSize) {
            setPreference { it.copy(gridThumbnailSize = size) }
        }

        suspend fun setShowMediaTypeIndicator(show: Boolean) {
            setPreference { it.copy(showMediaTypeIndicator = show) }
        }

        private suspend fun setCategoryPreference(pref: CategoryPreference) {
            val dbPref = us.mikeandwan.photos.database.CategoryPreference(
                PREFERENCE_ID,
                pref.displayType,
                pref.gridThumbnailSize,
                pref.showMediaTypeIndicator,
            )

            dao.setCategoryPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: CategoryPreference) -> CategoryPreference) {
            val pref = getCategoryPreference().first()

            setCategoryPreference(update(pref))
        }
    }
