package us.mikeandwan.photos.ui.screens.people

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import us.mikeandwan.photos.domain.PeoplePreferenceRepository
import us.mikeandwan.photos.domain.PeopleRepository
import us.mikeandwan.photos.domain.models.ExternalCallStatus
import us.mikeandwan.photos.domain.models.PeoplePreference
import us.mikeandwan.photos.domain.models.Person
import us.mikeandwan.photos.domain.models.PersonSort

data class PeopleUiState(
    // already filtered and ordered - the screen draws what it is given
    val people: List<Person> = emptyList(),
    val filter: String = "",
    val preferences: PeoplePreference = PeoplePreference(),
    val isLoading: Boolean = true,
    // told apart from an empty result so the screen can say "nobody is identified yet" rather than
    // "nothing matches what you typed"
    val hasAnyPeople: Boolean = false,
)

@HiltViewModel
class PeopleViewModel
    @Inject
    constructor(
        private val peopleRepository: PeopleRepository,
        private val peoplePreferenceRepository: PeoplePreferenceRepository,
    ) : ViewModel() {
        private val _filter = MutableStateFlow("")
        private val _isLoading = MutableStateFlow(true)

        private val _uiState = MutableStateFlow(PeopleUiState())
        val uiState = _uiState.asStateFlow()

        init {
            combine(
                peopleRepository.people,
                _filter,
                _isLoading,
                peoplePreferenceRepository.getPeoplePreference(),
            ) { people, filter, isLoading, preferences ->
                PeopleUiState(
                    people = people.filterBy(filter).sortedBy(preferences.sortBy),
                    filter = filter,
                    preferences = preferences,
                    isLoading = isLoading && people.isEmpty(),
                    hasAnyPeople = people.isNotEmpty(),
                )
            }.onEach { newState ->
                _uiState.update { newState }
            }.launchIn(viewModelScope)

            loadPeople()
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

        fun toggleFavorite(person: Person) {
            viewModelScope.launch {
                peopleRepository.setIsFavorite(person, !person.isFavorite)
            }
        }

        fun refresh() {
            loadPeople(forceRefresh = true)
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
