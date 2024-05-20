package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Suppress("FunctionName")
@Composable
fun Separator(modifier: Modifier = Modifier) {
    Box(
        modifier = Modifier.fillMaxWidth()
            .height(1.dp)
            .padding(horizontal = 2.dp)
            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
            .then(modifier)
    )
}