package ui.app

import androidx.compose.material.ExtendedFloatingActionButton
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshots.SnapshotStateList
import model.Library
import model.Reader

@Composable
fun ReadersApp(library: Library) {
    Scaffold(
        floatingActionButton = {
            ExtendedFloatingActionButton(
                text = { Text("New Reader") },
                icon = { Icon(imageVector = Icons.Default.PersonAdd, contentDescription = "") },
                onClick = { }
            )
        }
    ) {

    }
}