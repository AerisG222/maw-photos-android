package us.mikeandwan.photos.ui.controls.metadata

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import us.mikeandwan.photos.R

@Composable
fun CommentScreen(
    commentState: CommentState,
    modifier: Modifier = Modifier,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val interactionSource = remember { MutableInteractionSource() }
    val (newComment, setNewComment) = remember { mutableStateOf("") }

    fun addComment() {
        commentState.addComment(newComment)
        keyboardController?.hide()
        setNewComment("")
    }

    Column(modifier = modifier.fillMaxHeight()) {
        CommentTable(
            comments = commentState.comments,
            modifier = Modifier,
            footer = {
                Row(
                    modifier = Modifier
                        .padding(8.dp, 8.dp)
                        .fillMaxWidth(),
                ) {
                    TextField(
                        modifier = Modifier.fillMaxWidth(),
                        value = newComment,
                        singleLine = false,
                        minLines = 3,
                        maxLines = 3,
                        interactionSource = interactionSource,
                        onValueChange = { setNewComment(it) },
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = { addComment() },
                        ),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            unfocusedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        ),
                    )
                }

                Row(
                    modifier = Modifier
                        .padding(0.dp, 0.dp, 0.dp, 8.dp)
                        .fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Button(
                        onClick = { addComment() },
                    ) {
                        Text(
                            text = stringResource(id = R.string.frg_comment_add_comment),
                        )
                    }
                }
            },
        )
    }
}
