package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedCard
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import model.*
import ui.component.*

@Composable
fun ReadersApp(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()
    var addingReader by remember { mutableStateOf(false) }
    var editingReader by remember { mutableStateOf(false) }
    var readerUri by remember { mutableStateOf("") }
    var readerName by remember { mutableStateOf("") }
    var readerId by remember { mutableStateOf<Identifier?>(null) }

    val canSave by remember { derivedStateOf { readerName.isNotBlank() } }

    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New Reader") },
                icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "") },
                onClick = { addingReader = true }
            )
        }
    ) {
        Box(Modifier.padding(it)) {
            ReaderList(model.library) { reader ->
                readerId = reader.id
                readerName = reader.name
                readerUri = reader.avatarUri
                editingReader = true
            }
        }
    }

    if (addingReader || editingReader) {
        AlertDialog(
            text = {
                CompositionLocalProvider(LocalContentAlpha provides 1f) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.PersonAdd,
                                contentDescription = "",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = if (addingReader) "Adding a reader" else "Editing a reader",
                                style = MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onSurface),
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        AvatarInput(
                            uri = readerUri,
                            onUriChange = { readerUri = it },
                            defaultImageVector = Icons.Default.Person,
                            label = { Text("Avatar") }
                        )
                        OutlinedTextField(
                            value = readerName,
                            onValueChange = { readerName = it },
                            label = { Text("Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            onDismissRequest = {
                addingReader = false
                editingReader = false
            },
            buttons = {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        content = { Text("Save") },
                        onClick = {
                            if (addingReader) {
                                model.library.addReader(Reader(readerName, Identifier(), readerUri))
                            } else {
                                model.library.updateReader(Reader(readerName, readerId!!, readerUri))
                            }
                            coroutine.launch {
                                model.library.writeToFile()
                            }
                            addingReader = false
                            editingReader = false
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
private fun ReaderList(library: Library, onReaderClick: (Reader) -> Unit) {
    val coroutine = rememberCoroutineScope()
    var sorting by remember { mutableStateOf(false) }

    Column(Modifier.padding(horizontal = 12.dp).padding(top = 12.dp)) {
        Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
            Box {
                SortButton(
                    onClick = { sorting = true },
                )
                SortMenu(
                    expanded = sorting,
                    onDismissRequest = { sorting = false },
                    sortOrder = library.readerList.sortOrder,
                    onSortOrderChanged = {
                        library.sortReaders(it, library.readerList.sortedBy)
                        coroutine.launch {
                            library.writeToFile()
                        }
                    }
                ) {
                    SortMenuCaption("Keyword")
                    ReaderSortable.entries.forEach {
                        val selected = library.readerList.sortedBy == it
                        SortMenuItem(
                            text = { Text(it.label) },
                            icon = {
                                Icon(
                                    imageVector = if (selected) Icons.Default.Done else Icons.Default.SortByAlpha,
                                    contentDescription = ""
                                )
                            },
                            selected = selected,
                            onClick = {
                                library.sortReaders(library.readerList.sortOrder, it)
                                coroutine.launch {
                                    library.writeToFile()
                                }
                            }
                        )
                    }
                }
            }
        }

        LazyVerticalGrid(columns = GridCells.Adaptive(200.dp)) {
            library.readerList.items.forEach { reader ->
                item(reader.id) {
                    Box(Modifier.padding(6.dp).animateItemPlacement()) {
                        OutlinedCard(onClick = { onReaderClick(reader) }) {
                            Column(
                                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                LazyAvatar(
                                    uri = reader.avatarUri,
                                    defaultImageVector = Icons.Default.Person,
                                    modifier = Modifier.size(120.dp)
                                )
                                Text(text = reader.name, style = MaterialTheme.typography.h6)
                                Text(
                                    text = with(library) {
                                        val b = reader.borrows
                                        if (b > 1) {
                                            "${reader.borrows} borrows"
                                        } else {
                                            "${reader.borrows} borrow"
                                        }
                                    },
                                    style = MaterialTheme.typography.caption
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
