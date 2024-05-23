@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class)

package ui.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.VectorConverter
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowRightAlt
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import model.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import org.jetbrains.skia.Path
import org.jetbrains.skia.PathDirection
import resources.*
import ui.PaddingLarge
import ui.PaddingSmall
import ui.toDateString
import ui.toTimeString
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoField
import java.time.temporal.ChronoUnit
import java.util.*
import kotlin.math.pow
import kotlin.math.sqrt

@Composable
fun Basket(model: AppViewModel, component: BorrowCapability) {
    val coroutine = rememberCoroutineScope()
    var basketExpanded by remember { mutableStateOf(false) }
    var revealPercentage by remember { mutableStateOf(0f) }
    val secondaryColor = FloatingActionButtonDefaults.containerColor
    val primaryColor = MaterialTheme.colorScheme.primary

    var fabColor by remember(secondaryColor) { mutableStateOf(secondaryColor) }
    var borrower by remember { mutableStateOf<Reader?>(null) }
    var droppedBooks by remember { mutableStateOf<List<Book>>(emptyList()) }
    var sustain by remember { mutableStateOf(false) }

    var batchContent by remember { mutableStateOf<List<Book>>(emptyList()) }
    var batchOffset by remember { mutableStateOf(Offset.Zero) }
    var batchBounds by remember { mutableStateOf(Rect.Zero) }

    var fabScale by remember { mutableFloatStateOf(1f) }
    var batchScale by remember { mutableStateOf(1f) }

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
            onClick = { basketExpanded = true },
            content = { Icon(Icons.Default.ShoppingBasket, "") },
            containerColor = fabColor,
            modifier = Modifier.onGloballyPositioned {
                model.basketFabBounds = it.boundsInRoot()
            }
                .scale(fabScale)
                .pointerInput(Unit) {
                    var cursorInit = Offset.Zero
                    // whole basket d&d
                    detectDragGesturesAfterLongPress(
                        onDragStart = {
                            cursorInit = it
                            batchOffset = Offset.Zero
                            batchContent =
                                model.booksInBasket.mapNotNull { id ->
                                    model.library.getBook(id)?.takeIf { with(model.library) { it.getStock() > 0u } }
                                }
                        },
                        onDrag = { _, amount ->
                            batchOffset += amount
                            model.outDraggingBounds = Rect(batchBounds.topLeft + cursorInit, Size(1f, 1f))
                        },
                        onDragEnd = {
                            droppedBooks = batchContent.toList()
                            borrower = model.outDraggingTarget
                            batchContent = emptyList()
                            model.outDraggingTarget = null
                            model.outDraggingBounds = Rect.Zero
                        },
                        onDragCancel = {
                            batchContent = emptyList()
                            model.outDraggingTarget = null
                            model.outDraggingBounds = Rect.Zero
                        }
                    )
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val pointer = awaitFirstDown(requireUnconsumed = false)
                        val scaleUp = coroutine.launch {
                            animate(
                                fabScale,
                                1.2f,
                                animationSpec = spring(stiffness = Spring.StiffnessVeryLow)
                            ) { v, _ ->
                                fabScale = v
                            }
                        }
                        val longPressed = awaitLongPressOrCancellation(pointer.id)
                        if (longPressed == null || model.booksInBasket.isEmpty()) {
                            basketExpanded = true
                        }
                        scaleUp.cancel()
                        coroutine.launch {
                            coroutineScope {
                                launch {
                                    animate(fabScale, 1f) { v, _ ->
                                        fabScale = v
                                    }
                                }
                                launch {
                                    animate(0.8f, 1f) { v, _ ->
                                        batchScale = v
                                    }
                                }
                            }
                        }
                    }
                }
        )

        if (revealPercentage > 0 || sustain) {
            Popup(
                alignment = Alignment.BottomCenter,
                onDismissRequest = { basketExpanded = false },
            ) {
                Card(
                    modifier = Modifier.size(400.dp).circularReveal(revealPercentage),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    elevation = CardDefaults.cardElevation(12.dp)
                ) {
                    if (!model.booksInBasket.isEmpty()) {
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
                                                    droppedBooks = listOf(book)
                                                }
                                            )
                                        } else {
                                            StagedBookAvatar(
                                                book = Book(
                                                    stringResource(Res.string.unknown_book_para),
                                                    "",
                                                    "",
                                                    UuidIdentifier(),
                                                    "",
                                                    0u
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Row(
                                horizontalArrangement = Arrangement.End,
                                modifier = Modifier.padding(6.dp).fillMaxWidth()
                            ) {
                                TextButton(
                                    onClick = {
                                        model.booksInBasket.clear()
                                    },
                                    content = {
                                        Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "")
                                        Spacer(modifier = Modifier.width(2.dp))
                                        Text(stringResource(Res.string.clear_basket_para))
                                    }
                                )
                            }
                        }
                    } else {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(36.dp).fillMaxSize()) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingBasket,
                                    contentDescription = "",
                                    modifier = Modifier.size(36.dp)
                                )
                                Text(stringResource(Res.string.drag_here_to_stage_para), textAlign = TextAlign.Center)
                            }
                        }
                    }
                }
            }
        }

        if (batchContent.isNotEmpty()) {
            Popup(alignment = Alignment.Center) {
                val density = LocalDensity.current
                BookBatchAvatar(
                    books = batchContent,
                    modifier = with(density) {
                        Modifier
                            .offset(batchOffset.x.toDp(), batchOffset.y.toDp())
                            .scale(batchScale)
                            .onGloballyPositioned { batchBounds = it.boundsInRoot() }
                    }
                )
            }
        }
    }

    borrower?.takeIf { droppedBooks.isNotEmpty() }?.let {
        val dueTime = remember { model.getDueTime(it) }
        BorrowDialog(
            onDismissRequest = { droppedBooks = emptyList() },
            borrowingOut = droppedBooks,
            borrower = it,
            dueTime = dueTime,
            onBorrow = { due ->
                coroutine.launch {
                    if (droppedBooks.size == 1) {
                        component.addBorrow(it, droppedBooks[0], due)
                    } else if (droppedBooks.size > 1) {
                        component.addBorrowBatch(it, droppedBooks, due)
                    }
                    droppedBooks = emptyList()
                }
            }
        )
    }
}

private enum class Editor {
    Time, Date
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BorrowDialog(
    onDismissRequest: () -> Unit,
    borrowingOut: List<Book>,
    borrower: Reader,
    dueTime: Instant,
    onBorrow: (Instant) -> Unit,
) {
    var editor by remember { mutableStateOf<Editor?>(null) }
    val timePickerState =
        dueTime.atZone(ZoneId.systemDefault()).let {
            rememberTimePickerState(
                it.get(ChronoField.HOUR_OF_DAY),
                it.get(ChronoField.MINUTE_OF_HOUR)
            )
        }
    val datePickerState = rememberDatePickerState(dueTime.toEpochMilli())

    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = {
            Icon(
                imageVector = Icons.Default.AddCard,
                contentDescription = "",
                modifier = Modifier.size(24.dp)
            )
        },
        title = {
            Text(
                text = pluralStringResource(Res.plurals.lending_books_para, borrowingOut.size),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                Spacer(Modifier.height(PaddingLarge))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BookBatchAvatar(borrowingOut)
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowRightAlt,
                        contentDescription = "",
                        modifier = Modifier.size(48.dp).padding(horizontal = PaddingLarge)
                    )
                    LazyAvatar(
                        uri = borrower.avatarUri,
                        defaultImageVector = Icons.Default.Person,
                        modifier = Modifier.size(120.dp),
                    )
                }
                Spacer(Modifier.height(PaddingLarge))
                Field(
                    icon = { Icon(imageVector = Icons.Default.Timer, contentDescription = "") },
                    onEditClick = { editor = Editor.Time }
                ) {
                    Text(timePickerState.toTimeString())
                }

                AnimatedVisibility(
                    visible = editor == Editor.Time,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    TimePicker(timePickerState)
                }

                Field(
                    icon = { Icon(imageVector = Icons.Default.DateRange, contentDescription = "") },
                    onEditClick = { editor = Editor.Date }
                ) {
                    Text(datePickerState.toDateString())
                }
                AnimatedVisibility(
                    visible = editor == Editor.Date,
                    enter = expandVertically(),
                    exit = shrinkVertically()
                ) {
                    DatePicker(datePickerState)
                }
            }
        },
        confirmButton = {
            TextButton(
                content = { Text(stringResource(Res.string.ok_caption)) },
                onClick = {
                    onBorrow(
                        Instant.ofEpochMilli(datePickerState.selectedDateMillis!!)
                            .plus(timePickerState.hour.toLong(), ChronoUnit.HOURS)
                            .plus(timePickerState.minute.toLong(), ChronoUnit.MINUTES)
                            .plus(-TimeZone.getDefault().rawOffset.toLong(), ChronoUnit.MILLIS)
                    )
                },
            )
        }
    )
}

@Composable
private fun BookBatchAvatar(books: List<Book>, modifier: Modifier = Modifier) {
    if (books.size == 1) {
        BookAvatar(uri = books[0].avatarUri, modifier = Modifier.size(120.dp).then(modifier))
    } else if (books.size > 1) {
        Box(modifier) {
            Box(
                Modifier.graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithContent {
                        drawContent()
                        drawCircle(
                            brush = Brush.radialGradient(
                                colorStops = arrayOf(
                                    0.6f to Color.White,
                                    1f to Color.Transparent
                                ),
                                center = center,
                            ),
                            blendMode = BlendMode.DstIn,
                            radius = sqrt((size.width / 1.5f).pow(2f) + (size.height / 1.5f).pow(2f))
                        )
                    }
            ) {
                books.slice(1 until minOf(books.size, 3)).forEachIndexed { index, book ->
                    BookAvatar(
                        book.avatarUri,
                        modifier = Modifier.size(120.dp)
                            .alpha(0.6f / index)
                    )
                }
            }
            BookAvatar(books.first().avatarUri, modifier = Modifier.size(120.dp).alpha(0.5f))
            Text(
                text = books.size.toString(),
                style = MaterialTheme.typography.bodySmall.copy(color = MaterialTheme.colorScheme.onPrimary),
                textAlign = TextAlign.Center,
                modifier = Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                    .sizeIn(minWidth = 32.dp)
                    .padding(10.dp)
                    .align(Alignment.BottomEnd)
            )
        }
    }
}

@Composable
private fun Field(
    icon: @Composable () -> Unit,
    onEditClick: () -> Unit,
    content: @Composable () -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        icon()
        Spacer(Modifier.width(PaddingSmall))
        CompositionLocalProvider(androidx.compose.material.LocalTextStyle provides MaterialTheme.typography.bodyMedium) {
            content()
        }
        Spacer(Modifier.weight(1f))
        IconButton(onClick = onEditClick) {
            Icon(imageVector = Icons.Default.Edit, contentDescription = "")
        }
    }
}

@Composable
private fun StagedBookItem(
    book: Book,
    model: AppViewModel,
    onDragStart: () -> Unit,
    onDragStop: () -> Unit,
    onBorrow: (Reader) -> Unit,
) {
    val density = LocalDensity.current
    var dragging by remember { mutableStateOf(false) }
    var dragOff by remember { mutableStateOf(Offset.Zero) }
    var bookBounds by remember { mutableStateOf(Rect.Zero) }
    val outOfStock by remember(book) { derivedStateOf { with(model.library) { book.getStock() } <= 0u } }
    Box(
        modifier = Modifier.padding(6.dp).pointerInput(outOfStock) {
            if (outOfStock) {
                return@pointerInput
            }
            var dragBounds = Rect.Zero
            detectDragGestures(
                onDragStart = {
                    dragging = true
                    onDragStart()
                    dragBounds = Rect(it + bookBounds.topLeft, Size(1f, 1f))
                },
                onDrag = { _, o ->
                    dragOff += o
                    model.outDraggingBounds = dragBounds.translate(dragOff)
                },
                onDragEnd = {
                    model.outDraggingTarget?.let { onBorrow(it) }
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
            Modifier.onGloballyPositioned { bookBounds = it.boundsInRoot() }
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
            BookAvatar(book.avatarUri, modifier = Modifier.size(80.dp))
            Text(text = book.name, style = MaterialTheme.typography.labelMedium, textAlign = TextAlign.Center)
        }
        if (outOfStock) {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface,
            ) {
                Text(
                    text = stringResource(Res.string.out_of_stock_header),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(6.dp)
                )
            }
        }
    }
}

fun Modifier.circularReveal(progress: Float, offset: Offset? = null) = clip(CircularRevealShape(progress, offset))

private class CircularRevealShape(
    private val progress: Float,
    private val offset: Offset? = null,
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