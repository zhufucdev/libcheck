package ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource
import resources.*
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

@Composable
fun ZonedDateTime.relativeTo(other: ZonedDateTime) = when (val days = other.until(this, ChronoUnit.DAYS)) {
    0L -> stringResource(
        Res.string.last_accessed_at_span,
        DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(this)
    )
    in 1 until 30 -> pluralStringResource(
        Res.plurals.last_accessed_days_ago_span,
        days.toInt(),
        days.toInt()
    )
    in 31 until 300 -> (days / 30f).roundToInt().let { months ->
        pluralStringResource(
            Res.plurals.last_accessed_months_ago_span,
            months,
            months
        )
    }
    else -> stringResource(Res.string.last_accessed_long_ago_span)
}