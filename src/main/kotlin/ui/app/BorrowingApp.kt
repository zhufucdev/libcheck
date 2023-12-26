package ui.app

import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import model.AppViewModel
import model.Library
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.*

@Composable
fun BorrowingApp(model: AppViewModel) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("Borrow") },
                icon = { Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "") },
                onClick = {}
            )
        }
    ) {
        val formatter = remember { SimpleDateFormat() }
        LazyColumn {
            model.library.borrowList.items.forEach {
                item(it.id) {
                    Text(
                        "${model.library.getBook(it.bookId)?.name} to ${model.library.getReader(it.readerId)?.name} due in ${
                            formatter.format(Date(it.dueTime))
                        }"
                    )
                }
            }
        }
    }
}