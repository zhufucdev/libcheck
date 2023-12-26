package model

import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.window.WindowState
import ui.WindowSize

class AppViewModel(val library: Library, val windowState: WindowState, val windowSize: WindowSize) {
    val booksInBasket = mutableStateListOf<Book>()
    var basketFabBounds by mutableStateOf(Rect.Zero)
    var draggingIn by mutableStateOf(false)
    var outDraggingBounds by mutableStateOf(Rect.Zero)
    var outDraggingIntersection by mutableStateOf(Rect.Zero)
    var outDraggingTarget by mutableStateOf<Reader?>(null)

    fun addToBasket(book: Book) {
        if (!booksInBasket.contains(book)) {
            booksInBasket.add(book)
        }
    }
}