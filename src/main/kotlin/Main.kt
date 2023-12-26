import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import ui.app.LibcheckApp
import model.Library
import java.io.File

@Composable
@Preview
fun App() {
    val library = remember { Library(File("libcheck").absoluteFile) }

    MaterialTheme {
        LibcheckApp(library)
    }
}

fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        state = WindowState(width = 1200.dp, height = 800.dp),
        title = "Lib Check"
    ) {
        App()
    }
}
