package model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.StringResource
import resources.*

enum class RouteType(val label: StringResource, val icon: ImageVector, val docked: Boolean = true) {
    Books(Res.string.books_para, Icons.Default.Book),
    Readers(Res.string.readers_para, Icons.Default.People),
    Borrowing(Res.string.borrowing_para, Icons.Default.Key),
    Accounts(Res.string.accounts_para, Icons.Default.AccountCircle),
    Preferences(Res.string.preferences_para, Icons.Default.Settings, docked = false),
}

interface NavigationParameters

data class Route(val type: RouteType, val parameters: NavigationParameters)

@Stable
open class RevealParameters(val identifier: UuidIdentifier) : NavigationParameters

@Stable
open class RevealDetailsParameters(val identifier: UuidIdentifier) : NavigationParameters

sealed class FilterParameters<T> : NavigationParameters {
    abstract fun T.isCandidate(): Boolean
}

@Stable
open class FilterBorrowParameters(
    private val books: List<UuidIdentifier> = emptyList(),
    private val readers: List<UuidIdentifier> = emptyList(),
) : FilterParameters<BorrowLike>() {
    override fun BorrowLike.isCandidate(): Boolean =
        (books.isEmpty() || books.any { hasBook(it) })
                && (readers.isEmpty() || readers.contains(readerId))
}

data class ReconstructParameters(val identifier: UuidIdentifier) : NavigationParameters

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

    fun replace(dest: RouteType = current.type, parameters: NavigationParameters = EmptyParameters) {
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