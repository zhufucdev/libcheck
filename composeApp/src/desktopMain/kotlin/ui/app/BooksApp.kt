@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.onClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import extension.takeIfInstanceOf
import kotlinx.coroutines.launch
import model.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.*
import ui.component.*

@Composable
fun BooksApp(model: AppViewModel) {
    val coroutineScope = rememberCoroutineScope()
    val snackbars = remember { SnackbarHostState() }

    var editMode by remember(model) {
        mutableStateOf<BookEditMode?>(
            model.navigator.current.parameters
                .takeIfInstanceOf<NavigationParameters, ReconstructParameters>()
                ?.let { BookEditMode.Reconstruct(it.identifier) })
    }
    val detailedBook by remember(model) {
        derivedStateOf {
            model.navigator.current.parameters
                .takeIfInstanceOf<NavigationParameters, RevealDetailsParameters>()
                ?.identifier
                ?.let { model.library.getBook(it) }
        }
    }

    Scaffold(
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                Basket(model)
                Spacer(Modifier.width(PaddingLarge))
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(Res.string.new_book_para)) },
                    icon = { Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "") },
                    onClick = { editMode = BookEditMode.Create }
                )
            }
        },
        snackbarHost = { SnackbarHost(snackbars) }
    ) {
        Box(Modifier.padding(it).fillMaxSize()) {
            BookList(
                model = model,
                onBookClicked = { book ->
                    model.navigator.replace(
                        parameters = RevealDetailsParameters(book.id)
                    )
                },
                onBookEditRequest = { book ->
                    editMode = BookEditMode.Overwrite(book)
                },
                onBookDeleted = {
                    coroutineScope.launch {
                        val res = snackbars.showSnackbar(
                            getString(Res.string.is_deleted_para, it.name),
                            getString(Res.string.undo_para)
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            model.library.addBook(it)
                        }
                    }
                }
            )
        }
    }

    detailedBook?.let {
        DetailedBookDialog(
            model = it,
            library = model.library,
            onDismissRequest = {
                model.navigator.replace(RouteType.Books)
            },
            onRevealRequest = {
                model.navigator.push(
                    dest = RouteType.Borrowing,
                    parameters = FilterBorrowParameters(
                        books = listOf(it.id)
                    )
                )
            }
        )
    }

    editMode?.let { mode ->
        EditBookDialog(
            mode = mode,
            library = model.library,
            onDismissRequest = { editMode = null },
            onUpdateRequest = {
                coroutineScope.launch {
                    mode.apply(it, model.library)
                    if (mode is BookEditMode.Reconstruct) {
                        model.navigator.replace()
                    }
                }
            }
        )
    }
}

private sealed interface BookEditMode {
    suspend fun apply(model: Book, library: Library)

    sealed interface SpecificId {
        val identifier: Identifier
    }

    data class Overwrite(val original: Book) : BookEditMode, SpecificId {
        override val identifier: Identifier
            get() = original.id

        override suspend fun apply(model: Book, library: Library) {
            library.updateBook(model)
        }
    }

    data object Create : BookEditMode {
        override suspend fun apply(model: Book, library: Library) {
            library.addBook(model)
        }
    }

    data class Reconstruct(override val identifier: Identifier) : BookEditMode, SpecificId {
        override suspend fun apply(model: Book, library: Library) {
            library.addBook(model)
        }
    }
}

@Composable
private fun EditBookDialog(
    mode: BookEditMode,
    library: Library,
    onDismissRequest: () -> Unit,
    onUpdateRequest: (Book) -> Unit,
) {
    val textColor = MaterialTheme.colorScheme.onSurface

    var bookUri by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var bookIsbn by remember { mutableStateOf("") }
    var bookStock by remember { mutableStateOf("10") }
    var bookStockParsed by remember { mutableStateOf<UInt?>(10u) }

    LaunchedEffect(mode) {
        if (mode is BookEditMode.Overwrite) {
            val o = mode.original
            bookUri = o.avatarUri
            bookTitle = o.name
            bookAuthor = o.name
            bookIsbn = o.isbn
            bookStock = o.stock.toString()
            bookStockParsed = o.stock
        }
    }

    val negativeInStock by remember {
        derivedStateOf {
            val targetStock = bookStockParsed ?: return@derivedStateOf false
            mode is BookEditMode.SpecificId && targetStock < with(library) {
                getBook(mode.identifier)?.let { it.stock - it.getStock() } ?: return@derivedStateOf false
            }
        }
    }
    val canSave by remember { derivedStateOf { bookStockParsed != null && bookTitle.isNotBlank() && !negativeInStock } }
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.BookmarkAdd,
                contentDescription = "",
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = stringResource(
                    when (mode) {
                        is BookEditMode.Create -> Res.string.adding_a_book_para
                        is BookEditMode.Overwrite -> Res.string.editing_a_book_para
                        is BookEditMode.Reconstruct -> Res.string.reconstructing_a_book_para
                    }
                ),
            )
        },
        text = {
            Column {
                AvatarInput(
                    uri = bookUri,
                    onUriChange = { bookUri = it },
                    label = { Text(stringResource(Res.string.cover_para)) },
                    Icons.Default.Book
                )
                OutlinedTextField(
                    value = bookTitle,
                    onValueChange = { bookTitle = it },
                    label = { Text(stringResource(Res.string.title_para)) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = bookAuthor,
                    onValueChange = { bookAuthor = it },
                    label = { Text(stringResource(Res.string.auther_para)) },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = bookIsbn,
                    onValueChange = { bookIsbn = it },
                    label = { Text(stringResource(Res.string.isbn_caption)) },
                    modifier = Modifier.fillMaxWidth(),
                    visualTransformation = {
                        TransformedText(
                            buildAnnotatedString {
                                append(
                                    AnnotatedString(
                                        bookIsbn,
                                        SpanStyle(
                                            color = textColor,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                )
                                val placeholder = "000-0-00-000000-0"
                                append(
                                    AnnotatedString(
                                        placeholder.substring(bookIsbn.length until placeholder.length),
                                        SpanStyle(
                                            color = textColor.variant,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    )
                                )
                            },
                            object : OffsetMapping {
                                override fun originalToTransformed(offset: Int): Int {
                                    return offset
                                }

                                override fun transformedToOriginal(offset: Int): Int {
                                    return minOf(offset, bookIsbn.length)
                                }
                            }
                        )
                    }
                )
                OutlinedTextField(
                    value = bookStock,
                    onValueChange = {
                        bookStockParsed = it.toUIntOrNull()
                        bookStock = it
                    },
                    label = { Text(stringResource(Res.string.stock_para)) },
                    modifier = Modifier.fillMaxWidth(),
                    isError = bookStockParsed == null || negativeInStock,
                    singleLine = true
                )
            }
        },
        confirmButton = {
            TextButton(
                content = { Text(stringResource(Res.string.ok_caption)) },
                onClick = {
                    val book = Book(
                        bookTitle,
                        bookAuthor,
                        bookIsbn,
                        if (mode is BookEditMode.SpecificId) mode.identifier
                        else Identifier(),
                        bookUri,
                        bookStockParsed!!
                    )
                    onUpdateRequest(book)
                    onDismissRequest()
                },
                enabled = canSave,
                modifier = Modifier.padding(6.dp)
            )
        }
    )
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookList(
    model: AppViewModel,
    onBookClicked: (Book) -> Unit,
    onBookEditRequest: (Book) -> Unit,
    onBookDeleted: (Book) -> Unit,
) {
    val library = model.library
    val state = remember { LazyGridState() }
    val coroutine = rememberCoroutineScope()

    LaunchReveal(library.books, model, state)

    if (library.books.isEmpty()) {
        HeadingPlaceholder(
            imageVector = Icons.Default.Book,
            title = { Text(text = stringResource(Res.string.no_books_available_para)) },
            description = { Text(text = stringResource(Res.string.no_books_available_des)) }
        )
    } else {
        Column(Modifier.padding(horizontal = PaddingLarge).padding(top = PaddingLarge)) {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Box {
                    var sorting by remember { mutableStateOf(false) }
                    SortButton(
                        onClick = {
                            sorting = true
                        }
                    )
                    SortMenu(
                        expanded = sorting,
                        onDismissRequest = { sorting = false },
                        sortOrder = library.sorter.bookModel.order,
                        onSortOrderChanged = {
                            coroutine.launch {
                                library.sorter.sortBooks(it)
                            }
                        }
                    ) {
                        SortMenuCaption(stringResource(Res.string.keyword_para))
                        BookSortable.entries.forEach {
                            val selected = library.sorter.bookModel.by == it
                            SortMenuItem(
                                text = { Text(it.label) },
                                icon = {
                                    Icon(
                                        imageVector = if (!selected) Icons.Default.SortByAlpha else Icons.Default.Done,
                                        contentDescription = ""
                                    )
                                },
                                selected = selected,
                                onClick = {
                                    coroutine.launch {
                                        library.sorter.sortBooks(by = it)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(columns = GridCells.Adaptive(240.dp), state = state) {
                library.books.forEach { book ->
                    item(book.id) {
                        Box(modifier = Modifier.animateItemPlacement()) {
                            BookCard(model, book, onBookClicked, onBookEditRequest, onBookDeleted)
                            Text(
                                text = with(library) { book.getStock().toString() },
                                style = MaterialTheme.typography.titleMedium.copy(color = MaterialTheme.colorScheme.onPrimary),
                                textAlign = TextAlign.Center,
                                modifier = Modifier
                                    .sizeIn(minWidth = 44.dp)
                                    .background(
                                        color = MaterialTheme.colorScheme.primary,
                                        shape = CircleShape
                                    )
                                    .padding(10.dp)
                                    .align(Alignment.TopEnd)
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookCard(
    model: AppViewModel,
    book: Book,
    onClick: (Book) -> Unit,
    onEdit: (Book) -> Unit,
    onDeleted: (Book) -> Unit,
) {
    var dragging by remember { mutableStateOf(false) }
    var dragOff by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    var contextMenu by remember { mutableStateOf(false) }
    val cardColor = rememberRevealAnimation(model, book.id)

    LaunchedEffect(dragging) {
        model.draggingIn = dragging
    }

    Card(
        colors = CardDefaults.cardColors(cardColor),
        modifier = Modifier.padding(6.dp)
            .fillMaxSize()
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                contextMenu = true
            }
            .pointerInput(model.basketFabBounds, bounds) {
                detectDragGestures(
                    onDragStart = {
                        dragOff = Offset.Zero
                        dragging = true
                    },
                    onDrag = { _, o ->
                        dragOff += o
                    },
                    onDragEnd = {
                        dragging = false
                        val p = bounds.translate(dragOff)
                        if (!model.basketFabBounds.intersect(p).isEmpty) {
                            model.addToBasket(book)
                        }
                    },
                    onDragCancel = {
                        dragging = false
                    }
                )
            },
        onClick = { onClick(book) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(PaddingLarge).fillMaxSize()
        ) {
            Box {
                BookAvatar(
                    uri = book.avatarUri,
                    modifier =
                    Modifier
                        .size(120.dp)
                        .onGloballyPositioned {
                            bounds = it.boundsInRoot()
                        }
                )

                if (dragging) {
                    Popup {
                        BookAvatar(
                            uri = book.avatarUri,
                            modifier = with(density) {
                                Modifier.size(120.dp).offset(
                                    dragOff.x.toDp(), dragOff.y.toDp()
                                )
                            }
                        )
                    }
                }

                CommonContextMenu(
                    expanded = contextMenu,
                    onDismissRequest = { contextMenu = false },
                    onDelete = {
                        model.library.deleteBook(book)
                        onDeleted(book)
                    },
                    onEdit = {
                        onEdit(book)
                    }
                )
            }
            Text(text = book.name, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
            Text(text = book.author, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun DetailedBookDialog(
    model: Book,
    library: Library,
    onDismissRequest: () -> Unit,
    onRevealRequest: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest,
        content = {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)) {
                Column(Modifier.padding(PaddingLarge * 2)) {
                    Text(
                        text = stringResource(Res.string.about_this_book_para),
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Spacer(Modifier.height(PaddingLarge * 2))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        BookAvatar(
                            uri = model.avatarUri,
                            modifier = Modifier.size(120.dp)
                        )
                        Spacer(Modifier.width(PaddingLarge))
                        Column(Modifier) {
                            val textColor = LocalContentColor.current
                            BasicTextField(
                                value = model.name,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
                                readOnly = true,
                                onValueChange = {}
                            )
                            BasicTextField(
                                value = stringResource(Res.string.by_auther_para, model.author),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(color = textColor),
                                readOnly = true,
                                onValueChange = {}
                            )
                            if (model.isbn.isNotBlank()) {
                                BasicTextField(
                                    value = model.isbn,
                                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                                        fontFamily = FontFamily.Monospace,
                                        color = textColor
                                    ),
                                    readOnly = true,
                                    onValueChange = {}
                                )
                            }
                            Spacer(Modifier.height(PaddingSmall))
                            val available by remember(library) { derivedStateOf { with(library) { model.getStock() } } }
                            Text(
                                text = stringResource(
                                    Res.string.available_borrowed_total_para,
                                    available,
                                    model.stock - available,
                                    model.stock
                                ),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(Modifier.height(PaddingMedium))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextButton(
                            onClick = onRevealRequest
                        ) {
                            Text(stringResource(Res.string.borrows_para))
                        }
                        Spacer(Modifier.weight(1f))
                        TextButton(
                            onClick = onDismissRequest
                        ) {
                            Text(stringResource(Res.string.ok_caption))
                        }
                    }
                }
            }
        }
    )
}