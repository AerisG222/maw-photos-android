package us.mikeandwan.photos.ui.screens.people

import io.mockk.coEvery
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.ClanResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.PersonSort

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class PeopleViewModelTest {
    private lateinit var peopleRepository: PeopleRepository
    private lateinit var peoplePreferenceRepository: PeoplePreferenceRepository
    private lateinit var clanRepository: ClanRepository

    private val alice = person("Alice", mediaCount = 5)
    private val bob = person("bob", mediaCount = 90, isFavorite = true)
    private val carol = person("Carol", mediaCount = 40)

    private val preference = MutableStateFlow(PeoplePreference())
    private val clans = MutableStateFlow<List<Clan>>(emptyList())

    @Before
    fun setUp() {
        Dispatchers.setMain(StandardTestDispatcher())

        peopleRepository = mockk(relaxed = true)
        peoplePreferenceRepository = mockk(relaxed = true)
        clanRepository = mockk(relaxed = true)

        every { peopleRepository.people } returns MutableStateFlow(listOf(carol, alice, bob))
        every { peopleRepository.getPeople(any()) } returns
            flowOf(ExternalCallStatus.Success(listOf(carol, alice, bob)))
        every { peoplePreferenceRepository.getPeoplePreference() } returns preference
        every { clanRepository.clans } returns clans
        every { clanRepository.getClans(any()) } returns
            flowOf(ExternalCallStatus.Success(emptyList()))
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

    // CLANS

    @Test
    fun `picking for a new clan starts from an empty selection`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        advanceUntilIdle()

        assertEquals(ClanPicking.Create, vm.uiState.value.picking)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
        assertTrue(vm.uiState.value.isPicking)
    }

    // seeded with the current membership, so the same tap both adds and removes
    @Test
    fun `editing members starts from who is already in the clan`() = runTest {
        val clan = clan(members = listOf(alice, bob))
        val vm = viewModel()

        advanceUntilIdle()

        vm.startEditMembers(clan)
        advanceUntilIdle()

        assertEquals(setOf(alice.id, bob.id), vm.uiState.value.selectedIds)
    }

    @Test
    fun `tapping a person toggles them in and out of the selection`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        vm.toggleSelected(alice)
        advanceUntilIdle()

        assertEquals(setOf(alice.id), vm.uiState.value.selectedIds)

        vm.toggleSelected(alice)
        advanceUntilIdle()

        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    // the name is asked for last: it is easier to name a group once you can see who is in it
    @Test
    fun `submitting a new clan asks for a name rather than saving`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        vm.toggleSelected(alice)
        vm.submitPicking()
        advanceUntilIdle()

        assertEquals(ClanNaming.Create, vm.uiState.value.naming)
        coVerify(exactly = 0) { clanRepository.createClan(any(), any()) }
    }

    @Test
    fun `naming a new clan creates it and leaves picking`() = runTest {
        coEvery { clanRepository.createClan("The Kids", listOf(alice.id)) } returns
            ClanResult.Success(clan(name = "The Kids"))

        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        vm.toggleSelected(alice)
        vm.submitPicking()
        vm.submitName("The Kids")
        advanceUntilIdle()

        assertEquals(ClanNaming.Off, vm.uiState.value.naming)
        assertEquals(ClanPicking.Off, vm.uiState.value.picking)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `editing members saves straight away, without asking for a name`() = runTest {
        val existing = clan(members = listOf(alice))

        coEvery { clanRepository.setClanPeople(existing.id, listOf(bob.id)) } returns
            ClanResult.Success(existing)

        val vm = viewModel()

        advanceUntilIdle()

        vm.startEditMembers(existing)
        vm.toggleSelected(alice)
        vm.toggleSelected(bob)
        vm.submitPicking()
        advanceUntilIdle()

        coVerify { clanRepository.setClanPeople(existing.id, listOf(bob.id)) }
        assertEquals(ClanPicking.Off, vm.uiState.value.picking)
    }

    // the dialog that asked says this in place, rather than it arriving as an app wide snackbar
    @Test
    fun `a name already in use keeps the dialog open and says so`() = runTest {
        coEvery { clanRepository.createClan(any(), any()) } returns ClanResult.DuplicateName

        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        vm.toggleSelected(alice)
        vm.submitPicking()
        vm.submitName("The Kids")
        advanceUntilIdle()

        assertEquals(ClanSaveError.DuplicateName, vm.uiState.value.saveError)
        assertEquals(ClanNaming.Create, vm.uiState.value.naming)
    }

    @Test
    fun `cancelling the name dialog clears the error it was showing`() = runTest {
        coEvery { clanRepository.createClan(any(), any()) } returns ClanResult.DuplicateName

        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        vm.toggleSelected(alice)
        vm.submitPicking()
        vm.submitName("The Kids")
        advanceUntilIdle()

        vm.cancelNaming()
        advanceUntilIdle()

        assertEquals(null, vm.uiState.value.saveError)
        assertEquals(ClanNaming.Off, vm.uiState.value.naming)
    }

    @Test
    fun `renaming a clan does not disturb its membership`() = runTest {
        val existing = clan(name = "Kids", members = listOf(alice))

        coEvery { clanRepository.renameClan(existing.id, "The Kids") } returns
            ClanResult.Success(existing)

        val vm = viewModel()

        advanceUntilIdle()

        vm.startRename(existing)
        vm.submitName("The Kids")
        advanceUntilIdle()

        coVerify { clanRepository.renameClan(existing.id, "The Kids") }
        coVerify(exactly = 0) { clanRepository.setClanPeople(any(), any()) }
    }

    @Test
    fun `cancelling picking drops the selection`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.startCreateClan()
        vm.toggleSelected(alice)
        vm.stopPicking()
        advanceUntilIdle()

        assertEquals(ClanPicking.Off, vm.uiState.value.picking)
        assertTrue(vm.uiState.value.selectedIds.isEmpty())
    }

    @Test
    fun `deleting a clan closes the dialog either way`() = runTest {
        val existing = clan()

        coEvery { clanRepository.deleteClan(existing.id) } returns false

        val vm = viewModel()

        advanceUntilIdle()

        vm.startDelete(existing)
        advanceUntilIdle()

        assertEquals(existing, vm.uiState.value.deleting)

        vm.confirmDelete()
        advanceUntilIdle()

        // a failure has already been reported the way every other failed call is
        assertEquals(null, vm.uiState.value.deleting)
    }

    @Test
    fun `folding the clan row away writes the same preference the settings screen sets`() = runTest {
        val vm = viewModel()

        advanceUntilIdle()

        vm.toggleClansExpanded()
        advanceUntilIdle()

        coVerify { peoplePreferenceRepository.setShowClans(false) }
    }

    @Test
    fun `unfolding it writes the preference back`() = runTest {
        preference.value = PeoplePreference(showClans = false)

        val vm = viewModel()

        advanceUntilIdle()

        vm.toggleClansExpanded()
        advanceUntilIdle()

        coVerify { peoplePreferenceRepository.setShowClans(true) }
    }

    private fun clan(
        name: String = "The Kids",
        members: List<Person> = emptyList(),
    ) = Clan(id = Uuid.random(), name = name, members = members)

    private fun viewModel() =
        PeopleViewModel(peopleRepository, peoplePreferenceRepository, clanRepository)

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
