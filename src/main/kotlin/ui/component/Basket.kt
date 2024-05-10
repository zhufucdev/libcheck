package ui.component

import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.animate
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.material.Icon
import androidx.compose.material.LocalContentColor
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asComposePath
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.launch
import model.AppViewModel
import model.Book
import model.Identifier
import model.Reader
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathDirection
import ui.PaddingLarge
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.*

@Composable
fun Basket(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()
    var basketExpanded by remember { mutableStateOf(false) }
    var revealPercentage by remember { mutableStateOf(0f) }
    val primaryColor = MaterialTheme.colors.primary
    val secondaryColor = MaterialTheme.colors.secondary
    var fabColor by remember { mutableStateOf(secondaryColor) }
    var borrowing by remember { mutableStateOf(false) }
    var borrower by remember { mutableStateOf<Reader?>(null) }
    var borrowingOut by remember { mutableStateOf<Book?>(null) }
    var sustain by remember { mutableStateOf(false) }

    LaunchedEffect(basketExpanded) {
        if (basketExpanded) {
            animate(revealPercentage, 1f) { v, _ ->
                revealPercentage = v
            }
        } else {
            animate(revealPercentage, 0f) { v, _ ->
                revealPercentage = v
            }
        }
    }

    LaunchedEffect(model.draggingIn) {
        val converter = Color.VectorConverter(primaryColor.colorSpace)
        if (model.draggingIn) {
            animate(converter, fabColor, primaryColor) { v, _ ->
                fabColor = v
            }
        } else {
            animate(converter, fabColor, secondaryColor) { v, _ ->
                fabColor = v
            }
        }
    }

    Box {
        FloatingActionButton(
            onClick = {
                basketExpanded = true
            },
            content = { Icon(Icons.Default.ShoppingBasket, "") },
            backgroundColor = fabColor,
            modifier = Modifier.onGloballyPositioned {
                model.basketFabBounds = it.boundsInRoot()
            }
        )

        if (revealPercentage > 0 || sustain) {
            Popup(
                alignment = Alignment.BottomCenter,
                onDismissRequest = { basketExpanded = false },
            ) {
                Card(
                    modifier = Modifier.size(400.dp).circularReveal(revealPercentage),
                    backgroundColor = MaterialTheme.colors.primarySurface,
                    elevation = 12.dp
                ) {
                    Column {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(3),
                            modifier = Modifier.padding(PaddingLarge).fillMaxWidth()
                        ) {
                            model.booksInBasket.forEach { id ->
                                item(id) {
                                    val book = remember(model.library.books) { model.library.getBook(id) }
                                    if (book != null) {
                                        StagedBookItem(
                                            book,
                                            model,
                                            onDragStart = {
                                                basketExpanded = false
                                                sustain = true
                                            },
                                            onDragStop = {
                                                sustain = false
                                            },
                                            onBorrow = { reader ->
                                                borrower = reader
                                                borrowingOut = book
                                                borrowing = true
                                            }
                                        )
                                    } else {
                                        StagedBookAvatar(
                                            book = Book("Unknown book", "", "", Identifier(), "", 0u)
                                        )
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        if (!model.booksInBasket.isEmpty()) {
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.padding(6.dp).fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        model.booksInBasket.clear()
                                    },
                                    content = {
                                        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colors.onPrimary) {
                                            Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "")
                                            Spacer(modifier = Modifier.width(2.dp))
                                            Text("Clear Basket")
                                        }
                                    }
                                )
                            }
                        }
                    }
                    if (model.booksInBasket.isEmpty()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(36.dp)) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBasket,
                                    contentDescription = "",
                                    modifier = Modifier.size(36.dp)
                                )
                                Text("Drag some books here to stage for borrowing", textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }
    }

    if (borrowing) {
        BorrowDialog(
            onDismissRequest = { borrowing = false },
            borrowingOut = borrowingOut!!,
            borrower = borrower!!,
            onBorrow = {
                coroutine.launch {
                    model.library.addBorrow(borrower!!, borrowingOut!!, it)
                }
                borrowing = false
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BorrowDialog(
    onDismissRequest: () -> Unit,
    borrowingOut: Book,
    borrower: Reader,
    onBorrow: (Instant) -> Unit
) {
    var step by remember { mutableIntStateOf(0) }
    val timePickerState = rememberTimePickerState()
    val datePickerState = rememberDatePickerState(Instant.now().toEpochMilli())

    AlertDialog(
        onDismissRequest = onDismissRequest,
        text = {
            CompositionLocalProvider(LocalContentAlpha provides 1f) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Key, contentDescription = "", modifier = Modifier.size(24.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Borrowing a book", style = MaterialTheme.typography.h5)
                    }
                    Spacer(Modifier.height(PaddingLarge))
                    Row(
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        LazyAvatar(borrowingOut.avatarUri, Icons.Default.Book, Modifier.size(80.dp))
                        Icon(
                            imageVector = Icons.Default.ArrowRightAlt,
                            contentDescription = "",
                            modifier = Modifier.size(48.dp).padding(horizontal = PaddingLarge)
                        )
                        LazyAvatar(borrower.avatarUri, Icons.Default.Person, Modifier.size(80.dp))
                    }
                    Spacer(Modifier.height(PaddingLarge))

                    when (step) {
                        0 -> TimePicker(timePickerState)
                        1 -> DatePicker(datePickerState)
                    }
                }
            }
        },
        buttons = {
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp), horizontalArrangement = Arrangement.End) {
                TextButton(
                    content = { Text("Back") },
                    onClick = { step-- },
                    enabled = step > 0,
                )
                TextButton(
                    content = { Text(if (step < 2) "Next" else "Save") },
                    onClick = {
                        if (step < 2) {
                            step++
                        } else {
                            onBorrow(
                                Instant.ofEpochMilli(datePickerState.selectedDateMillis!!)
                                    .plus(timePickerState.hour.toLong(), ChronoUnit.HOURS)
                                    .plus(timePickerState.minute.toLong(), ChronoUnit.MINUTES)
                                    .plus(-TimeZone.getDefault().rawOffset.toLong(), ChronoUnit.MILLIS)
                            )
                        }
                    },
                )
                Spacer(Modifier.width(6.dp))
            }
        }
    )
}

@Composable
private fun StagedBookItem(
    book: Book,
    model: AppViewModel,
    onDragStart: () -> Unit,
    onDragStop: () -> Unit,
    onBorrow: (Reader) -> Unit
) {
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragOff by remember { mutableStateOf(Offset.Zero) }
    var bounds by remember { mutableStateOf(Rect.Zero) }
    val outOfStock by remember(book) { derivedStateOf { with(model.library) { book.getStock() } <= 0u } }
    Box(
        modifier = Modifier.padding(6.dp).pointerInput(outOfStock) {
            if (outOfStock) {
                return@pointerInput
            }
            detectDragGestures(
                onDragStart = {
                    dragging = true
                    onDragStart()
                },
                onDrag = { _, o ->
                    dragOff += o
                    model.outDraggingBounds = bounds.translate(dragOff)
                },
                onDragEnd = {
                    model.outDraggingTarget?.apply {
                        onBorrow(this)
                    }
                    dragOff = Offset.Zero
                    model.outDraggingBounds = Rect.Zero
                    dragging = false
                    onDragStop()
                },
                onDragCancel = {
                    dragOff = Offset.Zero
                    model.outDraggingBounds = Rect.Zero
                    dragging = false
                    onDragStop()
                }
            )
        }
    ) {
        StagedBookAvatar(
            book,
            outOfStock,
            Modifier.onGloballyPositioned { bounds = it.boundsInRoot() }
        )
        if (dragging) {
            Popup {
                StagedBookAvatar(
                    book = book,
                    modifier = with(density) { Modifier.offset(dragOff.x.toDp(), dragOff.y.toDp()) }
                )
            }
        }
    }
}

@Composable
private fun StagedBookAvatar(book: Book, outOfStock: Boolean = false, modifier: Modifier = Modifier) {
    Box(contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = modifier) {
            LazyAvatar(book.avatarUri, Icons.Default.Book, Modifier.size(80.dp))
            Text(text = book.name, style = MaterialTheme.typography.caption, textAlign = TextAlign.Center)
        }
        if (outOfStock) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colors.surface,
                contentColor = MaterialTheme.colors.onSurface,
            ) {
                Text(
                    "Out of Stock", style = MaterialTheme.typography.body1,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

fun Modifier.circularReveal(progress: Float, offset: Offset? = null) = clip(CircularRevealShape(progress, offset))

private class CircularRevealShape(
    private val progress: Float,
    private val offset: Offset? = null
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(Path().apply {
            addCircle(
                offset?.x ?: (size.width / 2),
                offset?.y ?: size.height,
                size.width.coerceAtLeast(size.height) * 2 * progress,
                PathDirection.CLOCKWISE
            )
        }.asComposePath())
    }
}