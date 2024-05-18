@file:Suppress("FunctionName")

package ui.component

import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.delete_para
import resources.edit_para

@OptIn(ExperimentalResourceApi::class)
@Composable
fun CommonContextMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    onEdit: () -> Unit,
    onDelete: suspend () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()
    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest,
        content = {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.edit_para)) },
                leadingIcon = { Icon(imageVector = Icons.Default.Edit, contentDescription = "edit") },
                onClick = {
                    onDismissRequest()
                    onEdit()
                }
            )
            var deleting by remember { mutableStateOf(false) }
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.delete_para)) },
                leadingIcon = {
                    if (deleting) {
                        CircularProgressIndicator(Modifier.size(24.dp))
                    } else {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = "delete")
                    }
                },
                onClick = {
                    coroutineScope.launch {
                        deleting = true
                        onDelete()
                        deleting = false
                    }
                },
            )
        },
        modifier = modifier
    )
}