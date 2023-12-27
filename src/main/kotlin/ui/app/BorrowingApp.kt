package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.material3.PlainTooltipState
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.AppViewModel
import model.BorrowSortable
import ui.component.SortButton
import ui.component.SortMenu
import ui.component.SortMenuCaption
import ui.component.SortMenuItem
import ui.variant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BorrowingApp(model: AppViewModel) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    val coroutine = rememberCoroutineScope()
    val now = rememberNow()

    Column {
        Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.End) {
            var sorting by remember { mutableStateOf(false) }
            Box {
                SortButton(
                    onClick = { sorting = true },
                    modifier = Modifier.padding(end = 6.dp)
                )
                SortMenu(
                    expanded = sorting,
                    onDismissRequest = { sorting = false },
                    sortOrder = model.library.borrowList.sortOrder,
                    onSortOrderChanged = {
                        model.library.sortBorrows(it, model.library.borrowList.sortedBy)
                        coroutine.launch {
                            model.library.writeToFile()
                        }
                    }
                ) {
                    SortMenuCaption("Keyword")
                    BorrowSortable.entries.forEach {
                        val selected = model.library.borrowList.sortedBy == it
                        SortMenuItem(
                            text = { Text(it.label) },
                            selected = selected,
                            icon = { Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = "") },
                            onClick = {
                                model.library.sortBorrows(model.library.borrowList.sortOrder, it)
                                coroutine.launch {
                                    model.library.writeToFile()
                                }
                            }
                        )
                    }
                }
            }
        }
        LazyColumn {
            model.library.borrowList.items.forEachIndexed { index, borrow ->
                item(borrow.id) {
                    val headTooltipState = remember { PlainTooltipState() }
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp).animateItemPlacement(),
                        verticalArrangement = Arrangement.Center
                    ) {
                        PlainTooltipBox(
                            tooltipState = headTooltipState,
                            tooltip = {
                                borrow.returnTime.let { rt ->
                                    if (rt != null) {
                                        androidx.compose.material3.Text(
                                            "Returned at ${
                                                formatter.format(
                                                    Instant.ofEpochMilli(rt).atZone(ZoneId.systemDefault())
                                                )
                                            }",
                                        )
                                    } else {
                                        androidx.compose.material3.Text(
                                            "Due in ${
                                                formatter.format(
                                                    Instant.ofEpochMilli(borrow.dueTime).atZone(ZoneId.systemDefault())
                                                )
                                            }"
                                        )
                                    }
                                }
                            },
                            content = {
                                IconButton(
                                    content = {
                                        Icon(
                                            imageVector = if (borrow.returnTime == null) {
                                                if (borrow.dueTime < now.toEpochMilli()) {
                                                    Icons.Default.Timer
                                                } else {
                                                    Icons.Default.Outbound
                                                }
                                            } else {
                                                Icons.Default.Done
                                            },
                                            contentDescription = "",
                                        )
                                    },
                                    onClick = {
                                        coroutine.launch {
                                            headTooltipState.show()
                                        }
                                    },
                                    modifier = Modifier.tooltipAnchor()
                                )
                            },
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Separator()
                        Text(
                            formatter.format(Instant.ofEpochMilli(borrow.time).atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Separator()
                        model.library.getBook(borrow.bookId).let { book ->
                            if (book == null) {
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = "",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        model.reveal = book.id
                                        model.route = Route.BOOKS
                                    },
                                    content = {
                                        Text(
                                            book.name,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.ArrowRightAlt,
                            contentDescription = "",
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        model.library.getReader(borrow.readerId).let { reader ->
                            if (reader == null) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = "",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            } else {
                                TextButton(
                                    onClick = {
                                        model.reveal = reader.id
                                        model.route = Route.READERS
                                    },
                                    content = {
                                        Text(
                                            reader.name,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                    }
                                )
                            }
                        }
                        Separator()
                        Spacer(Modifier.weight(1f))
                        if (borrow.returnTime == null) {
                            PlainTooltipBox(
                                tooltip = {
                                    androidx.compose.material3.Text("Mark as returned")
                                }
                            ) {
                                IconButton(
                                    onClick = {
                                        with(model.library) { borrow.returned() }
                                        coroutine.launch {
                                            model.library.writeToFile()
                                        }
                                    },
                                    content = {
                                        Icon(imageVector = Icons.Default.Archive, contentDescription = "")
                                    },
                                    modifier = Modifier.tooltipAnchor()
                                )
                            }
                        }
                    }
                }
                item {
                    if (index < model.library.borrowList.items.lastIndex) {
                        Spacer(
                            Modifier.fillParentMaxWidth().height(1.dp).padding(horizontal = 12.dp)
                                .background(MaterialTheme.colors.onSurface.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Separator() {
    Spacer(
        Modifier.padding(horizontal = 6.dp)
            .background(MaterialTheme.colors.onSurface.variant)
            .fillMaxHeight()
            .width(2.dp)
    )
}

@Composable
fun rememberNow(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(true) {
        while (true) {
            delay(0.5.seconds)
            now = Instant.now()
        }
    }
    return now
}
