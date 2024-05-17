package ui.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
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
            tint = MaterialTheme.colorScheme.onSurface.variant
        )
        CompositionLocalProvider(
            LocalTextStyle provides
                    MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface.variant)
        ) {
            title()
        }
        CompositionLocalProvider(
            LocalTextStyle provides
                    MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.onSurface.variant)
        ) {
            description?.invoke()
        }
    }
}