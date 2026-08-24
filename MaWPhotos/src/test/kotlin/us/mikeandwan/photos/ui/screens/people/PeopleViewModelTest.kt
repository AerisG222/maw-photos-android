package us.mikeandwan.photos.ui.screens.people

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlin.uuid.Uuid
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.PersonSort

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {
    private lateinit var peopleRepository: PeopleRepository
    private lateinit var peoplePreferenceRepository: PeoplePreferenceRepository

    private val alice = person("Alice", mediaCount = 5)
    private val bob = person("bob", mediaCount = 90, isFavorite = true)
    private val carol = person("Carol", mediaCount = 40)

    private val preference = MutableStateFlow(PeoplePreference())

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        peopleRepository = mockk(relaxed = true)
        peoplePreferenceRepository = mockk(relaxed = true)

        every { peopleRepository.people } returns MutableStateFlow(listOf(carol, alice, bob))
        every { peopleRepository.getPeople(any()) } returns
            flowOf(ExternalCallStatus.Success(listOf(carol, alice, bob)))
        every { peoplePreferenceRepository.getPeoplePreference() } returns preference
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sorts by name with favorites first`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        // bob is favorited, so he leads despite the lowercase name sorting after the others
        assertEquals(listOf("bob", "Alice", "Carol"), vm.uiState.value.people.map { it.name })
    }

    @Test
    fun `sorts by media count with favorites still first`() = runTest {
        preference.value = PeoplePreference(sortBy = PersonSort.MediaCount)

        val vm = viewModel()

        advanceUntilIdle()

        assertEquals(listOf("bob", "Carol", "Alice"), vm.uiState.value.people.map { it.name })
    }

    @Test
    fun `filters on any part of the name, ignoring case`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.setFilter("ol")
        advanceUntilIdle()

        assertEquals(listOf("Carol"), vm.uiState.value.people.map { it.name })
    }

    @Test
    fun `an empty filter shows everyone again`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.setFilter("  ")
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.people.size)
    }

    // told apart so the screen can say nobody is identified yet rather than nothing matched
    @Test
    fun `a filter that matches nobody still reports that people exist`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.setFilter("zzz")
        advanceUntilIdle()

        assertEquals(true, vm.uiState.value.people.isEmpty())
        assertEquals(true, vm.uiState.value.hasAnyPeople)
    }

    @Test
    fun `toggling sort writes the other ordering to preferences`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.toggleSort()
        advanceUntilIdle()

        coVerify { peoplePreferenceRepository.setSortBy(PersonSort.MediaCount) }
    }

    @Test
    fun `toggling a favorite sends the opposite of what the person holds`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.toggleFavorite(bob)
        advanceUntilIdle()

        coVerify { peopleRepository.setIsFavorite(bob, false) }
    }

    @Test
    fun `refreshing asks the api rather than the cache`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.refresh()
        advanceUntilIdle()

        coVerify { peopleRepository.getPeople(true) }
    }

    private fun viewModel() = PeopleViewModel(peopleRepository, peoplePreferenceRepository)

    private fun person(
        name: String,
        mediaCount: Int,
        isFavorite: Boolean = false,
    ) = Person(
        id = Uuid.random(),
        name = name,
        preferredFaceUrl = null,
        mediaCount = mediaCount,
        isFavorite = isFavorite,
    )
}
