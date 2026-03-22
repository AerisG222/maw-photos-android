package us.mikeandwan.photos.domain

import androidx.collection.LruCache
import androidx.room.withTransaction
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import us.mikeandwan.photos.api.ApiResult
import us.mikeandwan.photos.api.CategoryApiClient
import us.mikeandwan.photos.database.CategoryDao
import us.mikeandwan.photos.database.MawDatabase
import us.mikeandwan.photos.database.ScaleDao
import us.mikeandwan.photos.database.Year
import us.mikeandwan.photos.database.YearDao
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media

class CategoryRepository
    @Inject
    constructor(
        private val api: CategoryApiClient,
        private val db: MawDatabase,
        private val yearDao: YearDao,
        private val catDao: CategoryDao,
        private val scaleDao: ScaleDao,
        private val apiErrorHandler: ApiErrorHandler,
    ) : ICategoryRepository {
        companion object {
            private const val ERR_MSG_LOAD_YEARS =
                "Unable to load years at this time.  Please try again later."
            private const val ERR_MSG_LOAD_CATEGORIES =
                "Unable to load categories at this time.  Please try again later."
            private const val ERR_MSG_LOAD_MEDIA =
                "Unable to load media for the category at this time.  Please try again later."
        }

        private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
        private var cachedCategoryMedia = LruCache<Uuid, List<Media>>(8)

        init {
            scope.launch {
                // background task to load years that haven't pulled categories yet
                // add a delay to avoid hammering the app and API
                yearDao
                    .getYearsNeedingInitialization()
                    .collectLatest { years ->
                        try {
                            years.forEach { year ->
                                delay(2000)

                                loadCategories(year)
                                    .collect { status ->
                                        if (status is ExternalCallStatus.Success) {
                                            yearDao.upsert(listOf(Year(year, true)))
                                        }
                                    }
                            }
                        } catch (e: CancellationException) {
                            // list changed which could be expected based on updating the year a few lines above, swallow
                        }
                    }
            }
        }

        override fun getYears() =
            flow {
                val years = yearDao
                    .getYears()
                    .distinctUntilChanged()

                if (years.first().isEmpty()) {
                    emit(emptyList())
                    loadYears(ERR_MSG_LOAD_YEARS)
                        .collect { }
                }

                emitAll(years)
            }

        override fun getMostRecentYear() = yearDao.getMostRecentYear().distinctUntilChanged()

        override fun getUpdatedCategories() =
            flow {
                val modifyDate = catDao
                    .getMostRecentModifiedDate()
                    .firstOrNull()

                if (modifyDate != null) {
                    emitAll(loadUpdatedCategories(modifyDate))
                } else {
                    emit(ExternalCallStatus.Success(emptyList()))
                }
            }

        override fun getMedia(categoryId: Uuid) =
            flow {
                emit(ExternalCallStatus.Success(emptyList()))

                cachedCategoryMedia[categoryId]?.let {
                    emit(ExternalCallStatus.Success(it))
                    return@flow
                }

                emit(ExternalCallStatus.Loading)

                when (val result = api.getMediaForCategory(categoryId)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_MEDIA))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_MEDIA))
                    }

                    is ApiResult.Success -> {
                        val media = result.result.map { it.toDomainMedia() }

                        if (media.isNotEmpty()) {
                            cachedCategoryMedia.put(categoryId, media)
                        }

                        emit(ExternalCallStatus.Success(media))
                    }
                }
            }

        override fun getCategories(year: Int) =
            flow {
                val cat = catDao
                    .getCategoriesForYear(year)
                    .map { dbList ->
                        dbList.map { dbCat -> dbCat.toDomainCategory() }
                    }.distinctUntilChanged()

                val initialCategories = cat.first()

                if (initialCategories.isEmpty()) {
                    emit(emptyList())
                    loadCategories(year)
                        .collect { }
                } else if (initialCategories.any { it.mediaTypes.isEmpty() }) {
                    scope.launch {
                        loadCategories(year).collect { }
                    }
                }

                emitAll(cat)
            }

        override fun getCategory(categoryId: Uuid) =
            flow {
                val cat = catDao
                    .getCategory(categoryId)
                    .map { dbCat -> dbCat?.toDomainCategory() }
                    .distinctUntilChanged()

                val initialCategory = cat.first()

                if (initialCategory == null) {
                    loadCategory(categoryId)
                        .collect { }
                } else if (initialCategory.mediaTypes.isEmpty()) {
                    scope.launch {
                        loadCategory(categoryId).collect { }
                    }
                }

                emitAll(cat)
            }

        fun loadYears(errorMessage: String?) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getYears()) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, errorMessage))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, errorMessage))
                    }

                    is ApiResult.Success -> {
                        val years = result.result

                        if (years.isEmpty()) {
                            emit(ExternalCallStatus.Success(emptyList()))
                        } else {
                            val dbYears = years.map { Year(it) }

                            db.withTransaction {
                                yearDao.upsert(dbYears)
                            }

                            emit(ExternalCallStatus.Success(dbYears.map { it.year }))
                        }
                    }
                }
            }

        fun loadCategories(year: Int) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getCategoriesForYear(year)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_CATEGORIES))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_CATEGORIES))
                    }

                    is ApiResult.Success -> {
                        val categories = result.result
                        saveCategoriesToDb(categories, listOf(Year(year, true)))
                        emit(ExternalCallStatus.Success(categories))
                    }
                }
            }

        fun loadUpdatedCategories(latestModifyDate: Instant) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getUpdatedCategories(latestModifyDate)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_CATEGORIES))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_CATEGORIES))
                    }

                    is ApiResult.Success -> {
                        val categories = result.result
                        if (categories.isEmpty()) {
                            emit(ExternalCallStatus.Success(emptyList()))
                        } else {
                            val allYears = yearDao.getYears().first()
                            val newYears = categories
                                .map { it.effectiveDate.year }
                                .distinct()
                                .filter { !allYears.contains(it) }
                                .map { Year(it, false) }

                            saveCategoriesToDb(categories, newYears)
                            emit(ExternalCallStatus.Success(categories.map { it.toDomainCategory() }))
                        }
                    }
                }
            }

        fun loadCategory(categoryId: Uuid) =
            flow {
                emit(ExternalCallStatus.Loading)

                when (val result = api.getCategory(categoryId)) {
                    is ApiResult.Error -> {
                        emit(apiErrorHandler.handleError(result, ERR_MSG_LOAD_CATEGORIES))
                    }

                    is ApiResult.Empty -> {
                        emit(apiErrorHandler.handleEmpty(result, ERR_MSG_LOAD_CATEGORIES))
                    }

                    is ApiResult.Success -> {
                        val category = result.result
                        if (category == null) {
                            emit(ExternalCallStatus.Error("Category not found"))
                        } else {
                            saveCategoriesToDb(listOf(category))
                            emit(ExternalCallStatus.Success(category))
                        }
                    }
                }
            }

        private suspend fun saveCategoriesToDb(
            apiCategories: List<us.mikeandwan.photos.api.Category>,
            yearsToUpsert: List<Year> = emptyList(),
        ) {
            if (apiCategories.isEmpty()) return

            val dbCategories = apiCategories.map { it.toDatabaseCategory() }
            val dbMediaFiles = prepareMediaFilesForDatabase(apiCategories)

            db.withTransaction {
                catDao.upsertCategories(dbCategories)
                catDao.upsertMediaFiles(dbMediaFiles)
                if (yearsToUpsert.isNotEmpty()) {
                    yearDao.upsert(yearsToUpsert)
                }
            }
        }

        fun tryUpdateCache(media: Media) {
            val mediaList = cachedCategoryMedia[media.categoryId]

            if (mediaList != null) {
                val updatedList = mediaList.toMutableList()
                val index = updatedList.indexOfFirst { it.id == media.id }

                if (index >= 0) {
                    updatedList[index] = media
                    cachedCategoryMedia.put(media.categoryId, updatedList)
                }
            }
        }

        private fun us.mikeandwan.photos.api.Category.toDatabaseCategory(): us.mikeandwan.photos.database.Category =
            us.mikeandwan.photos.database.Category(
                this.id,
                this.effectiveDate.year,
                this.name,
                this.effectiveDate,
                this.modified,
                this.isFavorite,
                this.mediaTypes,
            )

        suspend fun prepareMediaFilesForDatabase(
            categories: List<us.mikeandwan.photos.api.Category>,
        ): List<us.mikeandwan.photos.database.MediaFile> {
            val scales = scaleDao
                .getScales()
                .first()
                .associateBy { it.code }

            return categories.flatMap { category ->
                category.teaser.files.map { file ->
                    val scale = scales[file.scale]
                        ?: throw IllegalArgumentException("Scale ${file.scale} not found in database")

                    us.mikeandwan.photos.database.MediaFile(
                        category.id,
                        scale.id,
                        file.type,
                        file.path,
                    )
                }
            }
        }
    }
