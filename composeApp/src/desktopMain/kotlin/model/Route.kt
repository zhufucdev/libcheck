@file:OptIn(ExperimentalResourceApi::class)

package model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.LibraryBooks
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Settings
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.StringResource
import resources.*
import java.util.*

enum class Route(val label: StringResource, val icon: ImageVector, val docked: Boolean = true) {
    Books(Res.string.books_para, Icons.AutoMirrored.Filled.LibraryBooks),
    Readers(Res.string.readers_para, Icons.Default.Contacts),
    Borrowing(Res.string.borrowing_para, Icons.Default.Key),
    Preferences(Res.string.preferences_para, Icons.Default.Settings, docked = false),
}

@Stable
class NavigationModel(current: Route = Route.Books) {
    var current: Route by mutableStateOf(current)
        private set
    private val stack = Stack<Route>()

    init {
        stack.push(current)
    }

    fun push(dest: Route) {
        stack.push(dest)
        current = dest
    }

    fun pop() {
        stack.pop()
        current = stack.peek()
    }
}