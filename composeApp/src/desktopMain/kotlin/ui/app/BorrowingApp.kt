@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class)

package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.Outbound
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import model.AppViewModel
import model.BorrowSortable
import model.Route
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.LaunchReveal
import ui.PaddingLarge
import ui.component.*
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
    val listState = rememberLazyListState()

    LaunchReveal(model.library.borrows, model.reveal, listState)

    Scaffold {
        if (model.library.borrows.isEmpty()) {
            HeadingPlaceholder(
                imageVector = Icons.Default.VpnKeyOff,
                title = { Text(text = stringResource(Res.string.no_borrow_records_para)) }
            )
        } else {
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
                            sortOrder = model.library.sorter.borrowModel.order,
                            onSortOrderChanged = {
                                coroutine.launch {
                                    model.library.sorter.sortBorrows(it)
                                }
                            }
                        ) {
                            SortMenuCaption(stringResource(Res.string.keyword_para))
                            BorrowSortable.entries.forEach {
                                val selected = model.library.sorter.borrowModel.by == it
                                SortMenuItem(
                                    text = { Text(it.label) },
                                    selected = selected,
                                    icon = { Icon(imageVector = Icons.Default.SortByAlpha, contentDescription = "") },
                                    onClick = {
                                        coroutine.launch {
                                            model.library.sorter.sortBorrows(model.library.sorter.borrowModel.order, it)
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
                LazyColumn(state = listState) {
                    model.library.borrows.forEachIndexed { index, borrow ->
                        item(borrow.id) {
                            val headTooltipState = remember { TooltipState() }
                            val bgColor = rememberRevealAnimation(model, borrow.id, surfaceColor = MaterialTheme.colorScheme.surface)
                            FlowRow(
                                modifier = Modifier.padding(horizontal = PaddingLarge).animateItemPlacement()
                                    .background(bgColor),
                                verticalArrangement = Arrangement.Center
                            ) {
                                TooltipBox(
                                    state = headTooltipState,
                                    tooltip = {
                                        PlainTooltip {
                                            val rt = borrow.returnTime
                                            Text(
                                                if (rt != null) {
                                                    stringResource(
                                                        Res.string.returned_at_para,
                                                        formatter.format(
                                                            Instant.ofEpochMilli(rt).atZone(ZoneId.systemDefault())
                                                        )
                                                    )
                                                } else {
                                                    stringResource(
                                                        Res.string.due_in,
                                                        formatter.format(
                                                            Instant.ofEpochMilli(borrow.dueTime)
                                                                .atZone(ZoneId.systemDefault())
                                                        )
                                                    )
                                                }
                                            )
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
                                                            Icons.AutoMirrored.Filled.Outbound
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
                                        )
                                    },
                                    modifier = Modifier.align(Alignment.CenterVertically),
                                    positionProvider = rememberComponentRectPositionProvider()
                                )
                                Separator()
                                Text(
                                    formatter.format(Instant.ofEpochMilli(borrow.time).atZone(ZoneId.systemDefault())),
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                Separator()
                                val book by remember { flow { emit(model.library.getBook(borrow.bookId)) } }.collectAsState(
                                    null
                                )
                                if (book == null) {
                                    Icon(
                                        imageVector = Icons.Default.QuestionMark,
                                        contentDescription = "",
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                } else {
                                    val b = book!!
                                    TextButton(
                                        onClick = {
                                            model.reveal = b.id
                                            model.route = Route.BOOKS
                                        },
                                        content = {
                                            Text(
                                                b.name,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                                    contentDescription = "",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                val reader by remember {
                                    flow { emit(model.library.getReader(borrow.readerId)) }
                                }.collectAsState(null)
                                if (reader == null) {
                                    Icon(
                                        imageVector = Icons.Default.PersonSearch,
                                        contentDescription = "",
                                        modifier = Modifier.align(Alignment.CenterVertically)
                                    )
                                } else {
                                    val r = reader!!
                                    TextButton(
                                        onClick = {
                                            model.reveal = r.id
                                            model.route = Route.READERS
                                        },
                                        content = {
                                            Text(
                                                r.name,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                    )
                                }
                                Separator()
                                Spacer(Modifier.weight(1f))
                                if (borrow.returnTime == null) {
                                    TooltipBox(
                                        state = rememberTooltipState(),
                                        tooltip = {
                                            PlainTooltip {
                                                Text(stringResource(Res.string.mark_as_returned_para))
                                            }
                                        },
                                        content = {
                                            IconButton(
                                                onClick = {
                                                    coroutine.launch {
                                                        with(model.library) { borrow.setReturned() }
                                                    }
                                                },
                                                content = {
                                                    Icon(imageVector = Icons.Default.Archive, contentDescription = "")
                                                },
                                            )
                                        },
                                        positionProvider = rememberComponentRectPositionProvider()
                                    )
                                }
                            }
                            if (index < model.library.borrows.lastIndex) {
                                Spacer(
                                    Modifier.fillParentMaxWidth().height(1.dp).padding(horizontal = PaddingLarge)
                                        .background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                            }
                        }
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
            .background(MaterialTheme.colorScheme.surfaceVariant)
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
