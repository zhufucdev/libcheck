package ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import model.Identifiable
import model.Identifier

@Suppress("FunctionName")
@Composable
fun LaunchReveal(model: List<Identifiable>, identifier: Identifier?, state: Any) {
    LaunchedEffect(identifier) {
        val revealId = identifier ?: return@LaunchedEffect
        val idx = model.indexOfFirst { it.id == revealId }
        if (idx > 0) {
            when (state) {
                is LazyGridState -> state.animateScrollToItem(idx)
                is LazyListState -> state.animateScrollToItem(idx)
                else -> throw NotImplementedError("${state::class.simpleName}")
            }
        }
    }
}