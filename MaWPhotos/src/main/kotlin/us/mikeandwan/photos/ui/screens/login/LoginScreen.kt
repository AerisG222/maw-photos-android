package us.mikeandwan.photos.ui.screens.login

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import us.mikeandwan.photos.R
import us.mikeandwan.photos.ui.components.logo.Logo

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tangerine = FontFamily(Font(R.font.tangerine))

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        item {
            AsyncImage(
                model = R.drawable.banner,
                contentDescription = "MaW Photos Banner",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .height(150.dp)
                    .padding(0.dp, 0.dp, 0.dp, 32.dp),
            )
        }

        item {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 0.dp, 0.dp, 32.dp),
            ) {
                Logo()
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "mikeandwan.us",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 64.sp,
                    fontFamily = tangerine,
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = "Photos",
                    color = MaterialTheme.colorScheme.secondary,
                    fontSize = 64.sp,
                    fontFamily = tangerine,
                )
            }
        }

        item {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(0.dp, 32.dp, 0.dp, 24.dp),
            ) {
                Button(
                    onClick = onLogin,
                ) {
                    AsyncImage(
                        model = R.drawable.ic_login,
                        contentDescription = stringResource(
                            id = R.string.fragment_settings_log_out,
                        ),
                        colorFilter = ColorFilter.tint(MaterialTheme.colorScheme.onSecondary),
                        modifier = Modifier
                            .padding(4.dp, 4.dp, 12.dp, 4.dp)
                            .height(24.dp)
                            .width(24.dp),
                    )

                    Text(
                        text = stringResource(id = R.string.activity_login_login_button_text),
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun LoginScreenPreview() {
    LoginScreen(
        uiState = LoginUiState(isAuthorized = false),
        onLogin = {},
    )
}
