package ui.component

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SimplePager(
    step: Int,
    vararg steps: @Composable () -> Unit,
) {
    val state = rememberPagerState { steps.count() }
    LaunchedEffect(step) {
        state.animateScrollToPage(step)
    }
    HorizontalPager(
        state = state,
        pageContent = { s ->
            steps[s]()
        },
    )
}

