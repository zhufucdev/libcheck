package model

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.DurationUnit

class AppViewModel(
    val library: Library,
    val navigator: NavigationModel,
    val configurations: Configurations,
) {
    val booksInBasket = mutableStateListOf<Identifier>()
    var basketFabBounds by mutableStateOf(Rect.Zero)
    var draggingIn by mutableStateOf(false)
    var outDraggingBounds by mutableStateOf(Rect.Zero)
    var outDraggingIntersection by mutableStateOf(Rect.Zero)
    var outDraggingTarget by mutableStateOf<Reader?>(null)

    fun addToBasket(book: Book) {
        if (!booksInBasket.contains(book.id)) {
            booksInBasket.add(book.id)
        }
    }

    fun getDueTime(reader: Reader): Instant =
        Instant.now().plus(
            configurations
                .let {
                    if (it.useUnifiedTierModel)
                        it.unifiedCreditTransformer
                    else
                        it.tiers[reader.tier]?.transformer
                }
                ?.transform(reader.creditability + (configurations.tiers[reader.tier]?.baseCredit ?: 0f))
                ?.let { transformed ->
                    Duration.of(transformed.days.toLong(DurationUnit.SECONDS), ChronoUnit.SECONDS)
                }
                ?: Duration.ofDays(1)
        )
}