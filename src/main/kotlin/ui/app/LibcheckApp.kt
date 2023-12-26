package ui.app

import model.Library
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
import kotlin.time.Duration.Companion.seconds

enum class Route(val label: String, val icon: ImageVector) {
    BOOKS("Books", Icons.Default.LibraryBooks),
    READERS("Readers", Icons.Default.Contacts),
    BORROWING("Borrowing", Icons.Default.Key)
}

@Composable
fun LibcheckApp(library: Library) {
    var route by remember { mutableStateOf(Route.BOOKS) }
    LaunchedEffect(library) {
        delay(0.5.seconds)
        library.initialize()
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
                    visible = !library.initialized || library.saving,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (library.loadProgress <= 0) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    } else {
                        LinearProgressIndicator(progress = library.loadProgress, modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    ) {
        AnimatedVisibility(
            visible = library.initialized,
            enter = fadeIn() + slideIn { IntOffset(0, it.height / 3) },
            exit = fadeOut()
        ) {
            Row {
                PermanentDrawerSheet {
                    NavigationItems(route) { route = it }
                }
                when (route) {
                    Route.BOOKS -> BooksApp(library)
                    Route.READERS -> ReadersApp(library)
                    Route.BORROWING -> BorrowingApp(library)
                }
            }
        }
        AnimatedVisibility(
            visible = !library.initialized,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            InitializationPlaceholder()
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
private fun NavigationItems(route: Route, onNavigation: (Route) -> Unit) {
    Spacer(modifier = Modifier.height(12.dp))
    Route.entries.forEach {
        NavigationDrawerItem(
            label = { Text(it.label) },
            onClick = { onNavigation(it) },
            icon = { Icon(imageVector = it.icon, contentDescription = "") },
            selected = route == it
        )
    }
}