package model

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.WindowState
import ui.WindowSize
import ui.app.Route
import java.util.UUID

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