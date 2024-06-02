@file:Suppress("FunctionName")
@file:OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.PointerMatcher
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.onClick
import androidx.compose.foundation.text.BasicTextField
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
import extension.getClearCredit
import extension.takeIfInstanceOf
import extension.toFixed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.*
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringArrayResource
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.*
import ui.component.*

@Composable
fun ReadersApp(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()
    val detailedReader by remember(model) {
        derivedStateOf {
            model.navigator.current.parameters
                .takeIfInstanceOf<NavigationParameters, RevealDetailsParameters>()
                ?.identifier
                ?.let { model.library.getReader(it) }
        }
    }
    val reconstructId by remember(model) {
        derivedStateOf {
            model.navigator.current.parameters
                .takeIfInstanceOf<NavigationParameters, ReconstructParameters>()
                ?.identifier
        }
    }

    var editMode by remember(reconstructId) {
        mutableStateOf<ReaderEditMode?>(reconstructId?.let { i ->
            model.library.components.of<ModificationCapability>()
                ?.let { l ->
                    ReaderEditMode.Reconstruct(i, l)
                }
        })
    }
    val snackbars = remember { SnackbarHostState() }

    Scaffold(
        floatingActionButton = {
            Row(horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.Bottom) {
                model.library.components.of<BorrowCapability>()
                    ?.let {
                        Basket(model, it)
                    }
                Spacer(Modifier.width(PaddingLarge))
                model.library.components.of<ModificationCapability>()
                    ?.let {
                        ExtendedFloatingActionButton(
                            text = { Text(stringResource(Res.string.new_reader_para)) },
                            icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "") },
                            onClick = { editMode = ReaderEditMode.Create(it) }
                        )
                    }
            }
        },
        snackbarHost = { SnackbarHost(snackbars) }
    ) {
        Box(Modifier.padding(it)) {
            ReaderList(
                model = model,
                onReaderClick = { reader ->
                    model.navigator.replace(parameters = RevealDetailsParameters(reader.id))
                },
                onEditReaderRequest = { reader, library ->
                    editMode = ReaderEditMode.Overwrite(reader, library)
                },
                onReaderDeleted = { reader, library ->
                    coroutine.launch {
                        val res = snackbars.showSnackbar(
                            getString(Res.string.is_deleted_para, reader.name),
                            getString(Res.string.undo_para)
                        )
                        if (res == SnackbarResult.ActionPerformed) {
                            library.addReader(reader)
                        }
                    }
                }
            )
        }
    }

    editMode?.let { mode ->
        EditReaderDialog(
            mode = mode,
            onDismissRequest = { editMode = null },
            onUpdateRequest = {
                coroutine.launch {
                    mode.apply(it)
                    if (reconstructId != null) {
                        model.navigator.replace()
                    }
                }
            }
        )
    }

    detailedReader?.let {
        DetailedReaderDialog(
            model = it,
            library = model.library,
            configurations = model.configurations,
            onDismissRequest = { model.navigator.replace() },
            onRevealRequest = {
                model.navigator.push(
                    RouteType.Borrowing,
                    FilterBorrowParameters(
                        readers = listOf(it.id)
                    )
                )
            }
        )
    }
}

private sealed interface ReaderEditMode {
    suspend fun apply(model: Reader)

    sealed interface SpecificId {
        val identifier: UuidIdentifier
    }

    data class Overwrite(val original: Reader, val library: ModificationCapability) : ReaderEditMode,
        SpecificId {
        override val identifier: UuidIdentifier
            get() = original.id

        override suspend fun apply(model: Reader) {
            library.updateReader(model)
        }
    }

    data class Create(val library: ModificationCapability) : ReaderEditMode {
        override suspend fun apply(model: Reader) {
            library.addReader(model)
        }
    }

    data class Reconstruct(override val identifier: UuidIdentifier, val library: ModificationCapability) :
        ReaderEditMode, SpecificId {
        override suspend fun apply(model: Reader) {
            library.addReader(model)
        }
    }
}

@Composable
private fun EditReaderDialog(
    mode: ReaderEditMode,
    onDismissRequest: () -> Unit,
    onUpdateRequest: (Reader) -> Unit,
) {
    var readerUri by remember { mutableStateOf("") }
    var readerName by remember { mutableStateOf("") }
    var readerTier by remember { mutableStateOf(ReaderTier.TIER_STARTER) }
    var tierMenuExpanded by remember { mutableStateOf(false) }

    val canSave by remember { derivedStateOf { readerName.isNotBlank() } }

    LaunchedEffect(mode) {
        if (mode is ReaderEditMode.Overwrite) {
            val o = mode.original
            readerName = o.name
            readerUri = o.avatarUri
            readerTier = o.tier
        }
    }

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
                text = stringResource(
                    when (mode) {
                        is ReaderEditMode.Create -> Res.string.adding_a_reader_para
                        is ReaderEditMode.Overwrite -> Res.string.editing_a_reader_para
                        is ReaderEditMode.Reconstruct -> Res.string.reconstructing_a_reader_para
                    }
                ),
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
                            if (tier.ordinal > ReaderTier.TIER_PLATINUM.ordinal) {
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
        onDismissRequest = onDismissRequest,
        confirmButton = {
            TextButton(
                content = { Text(stringResource(Res.string.ok_caption)) },
                onClick = {
                    onUpdateRequest(
                        Reader(
                            readerName,
                            if (mode is ReaderEditMode.SpecificId) mode.identifier
                            else UuidIdentifier(),
                            readerUri,
                            readerTier
                        )
                    )
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
private fun ReaderList(
    model: AppViewModel,
    onReaderClick: (Reader) -> Unit,
    onEditReaderRequest: (Reader, ModificationCapability) -> Unit,
    onReaderDeleted: (Reader, ModificationCapability) -> Unit,
) {
    val library = model.library
    val coroutine = rememberCoroutineScope()
    val gridState = remember { LazyGridState() }
    var sorting by remember { mutableStateOf(false) }

    LaunchReveal(library.readers, model, gridState)

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
                                        ReaderAvatar(reader.avatarUri)
                                        model.library.components.of<ModificationCapability>()
                                            ?.let { library ->
                                                CommonContextMenu(
                                                    expanded = contextMenu,
                                                    onDismissRequest = { contextMenu = false },
                                                    onEdit = {
                                                        onEditReaderRequest(reader, library)
                                                    },
                                                    onDelete = {
                                                        withContext(Dispatchers.IO) {
                                                            library.deleteReader(reader)
                                                            onReaderDeleted(reader, library)
                                                        }
                                                    }
                                                )
                                            }
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

@Composable
private fun DetailedReaderDialog(
    model: Reader,
    library: Library,
    configurations: Configurations,
    onDismissRequest: () -> Unit,
    onRevealRequest: () -> Unit,
) {
    BasicAlertDialog(
        onDismissRequest = onDismissRequest
    ) {
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceBright)) {
            Column(Modifier.padding(PaddingLarge * 2)) {
                Text(
                    text = stringResource(Res.string.about_this_reader_para),
                    style = MaterialTheme.typography.titleLarge,
                )
                Spacer(Modifier.height(PaddingLarge * 2))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    ReaderAvatar(model.avatarUri)
                    Spacer(Modifier.width(PaddingLarge))
                    Column {
                        val textColor = LocalContentColor.current
                        BasicTextField(
                            value = model.name,
                            textStyle = MaterialTheme.typography.titleLarge.copy(color = textColor),
                            readOnly = true,
                            onValueChange = {}
                        )
                        Text(
                            text = stringArrayResource(Res.array.tier_reader_para)[model.tier.number],
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(PaddingSmall))
                        Text(
                            text = with(library) {
                                val b = model.getBorrows().size
                                val cc = model.getClearCredit(configurations).toFixed(1000).toString()
                                pluralStringResource(Res.plurals.borrows_creditability_span, b, b, cc)
                            },
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
}

@Composable
private fun ReaderAvatar(uri: String) {
    LazyAvatar(
        uri = uri,
        defaultImageVector = Icons.Default.Person,
        modifier = Modifier.size(120.dp)
    )
}