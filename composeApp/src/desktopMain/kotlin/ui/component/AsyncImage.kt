package ui.component

import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ImageNotSupported
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.io.IOUtils
import java.net.URI

private val cache = mutableMapOf<String, ImageBitmap>()

@Composable
fun AsyncImage(uri: String, contentScale: ContentScale = ContentScale.Fit, modifier: Modifier = Modifier) {
    var image by remember { mutableStateOf<ImageBitmap?>(null) }
    var error by remember { mutableStateOf(false) }

    LaunchedEffect(uri) {
        val cached = cache[uri]
        if (cached == null) {
            withContext(Dispatchers.IO) {
                try {
                    image = null
                    val ba = IOUtils.toByteArray(URI.create(uri))
                    image = org.jetbrains.skia.Image.makeFromEncoded(ba).toComposeImageBitmap()
                    cache[uri] = image!!
                    error = false
                } catch (e: Exception) {
                    error = true
                }
            }
        } else {
            image = cached
        }
    }

    if (error) {
        Icon(
            imageVector = Icons.Default.ImageNotSupported,
            contentDescription = "",
            modifier
        )
    } else {
        image.let {
            if (it == null) {
                Box(modifier.shimmerBackground())
            } else {
                Image(
                    bitmap = it,
                    contentDescription = "",
                    contentScale = contentScale,
                    modifier = modifier
                )
            }
        }
    }
}

fun Modifier.shimmerBackground(shape: Shape = RoundedCornerShape(4.dp)): Modifier = composed {
    val transition = rememberInfiniteTransition()

    val translateAnimation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 400f,
        animationSpec = infiniteRepeatable(
            tween(durationMillis = 1500, easing = LinearOutSlowInEasing),
            RepeatMode.Restart
        ),
    )
    val shimmerColors = listOf(
        Color.LightGray.copy(alpha = 0.9f),
        Color.LightGray.copy(alpha = 0.4f),
    )
    val brush = Brush.linearGradient(
        colors = shimmerColors,
        start = Offset(translateAnimation, translateAnimation),
        end = Offset(translateAnimation + 100f, translateAnimation + 100f),
        tileMode = TileMode.Mirror,
    )
    return@composed this.then(background(brush, shape))
}