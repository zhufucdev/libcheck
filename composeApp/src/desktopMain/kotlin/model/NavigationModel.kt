@file:OptIn(ExperimentalResourceApi::class)

package model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import resources.*

enum class RouteType(val label: StringResource, val icon: ImageVector, val docked: Boolean = true) {
    Books(Res.string.books_para, Icons.Default.Book),
    Readers(Res.string.readers_para, Icons.Default.People),
    Borrowing(Res.string.borrowing_para, Icons.Default.Key),
    Preferences(Res.string.preferences_para, Icons.Default.Settings, docked = false),
}

interface NavigationParameters

data class Route(val type: RouteType, val parameters: NavigationParameters)

@Stable
open class RevealParameters(val identifier: Identifier) : NavigationParameters

sealed class FilterParameters<T> : NavigationParameters {
    abstract fun T.isCandidate(): Boolean
}

@Stable
open class FilterBorrowParameters(
    private val books: List<Identifier> = emptyList(),
    private val readers: List<Identifier> = emptyList(),
) : FilterParameters<Borrow>() {
    override fun Borrow.isCandidate(): Boolean =
        (books.isEmpty() || books.contains(bookId))
                && (readers.isEmpty() || readers.contains(readerId))
}

data object EmptyParameters : NavigationParameters

@Stable
class NavigationModel(current: Route = Route(RouteType.Books, EmptyParameters)) {
    var current: Route by mutableStateOf(current)
        private set
    var stackSize by mutableStateOf(0)
        private set
    val canGoBack by derivedStateOf { stackSize > 1 }
    private val stack = java.util.Stack<Route>()

    init {
        stack.push(current)
    }

    fun push(dest: RouteType, parameters: NavigationParameters = EmptyParameters) {
        val route = Route(dest, parameters)
        stack.push(route)
        current = route
        stackSize = stack.size
    }

    fun replace(dest: RouteType, parameters: NavigationParameters = EmptyParameters) {
        val route = Route(dest, parameters)
        stack[stack.size - 1] = route
        current = route
    }

    fun pop() {
        stack.pop()
        current = stack.peek()
        stackSize = stack.size
    }
}