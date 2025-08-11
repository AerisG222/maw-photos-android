package us.mikeandwan.photos.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import coil3.compose.AsyncImage
import kotlinx.serialization.Serializable
import us.mikeandwan.photos.R
import us.mikeandwan.photos.domain.models.NavigationArea
import us.mikeandwan.photos.ui.controls.logo.Logo
import us.mikeandwan.photos.ui.controls.topbar.TopBarState

@Serializable
object LoginRoute

fun NavGraphBuilder.loginScreen(
    updateTopBar : (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    navigateAfterLogin: () -> Unit
) {
    composable<LoginRoute> {
        val vm: LoginViewModel = hiltViewModel()
        val state by vm.state.collectAsStateWithLifecycle()
        val context = LocalContext.current

        when(state) {
            is LoginState.Unknown -> {}
            is LoginState.Authorized ->
                LaunchedEffect(state) {
                    navigateAfterLogin()
                }
            is LoginState.NotAuthorized -> {
                LoginScreen(
                    updateTopBar,
                    setNavArea,
                    { vm.login(context) }
                )
            }
        }
    }
}

@Composable
fun LoginScreen(
    updateTopBar : (TopBarState) -> Unit,
    setNavArea: (NavigationArea) -> Unit,
    login: () -> Unit
) {
    val tangerine = FontFamily(Font(R.font.tangerine))

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
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 32.dp)
            ) {
                Logo()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "mikeandwan.us",
                    fontSize = 72.sp,
                    fontFamily = tangerine,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "Photos",
                    fontSize = 72.sp,
                    fontFamily = tangerine,
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 32.dp, 0.dp, 24.dp)
            ) {
                Button(
                    onClick = login
                ) {
                    Text(
                        text = stringResource(id = R.string.activity_login_login_button_text)
                    )
                }
            }
        }
    }
}

@Preview
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        {},
        {},
        {}
    )
}
