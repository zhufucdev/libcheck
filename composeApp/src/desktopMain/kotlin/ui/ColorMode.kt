package ui

import androidx.compose.runtime.*
import currentPlatform
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberDarkModeEnabled(): Boolean {
    var enabled by remember { mutableStateOf(currentPlatform.isDarkModeEnabled()) }
    LaunchedEffect(true) {
        while (true) {
            delay(0.5.seconds)
            if (currentPlatform.isDarkModeEnabled() != enabled) {
                enabled = !enabled
            }
        }
    }
    return enabled
}