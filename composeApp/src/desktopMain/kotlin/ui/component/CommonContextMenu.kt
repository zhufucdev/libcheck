package ui.component

import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.delete_para

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CommonContextMenu(expanded: Boolean, onDismissRequest: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        content = {
            SortMenuItem(
                text = { Text(stringResource(Res.string.delete_para)) },
                icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "") },
                selected = false,
                onClick = onDelete,
                modifier = modifier
            )
        }
    )
}