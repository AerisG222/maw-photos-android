package us.mikeandwan.photos.ui.components.topbar

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarDefaults.inputFieldColors
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp

// copied and modified from SearchBarDefaults.InputField
// the main focus of this hack is to tweak the input size
// also removed some things i wasn't using
@ExperimentalMaterial3Api
@Composable
fun SearchInputField(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    colors: TextFieldColors = inputFieldColors(),
) {
    val interactionSource = remember { MutableInteractionSource() }

    BasicTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        enabled = enabled,
        singleLine = true,
        textStyle = LocalTextStyle.current.merge(
            TextStyle(
                fontSize = MaterialTheme.typography.titleMedium.fontSize,
                color = MaterialTheme.colorScheme.onSurface,
            ),
        ),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
        keyboardActions = KeyboardActions(onSearch = { onSearch(query) }),
        decorationBox =
            @Composable { innerTextField ->
                TextFieldDefaults.DecorationBox(
                    value = query,
                    innerTextField = innerTextField,
                    enabled = enabled,
                    singleLine = true,
                    visualTransformation = VisualTransformation.None,
                    placeholder = placeholder,
                    leadingIcon =
                        leadingIcon?.let { leading ->
                            { Box(Modifier.offset(x = 4.dp)) { leading() } }
                        },
                    trailingIcon =
                        trailingIcon?.let { trailing ->
                            { Box(Modifier.offset(x = (-4).dp)) { trailing() } }
                        },
                    shape = SearchBarDefaults.inputFieldShape,
                    colors = colors,
                    contentPadding = PaddingValues(16.dp, 2.dp),
                    interactionSource = interactionSource,
                    container = {},
                )
            },
    )
}
