package us.mikeandwan.photos.domain

import androidx.collection.LruCache
import androidx.room.withTransaction
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
        private const val ERR_MSG_LOAD_MEDIA = "Unable to load media for the category at this time.  Please try again later."
    }

    private var cachedCategoryMedia = LruCache<Uuid, List<Media>>(8)

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

        cachedCategoryMedia[categoryId]?.let {
            emit(ExternalCallStatus.Success(it))
            return@flow
        }

        emit(ExternalCallStatus.Loading)

        when(val result = api.getMediaForCategory(categoryId)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_MEDIA))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_MEDIA))
            is ApiResult.Success -> {
                val media = result.result.map { it.toDomainMedia() }

                if (media.isNotEmpty()) {
                    cachedCategoryMedia.put(categoryId, media)
                }

                emit(ExternalCallStatus.Success(media))
            }
        }
    }

    override fun getCategories(year: Int) = flow {
        val cat = catDao
            .getCategoriesForYear(year)
            .map { dbList ->
                dbList.map { dbCat -> dbCat.toDomainCategory() }
            }

        if(cat.first().isEmpty()) {
            emit(emptyList())
            loadCategories(year)
                .collect { }
        }

        emitAll(cat)
    }

    override fun getCategory(categoryId: Uuid) = flow {
        val cat = catDao
            .getCategory(categoryId)
            .map { dbCat -> dbCat?.toDomainCategory() }

        if(cat.first() == null) {
            loadCategory(categoryId)
                .filterNotNull()
                .collect { }
        }

        emitAll(cat)
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

    fun loadCategory(categoryId: Uuid) = flow {
        emit(ExternalCallStatus.Loading)

        when(val result = api.getCategory(categoryId)) {
            is ApiResult.Error -> emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_CATEGORIES))
            is ApiResult.Empty -> emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_CATEGORIES))
            is ApiResult.Success -> {
                val category = result.result

                if(category == null) {
                    emit(ExternalCallStatus.Error("Category not found"))
                } else {
                    val dbCategory = category.toDatabaseCategory()
                    val dbMediaFiles = prepareMediaFilesForDatabase(listOf(category))

                    db.withTransaction {
                        catDao.upsertCategories(dbCategory)
                        catDao.upsertMediaFiles(*dbMediaFiles.toTypedArray())
                    }

                    emit(ExternalCallStatus.Success(category))
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
