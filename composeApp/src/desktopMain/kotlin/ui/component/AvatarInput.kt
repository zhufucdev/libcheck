@file:Suppress("FunctionName")

package ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.OutlinedTextField
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileOpen
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker

@Composable
fun AvatarInput(
    uri: String,
    onUriChange: (String) -> Unit,
    label: @Composable () -> Unit,
    defaultImageVector: ImageVector
) {
    var showFilePicker by remember { mutableStateOf(false) }

    FilePicker(
        show = showFilePicker,
        fileExtensions = listOf("png", "jpg", "jpeg", "webp", "tiff", "bmp"),
    ) {
        onUriChange(it?.let { "file://${it.path}" } ?: uri)
        showFilePicker = false
    }

    Row(modifier = Modifier.height(100.dp)) {
        LazyAvatar(uri = uri, defaultImageVector = defaultImageVector, modifier = Modifier.size(100.dp))
        Spacer(Modifier.width(6.dp))
        Column {
            OutlinedTextField(
                value = uri,
                onValueChange = onUriChange,
                label = label,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            TextButton(
                content = {
                    Icon(imageVector = Icons.Default.FileOpen, contentDescription = "")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Select file")
                },
                onClick = {
                    showFilePicker = true
                },
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun LazyAvatar(
    uri: String,
    defaultImageVector: ImageVector,
    contentScale: ContentScale = ContentScale.Fit,
    modifier: Modifier = Modifier
) {
    if (uri.isNotBlank()) {
        AsyncImage(
            uri,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Icon(
            imageVector = defaultImageVector,
            contentDescription = "",
            modifier = modifier
        )
    }
}

