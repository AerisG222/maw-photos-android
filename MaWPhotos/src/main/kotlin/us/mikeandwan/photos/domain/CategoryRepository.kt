package us.mikeandwan.photos.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.database.CategoryDao
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.Year
import us.mikeandwan.photos.database.YearDao
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.Media
import javax.inject.Inject
import kotlin.collections.map
import kotlin.uuid.Uuid

class CategoryRepository @Inject constructor(
    private val api: CategoryApiClient,
    private val db: MawDatabase,
    private val yearDao: YearDao,
    private val catDao: CategoryDao,
    private val apiErrorHandler: ApiErrorHandler
) : ICategoryRepository {
    companion object {
        private const val ERR_MSG_LOAD_CATEGORIES = "Unable to load categories at this time.  Please try again later."
    }

    override fun getYears() = flow {
        val years = yearDao
            .getYears()

        if(years.first().isEmpty()) {
            emit(emptyList())
            loadYears(ERR_MSG_LOAD_CATEGORIES)
                .collect { }
        }

        emitAll(years)
    }

    override fun getMostRecentYear() = yearDao.getMostRecentYear()

    override fun getNewCategories() = flow {
        val modifyDate = catDao
            .getMostRecentModifiedDate()
            .firstOrNull()

        //val categories = loadCategories(category?.id ?: -1, null)

        //emitAll(categories)
        emit(ExternalCallStatus.Success(emptyList<Category>()))
    }

    private suspend fun fetchCategories(repository: ICategoryRepository): ExternalCallStatus<List<Category>> {
        return try {
            repository.getNewCategories().first { it !is ExternalCallStatus.Loading }
        } catch (e: NoSuchElementException) {
            ExternalCallStatus.Error("No categories emitted from repository.")
        }
    }

    override fun getMedia(categoryId: Uuid) = flow {
        emit(ExternalCallStatus.Success<List<Media>>(emptyList()))
//        cachedCategoryPhotos[categoryId]?.let {
//            emit(ExternalCallStatus.Success(it))
//            return@flow
//        }
//
//        emit(ExternalCallStatus.Loading)
//
//        when(val result = api.getPhotos(categoryId)) {
//            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_PHOTOS))
//            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_PHOTOS))
//            is ApiResult.Success -> {
//                val photos = result.result.items.map { it.toDomainPhoto() }
//
//                if (photos.isNotEmpty()) {
//                    cachedCategoryPhotos.put(categoryId, photos)
//                }
//
//                emit(ExternalCallStatus.Success(photos))
//            }
//        }
    }

    override fun getCategories(year: Int) = catDao
        .getCategoriesForYear(year)
        .map { dbList ->
            dbList.map { dbCat -> dbCat.toDomainCategory() }
        }

    override fun getCategory(categoryId: Uuid): Flow<Category> {
        throw NotImplementedError("Please use a specific media type repo to load individual categories")
    }

    fun loadYears(errorMessage: String?) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.getYears()) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, errorMessage))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, errorMessage))
            is ApiResult.Success -> {
                val years = result.result

                if(years.isEmpty()) {
                    emit(ExternalCallStatus.Success(emptyList()))
                } else {
                    val dbYears = years.map { y -> Year(y) }

                    db.withTransaction {
                        yearDao.upsert(*dbYears.toTypedArray())
                    }

                    emit(ExternalCallStatus.Success(dbYears.map { it.year }))
                }
            }
        }
    }
}
