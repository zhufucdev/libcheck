@file:Suppress("FunctionName")

package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import ui.PaddingLarge
import ui.PaddingMedium

@Composable
fun BookAvatar(uri: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box {
            Box(
                Modifier
                    .matchParentSize()
                    .offset(x = PaddingMedium)
                    .clip(RoundedCornerShape(topEnd = PaddingLarge * 2, bottomEnd = PaddingLarge * 2))
                    .background(MaterialTheme.colors.onSurface.copy(alpha = 0.3f))
            )
            LazyAvatar(
                uri,
                Icons.Default.Book,
                modifier = Modifier.clip(RoundedCornerShape(topEnd = PaddingLarge, bottomEnd = PaddingLarge))
            )
        }
    }
}
