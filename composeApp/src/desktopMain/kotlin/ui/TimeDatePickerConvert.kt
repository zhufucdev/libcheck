@file:OptIn(ExperimentalMaterial3Api::class)

package ui

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import java.text.DateFormat
import java.util.*

fun TimePickerState.toTimeString(): String {
    val cal = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, hour)
        set(Calendar.MINUTE, minute)
        set(Calendar.SECOND, 0)
    }
    return DateFormat.getTimeInstance().format(cal.time)
}

fun DatePickerState.toDateString(): String {
    return DateFormat.getDateInstance().format(Date(selectedDateMillis ?: return ""))
}