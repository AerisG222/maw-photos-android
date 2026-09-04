package us.mikeandwan.photos.ui.screens.people

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.MediaFeedSubject
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.LocalMawAppActions
import us.mikeandwan.photos.ui.components.people.ClanDeleteDialog
import us.mikeandwan.photos.ui.components.people.ClanNameDialog
import us.mikeandwan.photos.ui.components.topbar.TopBarState

@Serializable
object PeopleNavKey : NavKey

fun EntryProviderScope<NavKey>.people() {
    entry<PeopleNavKey> {
        PeopleRoute()
    }
}

@Composable
private fun PeopleRoute(vm: PeopleViewModel = hiltViewModel()) {
    val uiState by vm.uiState.collectAsStateWithLifecycle()
    val appActions = LocalMawAppActions.current
    val title = stringResource(id = R.string.people_title)

    LaunchedEffect(title) {
        appActions.setNavArea(NavigationArea.People)
        // the grid is nobody in particular, so nothing in the rail's list is current
        appActions.setActiveFeedSubject(null)
        appActions.updateTopBar(
            NavigationArea.People,
            TopBarState(title = title),
        )
    }

    PeopleScreen(
        uiState = uiState,
        onFilterChange = { vm.setFilter(it) },
        onToggleSort = { vm.toggleSort() },
        onToggleFavorite = { vm.toggleFavorite(it) },
        onSelectPerson = { person ->
            appActions.navigateToMediaFeed(MediaFeedSubject.Person(person.id))
        },
        onSelectClan = { clan ->
            appActions.navigateToMediaFeed(MediaFeedSubject.Clan(clan.id))
        },
        onCreateClan = { vm.startCreateClan() },
        onEditClanMembers = { vm.startEditMembers(it) },
        onRenameClan = { vm.startRename(it) },
        onDeleteClan = { vm.startDelete(it) },
        onToggleSelected = { vm.toggleSelected(it) },
        onClearSelection = { vm.clearSelection() },
        onSubmitPicking = { vm.submitPicking() },
        onCancelPicking = { vm.stopPicking() },
        onToggleClansExpanded = { vm.toggleClansExpanded() },
    )

    when (val naming = uiState.naming) {
        is ClanNaming.Create -> {
            ClanNameDialog(
                title = stringResource(id = R.string.clan_name_new),
                submitLabel = stringResource(id = R.string.clan_create),
                initialName = "",
                memberCount = uiState.selectedIds.size,
                isSaving = uiState.isSaving,
                error = uiState.saveError?.let { stringResource(id = it.messageId()) },
                onSubmit = { vm.submitName(it) },
                onCancel = { vm.cancelNaming() },
            )
        }

        is ClanNaming.Rename -> {
            ClanNameDialog(
                title = stringResource(id = R.string.clan_rename),
                submitLabel = stringResource(id = R.string.clan_save),
                initialName = naming.clan.name,
                // a rename does not touch the membership, so counting it would only be noise
                memberCount = null,
                isSaving = uiState.isSaving,
                error = uiState.saveError?.let { stringResource(id = it.messageId()) },
                onSubmit = { vm.submitName(it) },
                onCancel = { vm.cancelNaming() },
            )
        }

        ClanNaming.Off -> {}
    }

    uiState.deleting?.let { clan ->
        ClanDeleteDialog(
            clan = clan,
            isSaving = uiState.isSaving,
            onConfirm = { vm.confirmDelete() },
            onCancel = { vm.cancelDelete() },
        )
    }
}

private fun ClanSaveError.messageId() =
    when (this) {
        ClanSaveError.DuplicateName -> R.string.clan_error_duplicate_name
        ClanSaveError.Invalid -> R.string.clan_error_invalid
        ClanSaveError.Failed -> R.string.clan_error_failed
    }
