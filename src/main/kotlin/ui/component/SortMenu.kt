package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import model.SortOrder
import ui.variant

@Composable
fun SortMenu(
    expanded: Boolean,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    onDismissRequest: () -> Unit,
    sortOrder: SortOrder,
    onSortOrderChanged: (SortOrder) -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    DropdownMenu(expanded = expanded, onDismissRequest = onDismissRequest, offset = offset) {
        Column {
            SortMenuCaption("Order")
            SortMenuItem(
                text = { Text("Ascending") },
                icon = { Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = "") },
                selected = sortOrder == SortOrder.ASCENDING,
                onClick = {
                    onSortOrderChanged(SortOrder.ASCENDING)
                }
            )
            SortMenuItem(
                text = { Text("Descending") },
                icon = { Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = "") },
                selected = sortOrder == SortOrder.DESCENDING,
                onClick = { onSortOrderChanged(SortOrder.DESCENDING) }
            )
            Separator()
            content()
        }
    }
}

@Composable
fun SortMenuCaption(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.primary),
        modifier = Modifier.padding(12.dp)
    )
}

@Composable
fun SortMenuItem(text: @Composable () -> Unit, icon: @Composable () -> Unit, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.clickable { onClick() }
            .then(
                if (selected) Modifier.background(color = MaterialTheme.colors.primarySurface.variant)
                else Modifier
            )
            .then(modifier),
    ) {
        CompositionLocalProvider(LocalContentColor provides if (selected) MaterialTheme.colors.onPrimary else LocalContentColor.current) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icon()
                Spacer(modifier = Modifier.width(4.dp))
                text()
            }
        }
    }
}

@Composable
fun SortButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    TextButton(
        content = {
            Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = "")
            Spacer(Modifier.width(4.dp))
            Text("Sort")
        },
        onClick = onClick,
        modifier = modifier
    )
}