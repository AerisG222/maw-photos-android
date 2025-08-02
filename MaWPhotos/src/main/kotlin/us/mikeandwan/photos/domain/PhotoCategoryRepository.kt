package us.mikeandwan.photos.domain

import androidx.collection.LruCache
import androidx.room.withTransaction
import kotlinx.coroutines.flow.*
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.PhotoApiClient
import us.mikeandwan.photos.database.*
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Photo
import javax.inject.Inject

class PhotoCategoryRepository @Inject constructor(
    private val api: PhotoApiClient,
    private val db: MawDatabase,
    private val pcDao: PhotoCategoryDao,
    private val apiErrorHandler: ApiErrorHandler
) : ICategoryRepository {
    companion object {
        private const val ERR_MSG_LOAD_CATEGORIES = "Unable to load categories at this time.  Please try again later."
        private const val ERR_MSG_LOAD_PHOTOS = "Unable to load photos.  Please try again later."
    }

    private var cachedCategoryPhotos = LruCache<Int, List<Photo>>(8)

    override fun getYears() = flow {
        val data = pcDao.getYears()

        if(data.first().isEmpty()) {
            emit(emptyList())
            loadCategories(-1, ERR_MSG_LOAD_CATEGORIES)
                .collect { }
        }

        emitAll(data)
    }

    override fun getMostRecentYear() = pcDao.getMostRecentYear()

    override fun getNewCategories() = flow {
        val category = pcDao
            .getMostRecentCategory()
            .firstOrNull()

        val categories = loadCategories(category?.id ?: -1, null)

        emitAll(categories)
    }

    override fun getCategories(year: Int) = pcDao
        .getCategoriesForYear(year)
        .map { dbList ->
            dbList.map { dbCat -> dbCat.toDomainMediaCategory() }
        }

    override fun getCategory(categoryId: Int) = pcDao
        .getCategory(categoryId)
        .filterNotNull()
        .map { cat -> cat.toDomainMediaCategory() }

    override fun getMedia(categoryId: Int) = flow {
        cachedCategoryPhotos[categoryId]?.let {
            emit(ExternalCallStatus.Success(it))
            return@flow
        }

        emit(ExternalCallStatus.Loading)

        when(val result = api.getPhotos(categoryId)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_PHOTOS))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_PHOTOS))
            is ApiResult.Success -> {
                val photos = result.result.items.map { it.toDomainPhoto() }

                if (photos.isNotEmpty()) {
                    cachedCategoryPhotos.put(categoryId, photos)
                }

                emit(ExternalCallStatus.Success(photos))
            }
        }
    }

    private fun loadCategories(mostRecentCategory: Int, errorMessage: String?) = flow {
        emit(ExternalCallStatus.Loading)
//
//        when(val result = api.getRecentCategories(mostRecentCategory)) {
//            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, errorMessage))
//            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, errorMessage))
//            is ApiResult.Success -> {
//                val categories = result.result.items
//
//                    emit(ExternalCallStatus.Success(emptyList()))
////                if(categories.isEmpty()) {
////                } else {
////                    val dbCategories = categories.map { apiCat -> apiCat.toDatabasePhotoCategory() }
////
////                    db.withTransaction {
////                        pcDao.upsert(*dbCategories.toTypedArray())
////                    }
////
////                    emit(ExternalCallStatus.Success(dbCategories.map { it.toDomainMediaCategory() }))
////                }
//            }
//        }
    }
}
