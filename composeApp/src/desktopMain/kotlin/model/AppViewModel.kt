package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect

class AppViewModel(val library: Library, val navigator: NavigationModel) {
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