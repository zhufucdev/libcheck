package model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.window.WindowState
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.flow
import ui.WindowSize

enum class Route(val label: String, val icon: ImageVector) {
    BOOKS("Books", Icons.AutoMirrored.Filled.LibraryBooks),
    READERS("Readers", Icons.Default.Contacts),
    BORROWING("Borrowing", Icons.Default.Key)
}

class AppViewModel(val library: Library, val windowState: WindowState, val windowSize: WindowSize, route: MutableState<Route>) {
    var route by route
    var reveal by mutableStateOf<Identifier?>(null)

    val booksInBasket = mutableStateListOf<Identifier>()
    var basketFabBounds by mutableStateOf(Rect.Zero)
    var draggingIn by mutableStateOf(false)
    var outDraggingBounds by mutableStateOf(Rect.Zero)
    var outDraggingIntersection by mutableStateOf(Rect.Zero)
    var outDraggingTarget by mutableStateOf<Reader?>(null)

    fun addToBasket(book: Book) {
        if (!booksInBasket.contains(book.id)) {
            booksInBasket.add(book.id)
        }
    }
}