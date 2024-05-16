import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking
import model.AppViewModel
import model.Configurations
import model.LocalMachineConfigurationViewModel
import model.Route
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.libcheck_header
import ui.app.LibcheckApp
import ui.calculateWindowSize
import ui.rememberDarkModeEnabled

@Composable
@Preview
fun App(windowState: WindowState, configurations: Configurations) {
    val windowSize by remember(windowState) {
        derivedStateOf { calculateWindowSize(windowState.size) }
    }
    val library by remember(configurations) {
        derivedStateOf { configurations.dataSource.initialize(configurations) }
    }
    val route = remember {
        mutableStateOf(Route.BOOKS)
    }
    val darkMode = rememberDarkModeEnabled()

    val model = remember {
        AppViewModel(library, route)
    }

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        LibcheckApp(model, windowSize)
    }
}

@OptIn(ExperimentalResourceApi::class)
fun main() = runBlocking {
    val config = LocalMachineConfigurationViewModel(currentPlatform.dataDir)
    awaitApplication {
        val state = rememberWindowState(width = 1200.dp, height = 800.dp)
        Window(
            onCloseRequest = ::exitApplication,
            state = state,
            title = stringResource(Res.string.libcheck_header)
        ) {
            App(state, config)
        }
    }
}
