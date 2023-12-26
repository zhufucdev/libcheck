package ui.app

import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkAdd
import androidx.compose.runtime.Composable
import model.AppViewModel
import model.Library

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

    }
}