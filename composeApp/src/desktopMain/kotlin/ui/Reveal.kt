package ui

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import extension.takeIfInstanceOf
import kotlinx.coroutines.delay
import model.*
import kotlin.time.Duration.Companion.seconds

@Composable
fun rememberRevealAnimation(
    model: AppViewModel,
    current: UuidIdentifier,
    surfaceColor: Color = MaterialTheme.colorScheme.surfaceVariant,
): Color {
    val primarySurfaceColor = MaterialTheme.colorScheme.primaryContainer

    var cardColor by remember(surfaceColor) { mutableStateOf(surfaceColor) }
    var revealed by remember { mutableStateOf(false) }
    val target =
        model.navigator.current.parameters.takeIfInstanceOf<NavigationParameters, RevealParameters>()?.identifier

    LaunchedEffect(target) {
        if (target == current && !revealed) {
            val converter = Color.VectorConverter(cardColor.colorSpace)
            animate(converter, cardColor, primarySurfaceColor) { v, _ ->
                cardColor = v
            }
            delay(1.seconds)
            animate(converter, cardColor, surfaceColor) { v, _ ->
                cardColor = v
            }
            revealed = true
        }
    }

    return cardColor
}

@Suppress("FunctionName")
@Composable
fun LaunchReveal(list: List<Identifiable<UuidIdentifier>>, model: AppViewModel, state: Any) {
    val identifier =
        model.navigator.current.parameters.takeIfInstanceOf<NavigationParameters, RevealParameters>()?.identifier
    LaunchedEffect(identifier) {
        val revealId = identifier ?: return@LaunchedEffect
        val idx = list.indexOfFirst { it.id == revealId }
        if (idx > 0) {
            when (state) {
                is LazyGridState -> state.animateScrollToItem(idx)
                is LazyListState -> state.animateScrollToItem(idx)
                else -> throw NotImplementedError("${state::class.simpleName}")
            }
        }
    }
}
