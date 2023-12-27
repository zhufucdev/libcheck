package ui.app

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animate
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.*
import ui.component.*
import ui.variant
import kotlin.time.Duration.Companion.seconds

@Composable
fun BooksApp(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()
    val textColor = MaterialTheme.colors.onSurface

    var addingBook by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf(false) }
    var bookId by remember { mutableStateOf<Identifier?>(null) }
    var bookUri by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var bookIsbn by remember { mutableStateOf("") }
    var bookStock by remember { mutableStateOf("10") }
    var bookStockParsed by remember { mutableStateOf<UInt?>(10u) }

    val canSave by remember { derivedStateOf { bookStockParsed != null && bookTitle.isNotBlank() } }

    Scaffold(
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                Basket(model)
                Spacer(Modifier.width(12.dp))
                ExtendedFloatingActionButton(
                    text = { Text("New Book") },
                    icon = { Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "") },
                    onClick = { addingBook = true }
                )
            }
        }
    ) {
        Box(Modifier.padding(it)) {
            BookList(model) { book ->
                bookId = book.id
                bookUri = book.avatarUri
                bookTitle = book.name
                bookAuthor = book.author
                bookIsbn = book.isbn
                bookStock = book.stock.toString()
                editingBook = true
            }
        }
    }

    if (addingBook || editingBook) {
        AlertDialog(
            onDismissRequest = {
                addingBook = false
                editingBook = false
            },
            text = {
                CompositionLocalProvider(LocalContentAlpha provides 1f) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.BookmarkAdd,
                                contentDescription = "",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (addingBook) "Adding a book" else "Editing a book",
                                style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onSurface),
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        AvatarInput(
                            uri = bookUri,
                            onUriChange = { bookUri = it },
                            label = { Text("Cover") },
                            Icons.Default.Book
                        )
                        OutlinedTextField(
                            value = bookTitle,
                            onValueChange = { bookTitle = it },
                            label = { Text("Title") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true
                        )
                        OutlinedTextField(
                            value = bookAuthor,
                            onValueChange = { bookAuthor = it },
                            label = { Text("Author") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = bookIsbn,
                            onValueChange = { bookIsbn = it },
                            label = { Text("ISBN") },
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
                            label = { Text("Stock") },
                            modifier = Modifier.fillMaxWidth(),
                            isError = bookStockParsed == null,
                            singleLine = true
                        )
                    }
                }
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        content = { Text("Save") },
                        onClick = {
                            val book = Book(
                                bookTitle,
                                bookAuthor,
                                bookIsbn,
                                if (editingBook) bookId!! else Identifier(),
                                bookUri,
                                bookStockParsed!!
                            )

                            if (addingBook) {
                                model.library.addBook(book)
                            } else {
                                model.library.updateBook(book)
                            }
                            coroutine.launch {
                                model.library.writeToFile()
                            }
                            addingBook = false
                            editingBook = false
                        },
                        enabled = canSave,
                        modifier = Modifier.padding(6.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookList(model: AppViewModel, onBookClicked: (Book) -> Unit) {
    val library = model.library
    val state = remember { LazyGridState() }
    var sorting by remember { mutableStateOf(false) }
    var sortButtonPos by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    val coroutine = rememberCoroutineScope()

    LaunchedEffect(model.reveal) {
        val reveal = model.reveal ?: return@LaunchedEffect
        val idx = model.library.bookList.items.indexOfFirst { it.id == reveal }
        if (idx > 0) {
            state.animateScrollToItem(idx)
        }
    }

    Column(Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            SortMenu(
                expanded = sorting,
                offset = sortButtonPos,
                onDismissRequest = { sorting = false },
                sortOrder = library.bookList.sortOrder,
                onSortOrderChanged = {
                    library.sortBooks(it, library.bookList.sortedBy)
                    coroutine.launch {
                        library.writeToFile()
                    }
                }
            ) {
                SortMenuCaption("Keyword")
                BookSortable.entries.forEach {
                    val selected = library.bookList.sortedBy == it
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
                            library.sortBooks(library.bookList.sortOrder, it)
                            coroutine.launch {
                                library.writeToFile()
                            }
                        }
                    )
                }
            }
            SortButton(
                onClick = {
                    sorting = true
                },
                modifier = Modifier.onGloballyPositioned {
                    sortButtonPos = it.positionInParent().let { w ->
                        DpOffset(w.x.dp, (w.y - it.boundsInParent().height).dp)
                    }
                }
            )
        }

        LazyVerticalGrid(columns = GridCells.Adaptive(240.dp), state = state) {
            library.bookList.items.forEach { book ->
                item(book.id) {
                    Box(modifier = Modifier.animateItemPlacement()) {
                        BookCard(model, book) { onBookClicked(it) }
                        Text(
                            text = with(library) { book.inStock.toString() },
                            style = MaterialTheme.typography.button.copy(color = MaterialTheme.colors.onPrimary),
                            modifier = Modifier.background(
                                color = MaterialTheme.colors.primarySurface,
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun BookCard(model: AppViewModel, book: Book, onClicked: (Book) -> Unit) {
    val coroutine = rememberCoroutineScope()
    var dragging by remember { mutableStateOf(false) }
    var dragOff by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    var contextMenu by remember { mutableStateOf(false) }
    val cardColor = rememberRevealAnimation(model, book.id)

    LaunchedEffect(dragging) {
        model.draggingIn = dragging
    }

    OutlinedCard(
        modifier = Modifier.padding(6.dp)
            .fillMaxSize()
            .onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                contextMenu = true
            }
            .pointerInput(true) {
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
        colors = CardDefaults.outlinedCardColors(cardColor),
        onClick = { onClicked(book) }
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(12.dp).fillMaxSize()
        ) {
            Box {
                LazyAvatar(
                    uri = book.avatarUri,
                    defaultImageVector = Icons.Default.Book,
                    modifier = Modifier.size(120.dp).onGloballyPositioned {
                        bounds = it.boundsInRoot()
                    }
                )

                if (dragging) {
                    Popup {
                        LazyAvatar(
                            uri = book.avatarUri,
                            defaultImageVector = Icons.Default.Book,
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
                        coroutine.launch {
                            model.library.writeToFile()
                        }
                    },
                )
            }
            Text(text = book.name, style = MaterialTheme.typography.h6, textAlign = TextAlign.Center)
            Text(text = book.author, style = MaterialTheme.typography.body2)
        }
    }
}