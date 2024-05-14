@file:Suppress("FunctionName")

package ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete_para)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "") },
                onClick = onDelete,
                modifier = modifier
            )
        }
    )
}