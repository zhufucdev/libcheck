package ui.component

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.library_not_working_para
import resources.tune_the_preferences_to_fix

@Suppress("FunctionName")
@ExperimentalResourceApi
@Composable
fun ConnectionAlertDialog(
    exception: Exception,
    onDismissRequest: () -> Unit = {},
    confirmButton: @Composable () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        icon = { Icon(imageVector = Icons.Default.ErrorOutline, contentDescription = "library not working") },
        title = { Text(stringResource(Res.string.library_not_working_para)) },
        text = { Text(stringResource(Res.string.tune_the_preferences_to_fix, exception.message ?: "null")) },
        confirmButton = confirmButton
    )
}