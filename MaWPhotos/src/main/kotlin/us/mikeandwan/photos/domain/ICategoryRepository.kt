package us.mikeandwan.photos.domain

import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.Flow
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media

interface ICategoryRepository {
    fun getYears(): Flow<List<Int>>

    fun getMostRecentYear(): Flow<Int?>

    fun getUpdatedCategories(): Flow<ExternalCallStatus<List<Category>>>

    fun getCategories(year: Int): Flow<List<Category>>

    fun getCategory(categoryId: Uuid): Flow<Category?>

    fun getMedia(categoryId: Uuid): Flow<ExternalCallStatus<List<Media>>>
}
