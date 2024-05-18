@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class, ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.onClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import com.sqlmaster.proto.LibraryOuterClass.ReaderTier
import kotlinx.coroutines.launch
import model.AppViewModel
import model.Identifier
import model.Reader
import model.ReaderSortable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.LaunchReveal
import ui.PaddingLarge
import ui.component.*
import ui.stringRes

@Composable
fun ReadersApp(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()
    var addingReader by remember { mutableStateOf(false) }
    var editingReader by remember { mutableStateOf(false) }
    var readerUri by remember { mutableStateOf("") }
    var readerName by remember { mutableStateOf("") }
    var readerTier by remember { mutableStateOf(ReaderTier.TIER_STARTER) }
    var readerId by remember { mutableStateOf<Identifier?>(null) }
    var tierMenuExpanded by remember { mutableStateOf(false) }

    val canSave by remember { derivedStateOf { readerName.isNotBlank() } }

    Scaffold(
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                Basket(model)
                Spacer(Modifier.width(PaddingLarge))
                ExtendedFloatingActionButton(
                    text = { Text(stringResource(Res.string.new_reader_para)) },
                    icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "") },
                    onClick = { addingReader = true }
                )
            }
        }
    ) {
        Box(Modifier.padding(it)) {
            ReaderList(
                model = model,
                onReaderClick = {},
                onEditReader = { reader ->
                    readerId = reader.id
                    readerName = reader.name
                    readerUri = reader.avatarUri
                    readerTier = reader.tier
                    editingReader = true
                }
            )
        }
    }

    if (addingReader || editingReader) {
        AlertDialog(
            icon = {
                Icon(
                    imageVector = Icons.Default.PersonAdd,
                    contentDescription = "",
                    modifier = Modifier.size(24.dp)
                )
            },
            title = {
                Text(
                    text = stringResource(if (addingReader) Res.string.adding_a_reader_para else Res.string.editing_a_reader_para),
                )
            },
            text = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Spacer(modifier = Modifier.width(6.dp))
                    }

                    Spacer(modifier = Modifier.height(PaddingLarge))
                    AvatarInput(
                        uri = readerUri,
                        onUriChange = { readerUri = it },
                        defaultImageVector = Icons.Default.Person,
                        label = { Text(stringResource(Res.string.avatar_para)) }
                    )
                    OutlinedTextField(
                        value = readerName,
                        onValueChange = { readerName = it },
                        label = { Text(stringResource(Res.string.name_para)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(Modifier.height(PaddingLarge))

                    ExposedDropdownMenuBox(
                        expanded = tierMenuExpanded,
                        onExpandedChange = { tierMenuExpanded = it },
                    ) {
                        ExposedDropdownTextField(
                            value = readerTier.stringRes(),
                            label = { Text(stringResource(Res.string.tier_para)) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = tierMenuExpanded,
                            onDismissRequest = { tierMenuExpanded = false },
                        ) {
                            ReaderTier.entries.forEach { tier ->
                                if (tier.ordinal >= ReaderTier.TIER_PLATINUM.ordinal) {
                                    return@forEach
                                }
                                DropdownMenuItem(
                                    text = { Text(tier.stringRes()) },
                                    onClick = {
                                        readerTier = tier
                                        tierMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            onDismissRequest = {
                addingReader = false
                editingReader = false
            },
            confirmButton = {
                TextButton(
                    content = { Text(stringResource(Res.string.ok_caption)) },
                    onClick = {
                        coroutine.launch {
                            if (addingReader) {
                                model.library.addReader(
                                    Reader(
                                        readerName,
                                        Identifier(),
                                        readerUri,
                                        readerTier
                                    )
                                )
                            } else if (editingReader) {
                                model.library.updateReader(
                                    Reader(
                                        readerName,
                                        readerId!!,
                                        readerUri,
                                        readerTier
                                    )
                                )
                            }
                            addingReader = false
                            editingReader = false
                        }
                    },
                    enabled = canSave,
                    modifier = Modifier.padding(6.dp)
                )
            }
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ReaderList(model: AppViewModel, onReaderClick: (Reader) -> Unit, onEditReader: (Reader) -> Unit) {
    val library = model.library
    val coroutine = rememberCoroutineScope()
    val gridState = remember { LazyGridState() }
    var sorting by remember { mutableStateOf(false) }

    LaunchReveal(library.readers, model.reveal, gridState)

    if (library.readers.isEmpty()) {
        HeadingPlaceholder(
            imageVector = Icons.Default.Contacts,
            title = { Text(text = stringResource(Res.string.no_readers_available_para)) },
            description = { Text(text = stringResource(Res.string.no_readers_available_des)) }
        )
    } else {
        Column(Modifier.padding(horizontal = PaddingLarge).padding(top = PaddingLarge)) {
            Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
                Box {
                    SortButton(
                        onClick = { sorting = true },
                    )
                    SortMenu(
                        expanded = sorting,
                        onDismissRequest = { sorting = false },
                        sortOrder = library.sorter.readerModel.order,
                        onSortOrderChanged = {
                            coroutine.launch {
                                library.sorter.sortReaders(it)
                            }
                        }
                    ) {
                        SortMenuCaption(stringResource(Res.string.keyword_para))
                        ReaderSortable.entries.forEach {
                            val selected = library.sorter.readerModel.by == it
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
                                    coroutine.launch {
                                        library.sorter.sortReaders(by = it)
                                    }
                                }
                            )
                        }
                    }
                }
            }

            LazyVerticalGrid(columns = GridCells.Adaptive(200.dp), state = gridState) {
                library.readers.forEach { reader ->
                    item(reader.id) {
                        var contextMenu by remember { mutableStateOf(false) }
                        var bounds by remember { mutableStateOf(Rect.Zero) }
                        val intersection by remember(bounds) { derivedStateOf { bounds.intersect(model.outDraggingBounds) } }
                        val draggingIn by remember(bounds) {
                            derivedStateOf {
                                !intersection.isEmpty
                                        && intersection.height * intersection.width >= model.outDraggingIntersection.let { it.height * it.width }
                            }
                        }
                        val cardColor = rememberRevealAnimation(model, reader.id)

                        LaunchedEffect(draggingIn) {
                            if (draggingIn) {
                                model.outDraggingTarget = reader
                                model.outDraggingIntersection = intersection
                            } else if (model.outDraggingTarget == reader) {
                                model.outDraggingTarget = null
                                model.outDraggingIntersection = Rect.Zero
                            }
                        }

                        Box(
                            Modifier.padding(6.dp).animateItemPlacement()
                                .onGloballyPositioned { bounds = it.boundsInRoot() }) {
                            Card(
                                onClick = { onReaderClick(reader) },
                                colors = if (draggingIn) CardDefaults.outlinedCardColors(
                                    MaterialTheme.colorScheme.primary.copy(
                                        alpha = 0.1f
                                    )
                                ) else CardDefaults.cardColors(containerColor = cardColor),
                                modifier = Modifier.onClick(matcher = PointerMatcher.mouse(PointerButton.Secondary)) {
                                    contextMenu = true
                                },
                            ) {
                                Column(
                                    modifier = Modifier.padding(PaddingLarge).fillMaxWidth(),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Box {
                                        LazyAvatar(
                                            uri = reader.avatarUri,
                                            defaultImageVector = Icons.Default.Person,
                                            modifier = Modifier.size(120.dp)
                                        )

                                        CommonContextMenu(
                                            expanded = contextMenu,
                                            onDismissRequest = { contextMenu = false },
                                            onEdit = {
                                                onEditReader(reader)
                                            },
                                            onDelete = {
                                                model.library.deleteReader(reader)
                                            }
                                        )
                                    }
                                    Text(text = reader.name, style = MaterialTheme.typography.titleMedium)
                                    Text(
                                        text = with(library) {
                                            val b = reader.getBorrows().size
                                            pluralStringResource(Res.plurals.borrows_span, b, b)
                                        },
                                        style = MaterialTheme.typography.bodyMedium
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
