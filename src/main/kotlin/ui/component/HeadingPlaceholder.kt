package ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.LocalTextStyle
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ui.PaddingLarge
import ui.variant

@Composable
fun HeadingPlaceholder(imageVector: ImageVector, title: @Composable () -> Unit, description: (@Composable () -> Unit)? = null) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxSize()
    ) {
        Icon(
            imageVector = imageVector,
            contentDescription = "",
            modifier = Modifier.size(64.dp).padding(bottom = PaddingLarge),
            tint = MaterialTheme.colors.onSurface.variant
        )
        CompositionLocalProvider(
            LocalTextStyle provides
                    MaterialTheme.typography.h5.copy(color = MaterialTheme.colors.onSurface.variant)
        ) {
            title()
        }
        CompositionLocalProvider(
            LocalTextStyle provides
                    MaterialTheme.typography.caption.copy(color = MaterialTheme.colors.onSurface.variant)
        ) {
            description?.invoke()
        }
    }
}