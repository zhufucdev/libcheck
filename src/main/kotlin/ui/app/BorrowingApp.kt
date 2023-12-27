package ui.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PlainTooltipBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import model.AppViewModel
import ui.variant
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BorrowingApp(model: AppViewModel) {
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    val coroutine = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Borrow") },
                icon = { Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "") },
                onClick = {}
            )
        }
    ) { p ->
        LazyColumn(Modifier.padding(p)) {
            model.library.borrowList.items.forEach {
                item(it.id) {
                    FlowRow(
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Icon(
                            imageVector = if (it.returnTime == null) Icons.Default.Outbound else Icons.Default.Done,
                            contentDescription = "",
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Separator()
                        Text(
                            formatter.format(Instant.ofEpochMilli(it.time).atZone(ZoneId.systemDefault())),
                            style = MaterialTheme.typography.body2,
                            modifier = Modifier.align(Alignment.CenterVertically)
                        )
                        Separator()
                        model.library.getBook(it.bookId).let { book ->
                            if (book == null) {
                                Icon(
                                    imageVector = Icons.Default.QuestionMark,
                                    contentDescription = "",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            } else {
                                TextButton(
                                    onClick = {},
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
                        model.library.getReader(it.readerId).let { reader ->
                            if (reader == null) {
                                Icon(
                                    imageVector = Icons.Default.PersonSearch,
                                    contentDescription = "",
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            } else {
                                TextButton(
                                    onClick = {},
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
                        it.returnTime.let { rt ->
                            if (rt != null) {
                                Text(
                                    text = "Returned at ${
                                        formatter.format(
                                            Instant.ofEpochMilli(rt).atZone(ZoneId.systemDefault())
                                        )
                                    }",
                                    style = MaterialTheme.typography.body2,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                            } else {
                                PlainTooltipBox(
                                    tooltip = {
                                        androidx.compose.material3.Text("Mark as returned")
                                    }
                                ) {
                                    IconButton(
                                        onClick = {
                                            with(model.library) { it.returned() }
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