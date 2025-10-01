package us.mikeandwan.photos.ui.screens.inactiveUser

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.HorizontalAlignmentLine
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import timber.log.Timber
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.domain.models.UserStatus
import us.mikeandwan.photos.ui.controls.logo.Logo
import us.mikeandwan.photos.ui.controls.topbar.TopBarState

@Serializable
object InactiveUserRoute

fun NavGraphBuilder.inactiveUserScreen(
    updateTopBar : (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    navigateToLogin: () -> Unit,
    navigateAfterActivated: () -> Unit
) {
    composable<InactiveUserRoute> {
        val vm: InactiveUserViewModel = hiltViewModel()
        val userStatus by vm.userStatus.collectAsStateWithLifecycle()
        val context = LocalContext.current

        when(userStatus) {
            is UserStatus.Unknown -> { Timber.w("YO!  UNK!") }
            is UserStatus.Active ->
                LaunchedEffect(userStatus) {
                    navigateAfterActivated()
                }
            is UserStatus.Inactive -> {
                InactiveUserScreen(
                    updateTopBar,
                    setNavArea,
                    requeryStatus = { vm.queryUserStatus() },
                    logout = { vm.logout(context); navigateToLogin() }
                )
            }
        }
    }
}

@Composable
fun InactiveUserScreen(
    updateTopBar : (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    requeryStatus: () -> Unit,
    logout: () -> Unit
) {
    BackHandler(enabled = true) {
        // Do nothing on back press
        // This effectively disables the back button for this Composable
    }

    LaunchedEffect(Unit) {
        updateTopBar(
            TopBarState().copy(
                show = false
            )
        )

        setNavArea(NavigationArea.Login)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        item {
            AsyncImage(
                model = R.drawable.banner,
                contentDescription = "MaW Photos Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(150.dp)
                    .padding(0.dp, 0.dp, 0.dp, 32.dp)
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Thank you for logging into photos.mikeandwan.us!",
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    color = MaterialTheme.colorScheme.secondary,
                    text = "Your account has been created but has not been assigned any permissions. " +
                        "An administrator will review this shortly and get back to you once your account is all set.",
                    textAlign = TextAlign.Center
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    fontStyle = FontStyle.Italic,
                    text = "Please email the administrator if they have not gotten back to you!",
                    textAlign = TextAlign.Center
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 32.dp, 0.dp, 16.dp)
            ) {
                Button(
                    onClick = requeryStatus
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_inactive_user_recheck_status),
                        modifier = Modifier.padding( 4.dp)
                    )
                }
            }
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 16.dp, 0.dp, 24.dp)
            ) {
                OutlinedButton(
                    onClick = logout,
                    colors = ButtonColors(
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.surface,
                        MaterialTheme.colorScheme.onSurface
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    AsyncImage(
                        model = R.drawable.ic_logout,
                        contentDescription = stringResource(id = R.string.fragment_settings_log_out),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.primary),
                        modifier = Modifier
                            .padding(4.dp, 4.dp, 12.dp, 4.dp)
                            .height(24.dp)
                            .width(24.dp)
                    )

                    Text(
                        text = stringResource(id = R.string.activity_inactive_user_logout),
                        modifier = Modifier.padding(0.dp, 4.dp, 4.dp, 4.dp)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
@NonRestartableComposable
fun InactiveUserScreenPreview() {
    InactiveUserScreen(
        updateTopBar = {},
        setNavArea = {},
        requeryStatus = {},
        logout = {}
    )
}
