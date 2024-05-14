package ui.component

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.delay
import model.AppViewModel
import model.Identifier
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberRevealAnimation(
    model: AppViewModel,
    current: Identifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant
): Color {
    val primarySurfaceColor = MaterialTheme.colorScheme.primaryContainer

    var cardColor by remember { mutableStateOf(surfaceColor) }

    LaunchedEffect(model.reveal) {
        if (model.reveal == current) {
            val converter = Color.VectorConverter(cardColor.colorSpace)
            animate(converter, cardColor, primarySurfaceColor) { v, _ ->
                cardColor = v
            }
            delay(0.5.seconds)
            animate(converter, cardColor, surfaceColor) { v, _ ->
                cardColor = v
            }
            model.reveal = null
        }
    }

    return cardColor
}
