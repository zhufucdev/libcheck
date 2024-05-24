@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class)

package ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.automirrored.filled.Outbound
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import extension.calculateCredit
import extension.getClearCredit
import extension.takeIfInstanceOf
import extension.toFixed
import kotlinx.coroutines.launch
import model.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.*
import ui.component.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import kotlin.math.abs

@OptIn(ExperimentalLayoutApi::class, ExperimentalFoundationApi::class)
@Composable
fun BorrowingApp(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }
    val formatter = remember { DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM) }
    val now = rememberNow()
    val listState = rememberLazyListState()
    val borrows = model.library.borrows.let { borrows ->
        val parameters = model.navigator.current.parameters
        if (parameters is FilterBorrowParameters) {
            borrows.filter { with(parameters) { it.isCandidate() } }
        } else {
            borrows
        }
    }

    var judgingReader by remember { mutableStateOf<JudgeReaderModel?>(null) }

    val onReturn: ((borrow: BorrowLike) -> Unit)? = model.library.components.of<ReturnCapability>()?.let { component ->
        { borrow ->
            coroutine.launch {
                val onTime = Instant.now().toEpochMilli() <= borrow.dueTime
                val reader = model.library.getReader(borrow.readerId) ?: return@launch
                val newCredit = calculateCredit(reader, model.configurations, model.configurations.creditStep, onTime)
                with(component) {
                    borrow.setReturned(newCredit.toFloat())
                }
                val res = snackbars.showSnackbar(
                    message = getString(
                        if (onTime) Res.string.readers_creditability_increased_para
                        else Res.string.readers_creditability_decreased_para,
                        reader.name
                    ),
                    withDismissAction = true,
                    actionLabel = getString(Res.string.details_para)
                )
                if (res == SnackbarResult.ActionPerformed) {
                    judgingReader = JudgeReaderModel(reader, onTime) {
                        with(component) {
                            borrow.setReturned(it)
                        }
                    }
                }
            }
        }
    }

    LaunchReveal(model.library.borrows, model, listState)

    Scaffold(
        snackbarHost = { SnackbarHost(snackbars) }
    ) {
        if (borrows.isEmpty()) {
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
                    borrows.forEachIndexed { index, borrow ->
                        val hasSeparator = index < model.library.borrows.lastIndex
                        item(borrow.id) {
                            val bgColor = rememberRevealAnimation(
                                model,
                                borrow.id,
                                surfaceColor = MaterialTheme.colorScheme.surface
                            )

                            when (borrow) {
                                is Borrow ->
                                    BorrowItem(
                                        app = model,
                                        model = borrow,
                                        backgroundColor = bgColor,
                                        formatter = formatter,
                                        now = now,
                                        hasSeparator = hasSeparator,
                                        modifier = Modifier.animateItemPlacement(),
                                        onReturnRequest = onReturn
                                    )

                                is BorrowBatch ->
                                    BorrowBatchItem(
                                        model = borrow,
                                        app = model,
                                        backgroundColor = bgColor,
                                        formatter = formatter,
                                        now = now,
                                        hasSeparator = hasSeparator,
                                        modifier = Modifier.animateItemPlacement(),
                                        onReturnRequest = onReturn
                                    )
                            }
                        }
                    }
                }
            }
        }
    }

    judgingReader?.let {
        JudgeReaderDialog(
            model = it,
            configurations = model.configurations,
            onDismissRequest = {
                judgingReader = null
            },
            onConfirmRequest = { credit ->
                coroutine.launch {
                    it.update(credit)
                }
            }
        )
    }
}

@ExperimentalLayoutApi
@Composable
private fun BorrowItem(
    model: Borrow,
    app: AppViewModel,
    backgroundColor: Color,
    formatter: DateTimeFormatter,
    now: Instant,
    hasSeparator: Boolean,
    onReturnRequest: ((BorrowLike) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    BasicBorrowLike(
        model = model,
        backgroundColor = backgroundColor,
        formatter = formatter,
        now = now,
        modifier = modifier,
        separator = hasSeparator,
        onReturnRequest = onReturnRequest
    ) {
        val book = remember(model) { app.library.getBook(model.bookId) }
        if (book == null) {
            ReconstructButton(
                icon = {
                    Icon(
                        painter = painterResource(Res.drawable.book_search),
                        contentDescription = "book not found",
                        modifier = Modifier.align(Alignment.CenterVertically).size(24.dp)
                    )
                },
                text = { Text(stringResource(Res.string.book_was_not_found)) },
                onReconstructRequest = {
                    app.navigator.push(
                        RouteType.Books,
                        ReconstructParameters(model.bookId)
                    )
                }
            )
        } else {
            TextButton(
                onClick = {
                    app.navigator.push(RouteType.Books, RevealParameters(book.id))
                },
                content = {
                    Text(
                        book.name,
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
        val reader = remember(model) { app.library.getReader(model.readerId) }
        ReaderTextButton(reader, model.readerId, app)
    }
}

@Composable
private fun RowScope.ReaderTextButton(reader: Reader?, id: UuidIdentifier, app: AppViewModel) {
    if (reader == null) {
        ReconstructButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.PersonSearch,
                    contentDescription = "reader not found",
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            },
            text = {
                Text(stringResource(Res.string.reader_was_not_found))
            },
            onReconstructRequest = {
                app.navigator.push(
                    RouteType.Readers,
                    ReconstructParameters(id)
                )
            }
        )
    } else {
        TextButton(
            onClick = {
                app.navigator.push(RouteType.Readers, RevealParameters(reader.id))
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

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun BorrowBatchItem(
    model: BorrowBatch,
    app: AppViewModel,
    backgroundColor: Color,
    formatter: DateTimeFormatter,
    now: Instant,
    hasSeparator: Boolean,
    onReturnRequest: ((BorrowLike) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val filtering by remember(model) {
        derivedStateOf {
            app.navigator.current.parameters is FilterBorrowParameters
        }
    }
    val booksExpanded by remember(model) {
        derivedStateOf {
            app.navigator.current.parameters
                .takeIfInstanceOf<NavigationParameters, RevealDetailsParameters>()
                ?.identifier
                ?.let { it == model.id } == true
                    || filtering
        }
    }
    Column(modifier) {
        BasicBorrowLike(
            model = model,
            backgroundColor = backgroundColor,
            formatter = formatter,
            now = now,
            separator = hasSeparator && !booksExpanded,
            onReturnRequest = onReturnRequest
        ) {
            TextButton(
                enabled = !filtering,
                onClick = {
                    if (!booksExpanded) {
                        app.navigator.replace(parameters = RevealDetailsParameters(model.id))
                    } else {
                        app.navigator.replace()
                    }
                }
            ) {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "a batch of books",
                    tint = LocalContentColor.current
                )
                Text(
                    text = "×${model.bookIds.size}",
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
            Icon(
                imageVector = Icons.AutoMirrored.Default.ArrowRightAlt,
                contentDescription = "",
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            val reader = remember(model) { app.library.getReader(model.readerId) }
            ReaderTextButton(reader, model.readerId, app)
        }
        AnimatedVisibility(booksExpanded, enter = expandVertically(), exit = shrinkVertically()) {
            Column {
                model.bookIds.forEach { id ->
                    val book = remember(id) { app.library.getBook(id) }
                    FlowRow(
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(start = PaddingLarge * 2, end = PaddingLarge)
                    ) {
                        if (book == null) {
                            ReconstructButton(
                                icon = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            painter = painterResource(Res.drawable.book_search),
                                            contentDescription = "book not found"
                                        )
                                        Spacer(Modifier.width(PaddingSmall))
                                        Text(stringResource(Res.string.book_was_not_found))
                                    }
                                },
                                text = { Text(stringResource(Res.string.book_was_not_found)) },
                                onReconstructRequest = {
                                    app.navigator.push(dest = RouteType.Books, parameters = ReconstructParameters(id))
                                }
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Book,
                                contentDescription = book.name,
                                modifier = Modifier.align(Alignment.CenterVertically).padding(end = PaddingLarge)
                            )
                            VSpacer()
                            Text(
                                text = book.displayName(),
                                modifier = Modifier.align(Alignment.CenterVertically),
                                style = MaterialTheme.typography.labelLarge
                            )
                            VSpacer()
                            Text(
                                text = stringResource(Res.string.by_auther_para, book.author),
                                modifier = Modifier.align(Alignment.CenterVertically),
                                style = MaterialTheme.typography.labelLarge
                            )
                            Spacer(Modifier.weight(1f))
                            TooltipBox(
                                positionProvider = rememberComponentRectPositionProvider(),
                                state = rememberTooltipState(),
                                tooltip = {
                                    PlainTooltip {
                                        Text(stringResource(Res.string.reveal_in_books_para))
                                    }
                                }
                            ) {
                                IconButton(
                                    onClick = { app.navigator.push(RouteType.Books, RevealParameters(id)) }
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Default.OpenInNew,
                                        contentDescription = "reveal in books"
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        if (booksExpanded) {
            Separator()
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun BasicBorrowLike(
    model: BorrowLike,
    backgroundColor: Color,
    formatter: DateTimeFormatter,
    now: Instant,
    separator: Boolean,
    modifier: Modifier = Modifier,
    onReturnRequest: ((BorrowLike) -> Unit)? = null,
    content: @Composable FlowRowScope.() -> Unit,
) {
    val coroutine = rememberCoroutineScope()
    val headTooltipState = remember { TooltipState() }
    FlowRow(
        modifier = Modifier.padding(horizontal = PaddingLarge)
            .background(backgroundColor)
            .then(modifier),
        verticalArrangement = Arrangement.Center
    ) {
        TooltipBox(
            state = headTooltipState,
            tooltip = {
                PlainTooltip {
                    val rt = model.returnTime
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
                                    Instant.ofEpochMilli(model.dueTime)
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
                            imageVector = if (model.returnTime == null) {
                                if (model.dueTime < now.toEpochMilli()) {
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
        VSpacer()
        Text(
            formatter.format(Instant.ofEpochMilli(model.time).atZone(ZoneId.systemDefault())),
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.align(Alignment.CenterVertically)
        )
        VSpacer()
        content()
        VSpacer()
        Spacer(Modifier.weight(1f))
        if (model.returnTime == null && onReturnRequest != null) {
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
                            onReturnRequest(model)
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

    if (separator) {
        Separator()
    }
}

@Composable
private fun VSpacer() {
    Spacer(Modifier.width(14.dp))
}

@Composable
private fun Separator() {
    Spacer(
        Modifier.fillMaxWidth().height(1.dp).padding(horizontal = PaddingLarge)
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReconstructButton(
    icon: @Composable () -> Unit,
    text: @Composable () -> Unit,
    onReconstructRequest: () -> Unit,
) {
    var openMenu by remember { mutableStateOf(false) }
    TooltipBox(
        positionProvider = rememberComponentRectPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip {
                text()
            }
        }
    ) {
        IconButton(
            onClick = { openMenu = true }
        ) {
            icon()
        }

        DropdownMenu(
            expanded = openMenu,
            onDismissRequest = { openMenu = false }
        ) {
            DropdownMenuItem(
                text = { Text(stringResource(Res.string.reconstruct_para)) },
                onClick = onReconstructRequest,
                leadingIcon = { Icon(imageVector = Icons.Default.Replay, contentDescription = "reconstruct") }
            )
        }
    }
}

private data class JudgeReaderModel(val reader: Reader, val onTime: Boolean, val update: suspend (Float) -> Unit)

@Composable
private fun JudgeReaderDialog(
    model: JudgeReaderModel,
    configurations: Configurations,
    onDismissRequest: () -> Unit,
    onConfirmRequest: (Float) -> Unit,
) {
    val initialStep = remember(model) { if (model.onTime) configurations.creditStep else -configurations.creditStep }
    var step by remember { mutableFloatStateOf(initialStep) }
    val original by remember(
        model.reader,
        configurations
    ) { derivedStateOf { model.reader.getClearCredit(configurations) } }
    val currentReader by remember(model.reader, configurations) {
        derivedStateOf {
            model.reader.copy(
                creditability = calculateCredit(
                    reader = model.reader,
                    configurations = configurations,
                    step = abs(step),
                    positive = step > 0
                ).toFloat()
            )
        }
    }
    val current by remember(currentReader, configurations) {
        derivedStateOf {
            currentReader.getClearCredit(
                configurations
            )
        }
    }

    AlertDialog(
        icon = { Icon(imageVector = Icons.Default.CreditScore, contentDescription = "creditability") },
        title = { Text(stringResource(Res.string.creditability_details_para)) },
        text = {
            Column(Modifier.fillMaxWidth()) {
                CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.titleLarge.copy(fontFamily = FontFamily.Monospace)) {
                    Row(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(original.toFixed(1000).toString())
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                            contentDescription = "credit changed to",
                            modifier = Modifier.padding(horizontal = PaddingMedium)
                        )
                        Text(current.toFixed(1000).toString())
                    }
                }
                Slider(
                    value = step,
                    onValueChange = { step = it },
                    valueRange = -MAX_STEP..MAX_STEP
                )
            }
        },
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(onClick = {
                onConfirmRequest(currentReader.creditability)
                onDismissRequest()
            }) {
                Text(stringResource(Res.string.ok_caption))
            }
        },
        dismissButton = {
            TextButton(onClick = {
                step = initialStep
            }) {
                Text(stringResource(Res.string.reset_para))
            }
        }
    )
}