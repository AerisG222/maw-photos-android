package us.mikeandwan.photos.ui.screens.category

import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.CategoryRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.Media
import us.mikeandwan.photos.domain.models.MediaFilters
import us.mikeandwan.photos.domain.models.MediaType

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CategoryViewModelTest {
    private val categoryId = Uuid.random()
    private val photo = Media(Uuid.random(), categoryId, MediaType.Photo, isFavorite = false)
    private val favoriteVideo = Media(Uuid.random(), categoryId, MediaType.Video, isFavorite = true)

    private lateinit var categoryRepository: CategoryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        categoryRepository = mockk(relaxed = true)
        every { categoryRepository.getCategory(categoryId) } returns flowOf()
        every { categoryRepository.getMedia(categoryId) } returns
            flowOf(ExternalCallStatus.Success(listOf(photo, favoriteVideo)))
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // the same list next, previous and the slideshow walk, so this is also what keeps them to the
    // filtered media
    @Test
    fun `filters narrow what the grid and the steps through it see`() = runTest {
        val vm = CategoryViewModel(categoryRepository, mockk(relaxed = true))
        val subscription = backgroundScope.launch { vm.uiState.collect { } }

        vm.initState(categoryId)
        advanceUntilIdle()

        assertEquals(listOf(photo, favoriteVideo), vm.filteredMedia.value)

        vm.setFilters(MediaFilters(favoritesOnly = true))
        advanceUntilIdle()

        assertEquals(listOf(favoriteVideo), vm.filteredMedia.value)
        assertEquals(1, vm.uiState.value.gridItems.size)
        assertEquals(MediaFilters(favoritesOnly = true), vm.uiState.value.filters)

        vm.setFilters(MediaFilters(mediaType = MediaType.Photo))
        advanceUntilIdle()

        assertEquals(listOf(photo), vm.filteredMedia.value)

        // what was loaded is untouched, so clearing needs no round trip
        vm.setFilters(MediaFilters())
        advanceUntilIdle()

        assertEquals(listOf(photo, favoriteVideo), vm.filteredMedia.value)

        subscription.cancel()
    }
}
