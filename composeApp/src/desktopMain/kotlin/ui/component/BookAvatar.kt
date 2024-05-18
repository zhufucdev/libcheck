@file:Suppress("FunctionName")

package ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import ui.PaddingLarge
import ui.PaddingMedium

@Composable
fun BookAvatar(uri: String, modifier: Modifier = Modifier) {
    Box(modifier, contentAlignment = Alignment.Center) {
        Box {
            if (uri.isNotBlank()) {
                Box(
                    Modifier
                        .matchParentSize()
                        .offset(x = PaddingMedium)
                        .background(
                            color = Color.LightGray,
                            shape = RoundedCornerShape(topEnd = PaddingLarge * 2, bottomEnd = PaddingLarge * 2)
                        )
                )
                AsyncImage(
                    uri = uri,
                    modifier = Modifier.clip(RoundedCornerShape(topEnd = PaddingLarge, bottomEnd = PaddingLarge))
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Book,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}
