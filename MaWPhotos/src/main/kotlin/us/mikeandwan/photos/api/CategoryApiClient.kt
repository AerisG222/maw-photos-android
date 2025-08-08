package us.mikeandwan.photos.api

import retrofit2.Retrofit
import javax.inject.Inject
import kotlin.uuid.Uuid

class CategoryApiClient @Inject constructor(
    retrofit: Retrofit
): BaseApiClient() {
    private val _categoryApi: CategoryApi by lazy { retrofit.create(CategoryApi::class.java) }

    suspend fun getYears(): ApiResult<List<Int>> {
        return makeApiCall(::getYears.name, suspend { _categoryApi.getYears() })
    }

    suspend fun getCategoriesForYear(year: Int): ApiResult<List<Category>> {
        return makeApiCall(::getCategoriesForYear.name, suspend { _categoryApi.getCategoriesForYear(year) })
    }

    suspend fun getCategory(categoryId: Uuid): ApiResult<Category?> {
        return makeApiCall(::getCategory.name, suspend { _categoryApi.getCategory(categoryId) })
    }

    suspend fun getMediaForCategory(categoryId: Uuid): ApiResult<List<Media>> {
        return makeApiCall(::getMediaForCategory.name, suspend { _categoryApi.getMediaForCategory(categoryId) })
    }

    suspend fun search(query: String, start: Int = 0): ApiResult<SearchResults<Category>> {
        return makeApiCall(::search.name, suspend { _categoryApi.search(query, start) })
    }
}
