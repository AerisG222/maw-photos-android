package us.mikeandwan.photos.ui.components.navigation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.domain.models.SearchHistory

@Composable
fun NavigationRail(
    activeArea: NavigationArea,
    years: List<Int>,
    recentSearchTerms: List<SearchHistory>,
    navigateToSearchWithTerm: (String) -> Unit,
    clearSearchHistory: () -> Unit,
    fetchRandomPhotos: (Int) -> Unit,
    clearRandomPhotos: () -> Unit,
    activeYear: Int,
    navigateToCategories: () -> Unit,
    navigateToCategoriesByYear: (Int) -> Unit,
    navigateToRandom: () -> Unit,
    navigateToSearch: () -> Unit,
    navigateToUpload: () -> Unit,
    navigateToAbout: () -> Unit,
    navigateToSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .width(64.dp),
        ) {
            PrimaryNavigationLink(
                iconId = R.drawable.ic_home,
                descriptionStringId = R.string.categories_icon_description,
                isActiveArea = NavigationArea.Category == activeArea,
                onNavigate = { navigateToCategories() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            PrimaryNavigationLink(
                iconId = R.drawable.ic_search,
                descriptionStringId = R.string.search_icon_description,
                isActiveArea = NavigationArea.Search == activeArea,
                onNavigate = { navigateToSearch() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            PrimaryNavigationLink(
                iconId = R.drawable.ic_shuffle,
                descriptionStringId = R.string.random_photos_icon_description,
                isActiveArea = NavigationArea.Random == activeArea,
                onNavigate = { navigateToRandom() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            Spacer(modifier = Modifier.weight(1f))

            PrimaryNavigationLink(
                iconId = R.drawable.ic_file_upload,
                descriptionStringId = R.string.upload_queue_icon_description,
                isActiveArea = NavigationArea.Upload == activeArea,
                onNavigate = { navigateToUpload() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            PrimaryNavigationLink(
                iconId = R.drawable.ic_help_outline,
                descriptionStringId = R.string.help_icon_description,
                isActiveArea = NavigationArea.About == activeArea,
                onNavigate = { navigateToAbout() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            PrimaryNavigationLink(
                iconId = R.drawable.ic_settings,
                descriptionStringId = R.string.settings_icon_description,
                isActiveArea = NavigationArea.Settings == activeArea,
                onNavigate = { navigateToSettings() },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
        }

        Column(modifier = Modifier.fillMaxSize()) {
            when (activeArea) {
                NavigationArea.Category -> {
                    YearListMenu(
                        years = years,
                        activeYear = activeYear,
                        onYearSelected = { year -> navigateToCategoriesByYear(year) },
                    )
                }

                NavigationArea.Random -> {
                    RandomMenu(
                        fetchRandomPhotos = fetchRandomPhotos,
                        clearRandomPhotos = clearRandomPhotos,
                    )
                }

                NavigationArea.Search -> {
                    SearchListMenu(
                        recentSearchTerms = recentSearchTerms,
                        onTermSelected = { term -> navigateToSearchWithTerm(term) },
                        onClearSearchHistory = { clearSearchHistory() },
                    )
                }

                else -> {}
            }
        }
    }
}
