package us.mikeandwan.photos.ui.screens.search

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlin.time.Clock
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import org.junit.After
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.SearchPreferenceRepository
import us.mikeandwan.photos.domain.SearchRepository
import us.mikeandwan.photos.domain.models.Category
import us.mikeandwan.photos.domain.models.CategoryDisplayType
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.GridThumbnailSize
import us.mikeandwan.photos.domain.models.SearchPreference

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class SearchViewModelTest {
    private lateinit var searchRepository: SearchRepository
    private lateinit var categoryRepository: CategoryRepository
    private lateinit var searchPreferenceRepository: SearchPreferenceRepository

    private val now = Clock.System.now()

    private val category = Category(
        id = Uuid.random(),
        year = 2024,
        name = "Test Category",
        effectiveDate = now.toLocalDateTime(TimeZone.currentSystemDefault()).date,
        modified = now,
        isFavorite = false,
        teaser = emptyList(),
        mediaTypes = emptyList(),
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        searchRepository = mockk(relaxed = true)
        categoryRepository = mockk(relaxed = true)
        searchPreferenceRepository = mockk(relaxed = true)

        every { searchRepository.searchResults } returns MutableStateFlow(listOf(category))
        every { searchRepository.hasMoreResults } returns MutableStateFlow(false)
        every { searchRepository.activeSearchTerm } returns MutableStateFlow("test")
        every { searchPreferenceRepository.getSearchDisplayType() } returns
            flowOf(CategoryDisplayType.Grid)
        every { searchPreferenceRepository.getSearchGridItemSize() } returns
            flowOf(GridThumbnailSize.Medium)
        every { searchPreferenceRepository.getSearchPreference() } returns
            flowOf(
                SearchPreference(
                    id = 1,
                    recentQueryCountToSave = 20,
                    displayType = CategoryDisplayType.Grid,
                    gridThumbnailSize = GridThumbnailSize.Medium,
                    showMediaTypeIndicator = true,
                    showFavoriteIndicator = true,
                ),
            )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `toggleFavorite saves the new state through the api`() = runTest {
        val updated = category.copy(isFavorite = true)

        every { categoryRepository.setFavorite(category.id, true) } returns
            flowOf(ExternalCallStatus.Loading, ExternalCallStatus.Success(updated))

        val vm = SearchViewModel(
            searchRepository,
            categoryRepository,
            searchPreferenceRepository,
        )

        vm.toggleFavorite(category)
        advanceUntilIdle()

        verify { categoryRepository.setFavorite(category.id, true) }
        verify { searchRepository.updateCategory(updated) }
    }

    @Test
    fun `toggleFavorite clears the favorite when it was already set`() = runTest {
        val favorited = category.copy(isFavorite = true)
        val updated = category.copy(isFavorite = false)

        every { categoryRepository.setFavorite(favorited.id, false) } returns
            flowOf(ExternalCallStatus.Loading, ExternalCallStatus.Success(updated))

        val vm = SearchViewModel(
            searchRepository,
            categoryRepository,
            searchPreferenceRepository,
        )

        vm.toggleFavorite(favorited)
        advanceUntilIdle()

        verify { categoryRepository.setFavorite(favorited.id, false) }
        verify { searchRepository.updateCategory(updated) }
    }
}
