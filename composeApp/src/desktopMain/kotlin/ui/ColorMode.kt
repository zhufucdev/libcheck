package ui

import androidx.compose.runtime.*
import currentOS
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberDarkModeEnabled(): Boolean {
    var enabled by remember { mutableStateOf(currentOS.isDarkModeEnabled()) }
    LaunchedEffect(true) {
        while (true) {
            delay(0.5.seconds)
            if (currentOS.isDarkModeEnabled() != enabled) {
                enabled = !enabled
            }
        }
    }
    return enabled
}