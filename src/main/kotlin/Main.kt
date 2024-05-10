import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import model.AppViewModel
import ui.app.LibcheckApp
import model.LocalMachineLibrary
import model.Route
import ui.calculateWindowSize
import java.io.File

@Composable
@Preview
fun App(windowState: WindowState) {
    val windowSize by remember(windowState) {
        derivedStateOf { calculateWindowSize(windowState.size) }
    }
    val library = remember {
        LocalMachineLibrary(File("libcheck").absoluteFile)
    }
    val route = remember {
        mutableStateOf(Route.BOOKS)
    }
    val model by remember(windowState) {
        derivedStateOf {
            AppViewModel(
                library,
                windowState,
                windowSize,
                route
            )
        }
    }

    MaterialTheme {
        LibcheckApp(model)
    }
}

fun main() = application {
    val state = rememberWindowState(width = 1200.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = "Lib Check"
    ) {
        App(state)
    }
}
