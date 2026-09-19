package us.mikeandwan.photos.domain

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.CategoryPreferenceDao
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.CategoryPreference

@Singleton
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

        suspend fun setCategoryDisplayType(displayType: CategoryDisplayType) {
            setPreference { it.copy(displayType = displayType) }
        }

        private suspend fun setCategoryPreference(pref: CategoryPreference) {
            val dbPref = us.mikeandwan.photos.database.CategoryPreference(
                PREFERENCE_ID,
                pref.displayType,
            )

            dao.setCategoryPreference(dbPref)
        }

        private suspend fun setPreference(update: (pref: CategoryPreference) -> CategoryPreference) {
            val pref = getCategoryPreference().first()

            setCategoryPreference(update(pref))
        }
    }
