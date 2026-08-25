package us.mikeandwan.photos.ui.screens.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.hoc081098.flowext.combine
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.ClanRepository
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.Clan
import us.mikeandwan.photos.domain.models.ClanResult
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.PersonSort

data class PeopleUiState(
    // already filtered and ordered - the screen draws what it is given
    val people: List<Person> = emptyList(),
    val clans: List<Clan> = emptyList(),
    val filter: String = "",
    val preferences: PeoplePreference = PeoplePreference(),
    val isLoading: Boolean = true,
    // told apart from an empty result so the screen can say "nobody is identified yet" rather than
    // "nothing matches what you typed"
    val hasAnyPeople: Boolean = false,
    val picking: ClanPicking = ClanPicking.Off,
    val selectedIds: Set<Uuid> = emptySet(),
    val naming: ClanNaming = ClanNaming.Off,
    val deleting: Clan? = null,
    val isSaving: Boolean = false,
    // shown in the dialog that asked, rather than as an app wide snackbar - see ClanRepository
    val saveError: ClanSaveError? = null,
) {
    val isPicking: Boolean
        get() = picking != ClanPicking.Off
}

/** The clan failures a dialog has to say something specific about. */
enum class ClanSaveError {
    DuplicateName,
    Invalid,
    Failed,
}

@HiltViewModel
class PeopleViewModel
    @Inject
    constructor(
        private val peopleRepository: PeopleRepository,
        private val peoplePreferenceRepository: PeoplePreferenceRepository,
        private val clanRepository: ClanRepository,
    ) : ViewModel() {
        private val _filter = MutableStateFlow("")
        private val _isLoading = MutableStateFlow(true)
        private val _picking = MutableStateFlow<ClanPicking>(ClanPicking.Off)
        private val _selectedIds = MutableStateFlow<Set<Uuid>>(emptySet())
        private val _naming = MutableStateFlow<ClanNaming>(ClanNaming.Off)
        private val _deleting = MutableStateFlow<Clan?>(null)
        private val _isSaving = MutableStateFlow(false)
        private val _saveError = MutableStateFlow<ClanSaveError?>(null)

        private val _uiState = MutableStateFlow(PeopleUiState())
        val uiState = _uiState.asStateFlow()

        init {
            combine(
                peopleRepository.people,
                clanRepository.clans,
                _filter,
                _isLoading,
                peoplePreferenceRepository.getPeoplePreference(),
                _picking,
                _selectedIds,
                _naming,
                _deleting,
                _isSaving,
                _saveError,
            ) {
                people,
                clans,
                filter,
                isLoading,
                preferences,
                picking,
                selectedIds,
                naming,
                deleting,
                isSaving,
                saveError,
                ->
                PeopleUiState(
                    people = people.filterBy(filter).sortedBy(preferences.sortBy),
                    clans = clans,
                    filter = filter,
                    preferences = preferences,
                    isLoading = isLoading && people.isEmpty(),
                    hasAnyPeople = people.isNotEmpty(),
                    picking = picking,
                    selectedIds = selectedIds,
                    naming = naming,
                    deleting = deleting,
                    isSaving = isSaving,
                    saveError = saveError,
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)

            loadPeople()
            loadClans()
        }

        fun setFilter(filter: String) {
            _filter.update { filter }
        }

        fun toggleSort() {
            viewModelScope.launch {
                peoplePreferenceRepository.setSortBy(
                    _uiState.value.preferences.sortBy
                        .next(),
                )
            }
        }

        // the same preference the settings screen offers, so folding the row away here and turning
        // it off there are one setting rather than two that can disagree
        fun toggleClansExpanded() {
            viewModelScope.launch {
                peoplePreferenceRepository.setShowClans(!_uiState.value.preferences.showClans)
            }
        }

        fun toggleFavorite(person: Person) {
            viewModelScope.launch {
                peopleRepository.setIsFavorite(person, !person.isFavorite)
            }
        }

        fun refresh() {
            loadPeople(forceRefresh = true)
            loadClans(forceRefresh = true)
        }

        // CLAN PICKING

        fun startCreateClan() {
            _selectedIds.update { emptySet() }
            _picking.update { ClanPicking.Create }
        }

        // seeded with who is already in it, so the same interaction adds and removes: whatever is
        // selected when the bar is saved becomes the membership
        fun startEditMembers(clan: Clan) {
            _selectedIds.update { clan.members.map { it.id }.toSet() }
            _picking.update { ClanPicking.Members(clan) }
        }

        fun stopPicking() {
            _picking.update { ClanPicking.Off }
            _selectedIds.update { emptySet() }
    }

    fun toggleSelected(person: Person) {
        _selectedIds.update { selected ->
            if (person.id in selected) selected - person.id else selected + person.id
        }
    }

    fun clearSelection() {
        _selectedIds.update { emptySet() }
    }

    /**
     * Finishes picking: a new clan goes on to be named, while an edit to an existing one saves
     * straight away - it already has a name, and the membership is the only thing that changed.
     */
    fun submitPicking() {
        when (val picking = _picking.value) {
            is ClanPicking.Create -> {
                _naming.update { ClanNaming.Create }
            }

            is ClanPicking.Members -> {
                save {
                    clanRepository.setClanPeople(picking.clan.id, _selectedIds.value.toList())
                }
            }

            ClanPicking.Off -> {}
        }
    }

    // CLAN NAMING

    fun startRename(clan: Clan) {
        _naming.update { ClanNaming.Rename(clan) }
    }

    fun submitName(name: String) {
        when (val naming = _naming.value) {
            is ClanNaming.Create -> {
                save { clanRepository.createClan(name, _selectedIds.value.toList()) }
            }

            is ClanNaming.Rename -> {
                save { clanRepository.renameClan(naming.clan.id, name) }
            }

            ClanNaming.Off -> {}
        }
    }

    fun cancelNaming() {
        _naming.update { ClanNaming.Off }
        _saveError.update { null }
    }

    // CLAN DELETION

    fun startDelete(clan: Clan) {
        _deleting.update { clan }
    }

    fun confirmDelete() {
        val clan = _deleting.value ?: return

        viewModelScope.launch {
            _isSaving.update { true }
            clanRepository.deleteClan(clan.id)

            // closed either way: a failure has already been reported the way every other failed
            // call is, and leaving the dialog open would say it a second time
            _isSaving.update { false }
            _deleting.update { null }
        }
    }

    fun cancelDelete() {
        _deleting.update { null }
    }

    private fun save(call: suspend () -> ClanResult) {
        viewModelScope.launch {
            _isSaving.update { true }
            _saveError.update { null }

            when (val result = call()) {
                is ClanResult.Success -> {
                    _naming.update { ClanNaming.Off }
                    stopPicking()
                }

                ClanResult.DuplicateName -> {
                    _saveError.update { ClanSaveError.DuplicateName }
                }

                ClanResult.Invalid -> {
                    _saveError.update { ClanSaveError.Invalid }
                }

                ClanResult.Failed -> {
                    _saveError.update { ClanSaveError.Failed }
                }
            }

            _isSaving.update { false }
        }
        }

        private fun loadPeople(forceRefresh: Boolean = false) {
            viewModelScope.launch {
                peopleRepository
                    .getPeople(forceRefresh)
                    .collect { status ->
                        if (status !is ExternalCallStatus.Loading) {
                            _isLoading.update { false }
                        }
                    }
            }
        }

    private fun loadClans(forceRefresh: Boolean = false) {
        viewModelScope.launch {
            clanRepository
                .getClans(forceRefresh)
                .collect { }
        }
    }

        // matched anywhere in the name rather than only at the start: people are remembered by
        // whichever part of their name comes to mind first
        private fun List<Person>.filterBy(filter: String): List<Person> {
            val term = filter.trim()

            return if (term.isEmpty()) {
                this
            } else {
                filter { it.name.contains(term, ignoreCase = true) }
            }
        }

        // favorites lead either ordering, mirroring the list the API hands back.  name breaks a tie
        // on media count so the order does not shuffle between reads.
        private fun List<Person>.sortedBy(sort: PersonSort): List<Person> =
            when (sort) {
                PersonSort.Name -> sortedWith(
                    compareByDescending<Person> { it.isFavorite }
                        .thenBy { it.name.lowercase() },
                )

                PersonSort.MediaCount -> sortedWith(
                    compareByDescending<Person> { it.isFavorite }
                        .thenByDescending { it.mediaCount }
                        .thenBy { it.name.lowercase() },
                )
            }
    }
