package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun Separator() {
    Box(modifier = Modifier.fillMaxWidth()
        .height(1.dp)
        .padding(horizontal = 2.dp)
        .background(MaterialTheme.colors.onSurface.copy(alpha = 0.4f)))
}