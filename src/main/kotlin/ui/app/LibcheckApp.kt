package ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideIn
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import model.AppViewModel
import ui.WindowSize
import kotlin.time.Duration.Companion.seconds

enum class Route(val label: String, val icon: ImageVector) {
    BOOKS("Books", Icons.Default.LibraryBooks),
    READERS("Readers", Icons.Default.Contacts),
    BORROWING("Borrowing", Icons.Default.Key)
}

@Composable
fun LibcheckApp(model: AppViewModel) {
    LaunchedEffect(model.library) {
        delay(0.5.seconds)
        model.library.initialize()
    }

    Scaffold(
        topBar = {
            Column {
                TopAppBar(
                    title = { Text("Lib Check") },
                    actions = {
                        IconButton(
                            content = { Icon(imageVector = Icons.Default.Search, contentDescription = "") },
                            onClick = {

                            }
                        )
                    }
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