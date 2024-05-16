@file:Suppress("FunctionName")

package ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
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
import model.*
import ui.PaddingLarge
import ui.PaddingMedium
import ui.WindowSize

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibcheckApp(model: AppViewModel, windowSize: WindowSize) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val searchResult = remember { mutableStateListOf<Searchable>() }

    LaunchedEffect(model.library) {
        model.library.connect()
    }

    LaunchedEffect(searchQuery) {
        searchResult.clear()
        model.library.search(searchQuery)
            .collect {
                searchResult.add(it)
            }
    }

    Scaffold(
        topBar = {
            Column(
                Modifier.fillMaxWidth()
            ) {
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
                                        model.reveal = it.id
                                        model.route = when (it) {
                                            is Reader -> Route.READERS
                                            is Book -> Route.BOOKS
                                            is BorrowInstanced -> Route.BORROWING
                                            else -> throw IllegalArgumentException()
                                        }
                                    }
                                }
                            }
                        }
                    },
                    leadingIcon = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
                    onSearch = {},
                    modifier = Modifier.widthIn(min = 500.dp)
                        .align(Alignment.CenterHorizontally)
                        .padding(bottom = PaddingMedium)
                )
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
                    BottomNavigationItems(model.route) {
                        model.route = it
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
                            NavigationDrawerItems(model.route) { next -> model.route = next }
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
private fun NavigationDrawerItems(current: Route, onNavigation: (Route) -> Unit) {
    Spacer(Modifier.height(PaddingLarge))
    Column(Modifier.padding(horizontal = PaddingMedium)) {
        Route.entries.forEach {
            NavigationDrawerItem(
                label = { Text(it.label) },
                onClick = { onNavigation(it) },
                icon = { Icon(imageVector = it.icon, contentDescription = "") },
                selected = current == it
            )
        }
    }
}

@Composable
private fun RowScope.BottomNavigationItems(current: Route, onNavigation: (Route) -> Unit) {
    Route.entries.forEach {
        NavigationBarItem(
            selected = current == it,
            onClick = { onNavigation(it) },
            icon = { Icon(it.icon, "") },
            label = { Text(it.label) }
        )
    }
}

@Composable
private fun MainContent(model: AppViewModel) {
    when (model.route) {
        Route.BOOKS -> BooksApp(model)
        Route.READERS -> ReadersApp(model)
        Route.BORROWING -> BorrowingApp(model)
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