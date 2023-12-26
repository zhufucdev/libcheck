package ui.component

import androidx.compose.material.DropdownMenu
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun CommonContextMenu(expanded: Boolean, onDismissRequest: () -> Unit, onDelete: () -> Unit, modifier: Modifier = Modifier) {
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        content = {
            SortMenuItem(
                text = { Text("Delete") },
                icon = { Icon(imageVector = Icons.Default.Delete, contentDescription = "") },
                selected = false,
                onClick = onDelete,
                modifier = modifier
            )
        }
    )
}