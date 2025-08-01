package us.mikeandwan.photos.domain

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.database.MediaCategoryDao
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaCategory
import us.mikeandwan.photos.domain.models.MediaType
import javax.inject.Inject

class MediaCategoryRepository @Inject constructor(
    private val mcDao: MediaCategoryDao,
    private val pcRepo: PhotoCategoryRepository
) : ICategoryRepository {
    override fun getYears() = flow {
        val years = mcDao.getYears()

        if(years.first().isEmpty()) {
            emit(emptyList())
            getNewCategories()
                .collect { }
        }

        emitAll(years)
    }

    override fun getMostRecentYear() = mcDao.getMostRecentYear()

    override fun getNewCategories() = flow {
        emit(ExternalCallStatus.Loading)

        val photoCategoriesResult = fetchCategories(pcRepo)
        val combinedCategories = mutableListOf<MediaCategory>()

        if (photoCategoriesResult is ExternalCallStatus.Success) {
            combinedCategories.addAll(photoCategoriesResult.result)
        }

        val result = when {
            photoCategoriesResult is ExternalCallStatus.Error ->
                ExternalCallStatus.Error("Unable to load all categories.")
            else ->
                ExternalCallStatus.Success(combinedCategories)
        }

        emit(result)
    }

    private suspend fun fetchCategories(repository: ICategoryRepository): ExternalCallStatus<List<MediaCategory>> {
        return try {
            repository.getNewCategories().first { it !is ExternalCallStatus.Loading }
        } catch (e: NoSuchElementException) {
            ExternalCallStatus.Error("No categories emitted from repository.")
        }
    }

    override fun getCategories(year: Int) = mcDao
        .getCategoriesForYear(year)
        .map { dbList ->
            dbList.map { dbCat -> dbCat.toDomainMediaCategory() }
        }

    override fun getCategory(categoryId: Int): Flow<MediaCategory> {
        throw NotImplementedError("Please use a specific media type repo to load individual categories")
    }

    fun getCategory(type: MediaType, id: Int): Flow<MediaCategory> {
        val repo = getRepo(type)

        return repo.getCategory(id)
    }

    override fun getMedia(categoryId: Int): Flow<ExternalCallStatus<List<Media>>> {
        throw NotImplementedError("Please use a specific media type repo to load media")
    }

    fun getMedia(type: MediaType, categoryId: Int): Flow<ExternalCallStatus<List<Media>>> {
        val repo = getRepo(type)

        return repo.getMedia(categoryId)
    }

    private fun getRepo(type: MediaType): ICategoryRepository {
        return when(type) {
            MediaType.Photo -> pcRepo
        }
    }
}
