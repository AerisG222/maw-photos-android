package us.mikeandwan.photos.ui.screens.about

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mikepenz.markdown.m3.Markdown
import com.mikepenz.markdown.m3.markdownTypography
import us.mikeandwan.photos.R
import us.mikeandwan.photos.ui.components.loading.Loading
import us.mikeandwan.photos.ui.components.logo.Logo

@Composable
fun AboutScreen(
    state: AboutUiState,
    modifier: Modifier = Modifier,
) {
    if (state.isLoading) {
        Loading()
        return
    }

    val tangerine = remember { FontFamily(Font(R.font.tangerine)) }

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
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
                fontSize = 48.sp,
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
                fontSize = 48.sp,
                fontFamily = tangerine,
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = state.version,
                color = MaterialTheme.colorScheme.secondary,
                fontSize = 48.sp,
                fontFamily = tangerine,
            )
        }

        Row(
            modifier = Modifier
                .padding(8.dp, 16.dp, 8.dp, 8.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            Markdown(
                state.history,
                typography = markdownTypography(
                    h1 = MaterialTheme.typography.headlineMedium.copy(
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 24.sp,
                    ),
                    h2 = MaterialTheme.typography.headlineSmall.copy(
                        color = MaterialTheme.colorScheme.secondary,
                        fontSize = 20.sp,
                    ),
                ),
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun AboutScreenPreview() {
    AboutScreen(
        AboutUiState(version = "vX.Y.Z", history = "Release Notes", isLoading = false),
    )
}

@Preview(showBackground = true)
@Composable
fun AboutScreenLoadingPreview() {
    AboutScreen(
        AboutUiState(isLoading = true),
    )
}
