import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.*
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.awaitApplication
import androidx.compose.ui.window.rememberWindowState
import kotlinx.coroutines.runBlocking
import model.*
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.libcheck_header
import ui.app.LibcheckApp
import ui.app.PreferencesApp
import ui.app.SetUpApp
import ui.calculateWindowSize
import ui.rememberSystemDarkMode

@Suppress("FunctionName")
@Composable
@Preview
fun App(windowState: WindowState, configurations: Configurations) {
    val windowSize by remember(windowState) {
        derivedStateOf { calculateWindowSize(windowState.size) }
    }
    val navigator = remember { NavigationModel() }

    val systemInDarkMode = rememberSystemDarkMode()
    val darkMode by remember(configurations, systemInDarkMode) {
        derivedStateOf {
            when (configurations.colorMode) {
                ColorMode.System -> systemInDarkMode
                ColorMode.Dark -> true
                ColorMode.Light -> false
            }
        }
    }

    MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
        Surface {
            if (configurations.firstLaunch) {
                val model = remember { SetUpAppModel(configurations) }
                SetUpApp(windowSize, model)
            }
            AnimatedVisibility(
                visible = navigator.current.type == RouteType.Preferences,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                PreferencesApp(configurations, navigator)
            }
            AnimatedVisibility(
                visible = !configurations.firstLaunch && navigator.current.type.docked,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                val library by remember(configurations) {
                    derivedStateOf {
                        configurations
                            .sources[configurations.currentSourceType]!!
                            .initialize(configurations.dataSourceContext)
                    }
                }
                DisposableEffect(library) {
                    onDispose {
                        library.close()
                    }
                }
                val model = remember(library, navigator, configurations) {
                    AppViewModel(library, navigator, configurations)
                }

                LibcheckApp(model, windowSize)
            }
        }
    }
}

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
    config.save()
}
