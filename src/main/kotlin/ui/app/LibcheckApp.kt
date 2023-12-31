package ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import model.*
import ui.WindowSize
import kotlin.time.Duration.Companion.seconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibcheckApp(model: AppViewModel) {
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    val searchResult = remember { mutableStateListOf<Searchable>() }

    LaunchedEffect(model.library) {
        delay(0.5.seconds)
        model.library.initialize()
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
            Column(Modifier.fillMaxWidth()) {
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
                    modifier = Modifier.widthIn(min = 500.dp).align(Alignment.CenterHorizontally)
                )
                AnimatedVisibility(
                    visible = !model.library.initialized || model.library.saving,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (model.library.loadProgress <= 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(
                            progress = model.library.loadProgress,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (model.windowSize < WindowSize.WIDE) {
                BottomNavigation {
                    BottomNavigationItems(model.route) {
                        model.route = it
                    }
                }
            }
        }
    ) {
        Box(Modifier.padding(it)) {
            AnimatedVisibility(
                visible = model.library.initialized,
                enter = fadeIn() + slideIn { IntOffset(0, it.height / 3) },
                exit = fadeOut()
            ) {
                if (model.windowSize >= WindowSize.WIDE) {
                    Row {
                        PermanentDrawerSheet {
                            NavigationDrawerItems(model.route) { model.route = it }
                        }
                        MainContent(model)
                    }
                } else {
                    MainContent(model)
                }
            }
            AnimatedVisibility(
                visible = !model.library.initialized,
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
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Loading library",
            style = MaterialTheme.typography.h5
        )
    }
}

@Composable
private fun NavigationDrawerItems(current: Route, onNavigation: (Route) -> Unit) {
    Spacer(modifier = Modifier.height(12.dp))
    Route.entries.forEach {
        NavigationDrawerItem(
            label = { Text(it.label) },
            onClick = { onNavigation(it) },
            icon = { Icon(imageVector = it.icon, contentDescription = "") },
            selected = current == it
        )
    }
}

@Composable
private fun RowScope.BottomNavigationItems(current: Route, onNavigation: (Route) -> Unit) {
    Route.entries.forEach {
        BottomNavigationItem(
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
            modifier = Modifier.padding(12.dp)
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