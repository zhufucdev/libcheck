package ui

import androidx.compose.ui.unit.DpSize

enum class WindowSize {
    COMPAT, MEDIUM, WIDE
}

fun calculateWindowSize(windowSize: DpSize) =
    when (windowSize.width.value.toInt()) {
        in 0 until 300 -> WindowSize.COMPAT
        in 300 until 1000 -> WindowSize.MEDIUM
        else -> WindowSize.WIDE
    }
