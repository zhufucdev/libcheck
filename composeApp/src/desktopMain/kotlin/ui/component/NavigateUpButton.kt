package ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.navigate_up_span

@Suppress("FunctionName")
@ExperimentalMaterial3Api
@Composable
fun NavigateUpButton(onClick: () -> Unit) {
    TooltipBox(
        positionProvider = rememberComponentRectPositionProvider(),
        state = rememberTooltipState(),
        tooltip = {
            PlainTooltip { Text(stringResource(Res.string.navigate_up_span)) }
        }
    ) {
        IconButton(
            onClick = onClick
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "navigation up"
            )
        }
    }
}