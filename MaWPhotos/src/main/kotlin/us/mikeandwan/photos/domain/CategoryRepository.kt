package us.mikeandwan.photos.domain

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.database.CategoryDao
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.ScaleDao
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
    private val scaleDao: ScaleDao,
    private val apiErrorHandler: ApiErrorHandler
) : ICategoryRepository {
    companion object {
        private const val ERR_MSG_LOAD_YEARS = "Unable to load years at this time.  Please try again later."
        private const val ERR_MSG_LOAD_CATEGORIES = "Unable to load categories at this time.  Please try again later."
    }

    override fun getYears() = flow {
        val years = yearDao
            .getYears()

        if(years.first().isEmpty()) {
            emit(emptyList())
            loadYears(ERR_MSG_LOAD_YEARS)
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

    override fun getCategory(categoryId: Uuid): Flow<Category> = catDao
        .getCategory(categoryId)
        .filterNotNull()
        .map { dbCat -> dbCat.toDomainCategory() }

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

    fun loadCategories(year: Int) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.getCategoriesForYear(year)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_CATEGORIES))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_CATEGORIES))
            is ApiResult.Success -> {
                val categories = result.result

                if(categories.isEmpty()) {
                    emit(ExternalCallStatus.Success(emptyList()))
                } else {
                    val dbCategories = categories.map { apiCat -> apiCat.toDatabaseCategory() }
                    val dbMediaFiles = prepareMediaFilesForDatabase(categories)

                    db.withTransaction {
                        catDao.upsertCategories(*dbCategories.toTypedArray())
                        catDao.upsertMediaFiles(*dbMediaFiles.toTypedArray())
                    }

                    emit(ExternalCallStatus.Success(categories))
                }
            }
        }
    }

    fun us.mikeandwan.photos.api.Category.toDatabaseCategory(): us.mikeandwan.photos.database.Category {
        return us.mikeandwan.photos.database.Category(
            this.id,
            this.effectiveDate.year,
            this.name,
            this.effectiveDate,
            this.modified,
            this.isFavorite
        )
    }

    suspend fun prepareMediaFilesForDatabase(categories: List<us.mikeandwan.photos.api.Category>): List<us.mikeandwan.photos.database.MediaFile> {
        val result = mutableListOf<us.mikeandwan.photos.database.MediaFile>()
        val scales = scaleDao
            .getScales()
            .first()

        for (category in categories) {
            val adaptedFiles = buildMediaFiles(category, scales)
            result.addAll(adaptedFiles)
        }
        
        return result
    }
    
    fun buildMediaFiles(category: us.mikeandwan.photos.api.Category, scales: List<us.mikeandwan.photos.database.Scale>): List<us.mikeandwan.photos.database.MediaFile> {
        val result = mutableListOf<us.mikeandwan.photos.database.MediaFile>()

        for(file in category.teaser.files) {
            val scale = scales.firstOrNull { it.code == file.scale }

            if (scale == null) {
                throw IllegalArgumentException("Scale ${file.scale} not found in database")
            }

            result.add(
                us.mikeandwan.photos.database.MediaFile(
                    category.id,
                    scale.id,
                    file.type,
                    file.path
                )
            )
        }

        return result
    }
}
