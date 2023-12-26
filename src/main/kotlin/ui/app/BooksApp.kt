package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.darkrockstudios.libraries.mpfilepicker.FilePicker
import kotlinx.coroutines.launch
import model.Book
import model.BookSortable
import model.Identifier
import model.Library
import ui.component.*
import ui.variant
import java.util.*

@Composable
fun BooksApp(library: Library) {
    val coroutine = rememberCoroutineScope()
    val textColor = MaterialTheme.colors.onSurface

    var addingBook by remember { mutableStateOf(false) }
    var editingBook by remember { mutableStateOf(false) }
    var bookId by remember { mutableStateOf<UUID?>(null) }
    var bookUri by remember { mutableStateOf("") }
    var bookTitle by remember { mutableStateOf("") }
    var bookAuthor by remember { mutableStateOf("") }
    var bookIsbn by remember { mutableStateOf("") }
    var bookStock by remember { mutableStateOf("10") }
    var bookStockParsed by remember { mutableStateOf<UInt?>(10u) }

    var showFilePicker by remember { mutableStateOf(false) }
    val canSave by remember { derivedStateOf { bookStockParsed != null && bookTitle.isNotBlank() } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New Book") },
                icon = { Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "") },
                onClick = { addingBook = true }
            )
        }
    ) {
        Box(Modifier.padding(it)) {
            BookList(library) { book ->
                bookId = book.id.uuid
                bookUri = book.avatarUri
                bookTitle = book.name
                bookAuthor = book.author
                bookIsbn = book.isbn
                bookStock = book.stock.toString()
                editingBook = true
            }
        }
    }

    FilePicker(
        show = showFilePicker,
        fileExtensions = listOf("png", "jpg", "jpeg", "webp", "tiff", "bmp"),
    ) {
        bookUri = it?.let { "file://${it.path}" } ?: bookUri
        showFilePicker = false
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
                        Row(modifier = Modifier.height(100.dp)) {
                            BookAvatar(uri = bookUri, modifier = Modifier.size(100.dp))
                            Spacer(Modifier.width(6.dp))
                            Column {
                                OutlinedTextField(
                                    value = bookUri,
                                    onValueChange = { bookUri = it },
                                    label = { Text("Cover") },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true
                                )
                                TextButton(
                                    content = {
                                        Icon(imageVector = Icons.Default.FileOpen, contentDescription = "")
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Select file")
                                    },
                                    onClick = {
                                        showFilePicker = true
                                    },
                                    modifier = Modifier.align(Alignment.End)
                                )
                            }
                        }
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
                            fun getBook() =
                                Book(
                                    bookTitle,
                                    bookAuthor,
                                    bookIsbn,
                                    Identifier(if (editingBook) bookId!! else UUID.randomUUID()),
                                    bookUri,
                                    bookStockParsed!!
                                )

                            if (addingBook) {
                                library.addBook(getBook())
                            } else {
                                library.updateBook(getBook())
                            }
                            coroutine.launch {
                                library.writeToFile()
                            }
                            addingBook = false
                            editingBook = false
                        },
                        enabled = canSave
                    )
                    Spacer(Modifier.width(6.dp))
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
private fun BookList(library: Library, onBookClicked: (Book) -> Unit) {
    var sorting by remember { mutableStateOf(false) }
    var sortButtonPos by remember { mutableStateOf(DpOffset(0.dp, 0.dp)) }
    val coroutine = rememberCoroutineScope()


    Column(Modifier.padding(12.dp)) {
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
                        icon = { Icon(imageVector = if (!selected) Icons.Default.SortByAlpha else Icons.Default.Done, contentDescription = "") },
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

        LazyVerticalGrid(columns = GridCells.Fixed(3)) {
            library.bookList.items.forEach { book ->
                item(book.id) {
                    Box(modifier = Modifier.animateItemPlacement()) {
                        OutlinedCard(
                            modifier = Modifier.padding(6.dp).fillMaxSize(),
                            onClick = { onBookClicked(book) }
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.padding(12.dp).fillMaxSize()
                            ) {
                                BookAvatar(book.avatarUri, modifier = Modifier.size(60.dp))
                                Text(text = book.name, style = MaterialTheme.typography.h6)
                                Text(text = book.author, style = MaterialTheme.typography.body2)
                            }
                        }

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

@Composable
private fun BookAvatar(uri: String, modifier: Modifier = Modifier) {
    if (uri.isNotBlank()) {
        AsyncImage(
            uri,
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.Book,
            contentDescription = "",
            modifier = modifier
        )
    }
}

