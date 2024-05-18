@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.animation.*
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import kotlinx.coroutines.flow.toCollection
import model.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.go_para
import ui.PaddingLarge
import ui.PaddingMedium
import ui.WindowSize
import ui.component.ConnectionAlertDialog
import ui.component.NavigateUpButton

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibcheckApp(model: AppViewModel, windowSize: WindowSize) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val searchResult = remember { mutableStateListOf<Searchable>() }
    var connectionException by remember { mutableStateOf<Exception?>(null) }

    LaunchedEffect(model.library) {
        connectionException = null
        try {
            model.library.connect()
        } catch (e: Exception) {
            connectionException = e
        }
    }

    LaunchedEffect(searchQuery) {
        searchResult.clear()
        model.library.search(searchQuery).toCollection(searchResult)
    }

    Scaffold(
        topBar = {
            Column(
                Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(bottom = PaddingLarge),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AnimatedVisibility(
                        visible = !isSearching && model.navigator.canGoBack,
                        enter = expandHorizontally(),
                        exit = shrinkHorizontally()
                    ) {
                        NavigateUpButton {
                            model.navigator.pop()
                        }
                    }

                    SearchBar(
                        query = searchQuery,
                        active = isSearching,
                        onActiveChange = { isSearching = it },
                        onQueryChange = { searchQuery = it },
                        content = {
                            LazyColumn {
                                searchResult.forEach {
                                    item(it.id) {
                                        SearchResult(it, model) {
                                            isSearching = false
                                            model.navigator.push(
                                                when (it) {
                                                    is Reader -> RouteType.Readers
                                                    is Book -> RouteType.Books
                                                    is BorrowInstanced -> RouteType.Borrowing
                                                    else -> throw IllegalArgumentException()
                                                },
                                                RevealParameters(it.id)
                                            )
                                        }
                                    }
                                }
                            }
                        },
                        leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "search icon") },
                        onSearch = {},
                        modifier = (if (windowSize <= WindowSize.WIDE) Modifier.weight(1f) else Modifier),
                    )
                    AnimatedVisibility(!isSearching, enter = expandHorizontally(), exit = shrinkHorizontally()) {
                        IconButton(
                            onClick = { model.navigator.push(RouteType.Preferences) },
                            modifier = Modifier.padding(end = PaddingMedium)
                        ) {
                            TooltipBox(
                                tooltip = {
                                    PlainTooltip {
                                        Text(stringResource(RouteType.Preferences.label))
                                    }
                                },
                                positionProvider = rememberComponentRectPositionProvider(),
                                state = rememberTooltipState()
                            ) {
                                Icon(imageVector = RouteType.Preferences.icon, contentDescription = "preferences")
                            }
                        }
                    }
                }
                AnimatedVisibility(
                    visible = model.library.state !is LibraryState.Idle,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    val progress by remember {
                        derivedStateOf {
                            when (val curr = model.library.state) {
                                is LibraryState.HasProgress -> curr.progress
                                else -> 0f
                            }
                        }
                    }
                    if (progress <= 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (windowSize < WindowSize.WIDE) {
                NavigationBar {
                    BottomNavigationItems(model.navigator.current.type) {
                        model.navigator.replace(it)
                    }
                }
            }
        }
    ) {
        Box(Modifier.padding(it)) {
            AnimatedVisibility(
                visible = model.library.state !is LibraryState.Initializing,
                enter = fadeIn() + slideIn { IntOffset(0, it.height / 3) },
                exit = fadeOut()
            ) {
                if (windowSize >= WindowSize.WIDE) {
                    Row {
                        PermanentDrawerSheet {
                            NavigationDrawerItems(model.navigator.current.type) { next -> model.navigator.replace(next) }
                        }
                        MainContent(model)
                    }
                } else {
                    MainContent(model)
                }
            }
            AnimatedVisibility(
                visible = model.library.state is LibraryState.Initializing,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                InitializationPlaceholder()
            }
        }
    }

    connectionException?.let {
        ConnectionAlertDialog(
            exception = it,
            confirmButton = {
                TextButton(onClick = { model.navigator.push(RouteType.Preferences) }) {
                    Text(stringResource(Res.string.go_para))
                }
            }
        )
    }
}

@Composable
private fun InitializationPlaceholder() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = Icons.Default.ContentPasteSearch,
            contentDescription = "",
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.height(PaddingLarge))
        Text(
            text = "Loading library",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun NavigationDrawerItems(current: RouteType, onNavigation: (RouteType) -> Unit) {
    Spacer(Modifier.height(PaddingLarge))
    Column(Modifier.padding(horizontal = PaddingMedium)) {
        RouteType.entries.forEach {
            if (it.docked) {
                NavigationDrawerItem(
                    label = { Text(stringResource(it.label)) },
                    onClick = { onNavigation(it) },
                    icon = { Icon(imageVector = it.icon, contentDescription = "") },
                    selected = current == it
                )
            }
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItems(current: RouteType, onNavigation: (RouteType) -> Unit) {
    RouteType.entries.forEach {
        if (it.docked) {
            NavigationBarItem(
                selected = current == it,
                onClick = { onNavigation(it) },
                icon = { Icon(it.icon, "") },
                label = { Text(stringResource(it.label)) }
            )
        }
    }
}

@Composable
private fun MainContent(model: AppViewModel) {
    when (model.navigator.current.type) {
        RouteType.Books -> BooksApp(model)
        RouteType.Readers -> ReadersApp(model)
        RouteType.Borrowing -> BorrowingApp(model)
        else -> {}
    }
}

@Composable
fun SearchResult(result: Searchable, model: AppViewModel, onClick: () -> Unit) {
    Box(
        modifier = Modifier.fillMaxWidth().clickable { onClick() }
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(PaddingLarge)
        ) {
            Icon(
                imageVector = when (result) {
                    is Reader -> Icons.Default.Person
                    is Book -> Icons.Default.Book
                    is BorrowInstanced -> Icons.Default.Key
                    else -> Icons.Default.QuestionMark
                },
                contentDescription = ""
            )
            Spacer(Modifier.width(8.dp))
            Text(result.name)
        }
    }
}