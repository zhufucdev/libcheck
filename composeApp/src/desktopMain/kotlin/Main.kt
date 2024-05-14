import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import model.AppViewModel
import model.LocalMachineLibrary
import model.Route
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.libcheck_header
import ui.app.LibcheckApp
import ui.calculateWindowSize
import ui.rememberDarkModeEnabled
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
    val darkMode = rememberDarkModeEnabled()

    val model = remember {
        AppViewModel(
            library,
            windowSize,
            route
        )
    }

    MaterialTheme(colors = if (darkMode) darkColors() else lightColors()) {
        androidx.compose.material3.MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
            LibcheckApp(model)
        }
    }
}

@OptIn(ExperimentalResourceApi::class)
fun main() = application {
    val state = rememberWindowState(width = 1200.dp, height = 800.dp)
    Window(
        onCloseRequest = ::exitApplication,
        state = state,
        title = stringResource(Res.string.libcheck_header)
    ) {
        App(state)
    }
}
