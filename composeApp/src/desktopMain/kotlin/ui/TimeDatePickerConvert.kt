@file:OptIn(ExperimentalMaterial3Api::class)

package ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState

fun TimePickerState.toTimeString(): String {
    return "%02d:%02d".format(hour, minute)
}