package ui

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import java.time.Instant
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberNow(): Instant {
    var now by remember { mutableStateOf(Instant.now()) }
    LaunchedEffect(true) {
        while (true) {
            delay(0.5.seconds)
            now = Instant.now()
        }
    }
    return now
}

