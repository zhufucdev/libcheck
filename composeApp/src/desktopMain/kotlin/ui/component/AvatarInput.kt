@file:Suppress("FunctionName")
@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalResourceApi::class)

package ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Image
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import getUserHome
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.select_file_para

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
        initialDirectory = getUserHome()
    ) {
        onUriChange(it?.let { "file://${it.path}" } ?: uri)
        showFilePicker = false
    }

    Row(modifier = Modifier.height(100.dp), verticalAlignment = Alignment.CenterVertically) {
        LazyAvatar(uri = uri, defaultImageVector = defaultImageVector, modifier = Modifier.size(100.dp))
        Spacer(Modifier.width(6.dp))
        OutlinedTextField(
            value = uri,
            onValueChange = onUriChange,
            label = label,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            trailingIcon = {
                TooltipBox(
                    positionProvider = rememberComponentRectPositionProvider(),
                    state = rememberTooltipState(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(Res.string.select_file_para))
                        }
                    }
                ) {
                    IconButton(onClick = { showFilePicker = true }) {
                        Icon(imageVector = Icons.Default.Image, contentDescription = "")
                    }
                }
            }
        )
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
