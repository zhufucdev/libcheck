@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class)

package ui.component

import AesCipher
import EncryptDecrypt
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import currentPlatform
import kotlinx.coroutines.delay
import model.DataSource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.PaddingMedium
import kotlin.time.Duration.Companion.seconds

@Stable
class PreferenceState(valid: Boolean = true, loading: Boolean = false) {
    var valid: Boolean by mutableStateOf(valid)
    var loading: Boolean by mutableStateOf(loading)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDataSourcePreferences(
    enabled: Boolean,
    state: PreferenceState = remember { PreferenceState() },
    source: DataSource.Local,
    onValueChanged: (DataSource.Local) -> Unit,
    modifier: Modifier = Modifier,
) {
    var rootPath by remember { mutableStateOf(source.rootPath ?: currentPlatform.dataDir.absolutePath) }
    LaunchedEffect(rootPath) {
        val captured = rootPath
        delay(1.seconds)
        if (rootPath == captured) {
            onValueChanged(source.copy(rootPath = rootPath.takeIf { it.isNotBlank() }))
        }
    }
    LaunchedEffect(rootPath) {
        state.valid = rootPath.isNotBlank()
        state.loading = false
    }

    DisposableEffect(true) {
        onDispose {
            onValueChanged(source.copy(rootPath = rootPath.takeIf { it.isNotBlank() }))
        }
    }

    Box(modifier) {
        OutlinedTextField(
            value = rootPath,
            onValueChange = { rootPath = it },
            enabled = enabled,
            label = { Text(stringResource(Res.string.root_path_para)) },
            trailingIcon = {
                TooltipBox(
                    positionProvider = rememberComponentRectPositionProvider(),
                    state = rememberTooltipState(),
                    tooltip = {
                        PlainTooltip {
                            Text(stringResource(Res.string.select_file_para))
                        }
                    },
                    enableUserInput = enabled
                ) {
                    IconButton(
                        onClick = {},
                        enabled = enabled,
                        modifier = Modifier
                    ) {
                        Icon(imageVector = Icons.Default.Folder, contentDescription = "open file chooser")
                    }
                }
            },
            isError = rootPath.isBlank(),
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun PasswordRemoteDataSourcePreferences(
    enabled: Boolean,
    source: DataSource.Remote,
    state: PreferenceState = remember { PreferenceState(loading = false) },
    onValueChanged: (DataSource.Remote) -> Unit,
    password: String,
    onPasswordChanged: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicRemoteDataSourcePreferences(
        enabled = enabled,
        source = source,
        state = state,
        onValueChanged = onValueChanged,
        modifier = modifier
    ) { actuallyEnabled ->
        var showPassword by remember { mutableStateOf(false) }
        OutlinedPasswordTextField(
            value = password,
            onValueChange = onPasswordChanged,
            showPassword = showPassword,
            onShowPasswordChanged = { showPassword = it },
            enabled = actuallyEnabled
        )
        Spacer(Modifier.height(PaddingMedium))
    }
}

@Composable
fun RemoteDataSourcePreferences(
    enabled: Boolean,
    source: DataSource.Remote,
    state: PreferenceState = remember { PreferenceState(loading = false) },
    onValueChanged: (DataSource.Remote) -> Unit,
    modifier: Modifier = Modifier,
) {
    BasicRemoteDataSourcePreferences(enabled, source, state, onValueChanged, modifier) {}
}

@Composable
private fun BasicRemoteDataSourcePreferences(
    enabled: Boolean,
    source: DataSource.Remote,
    state: PreferenceState = remember { PreferenceState(loading = false) },
    onValueChanged: (DataSource.Remote) -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (Boolean) -> Unit,
) {
    var cipher by remember { mutableStateOf<EncryptDecrypt?>(null) }
    LaunchedEffect(true) {
        state.loading = true
        cipher = AesCipher()
        state.loading = false
    }

    val actualEnabled by remember(state, enabled) { derivedStateOf { enabled && !state.loading } }

    var host by remember(source) { mutableStateOf(source.remoteHost) }
    var port by remember(source) { mutableStateOf(source.remotePort.toString()) }
    var deviceName by remember(source) { mutableStateOf(source.deviceName) }
    var useTls by remember(source) { mutableStateOf(source.useTransportSecurity) }

    val portValid by remember { derivedStateOf { port.toShortOrNull()?.let { it > 0 } == true } }

    LaunchedEffect(host, port, deviceName, useTls) {
        if (state.loading) {
            return@LaunchedEffect
        }
        val captured = listOf(host, port, deviceName, useTls)
        delay(1.seconds)
        if (!state.valid) {
            return@LaunchedEffect
        }
        val current = listOf(host, port, deviceName, useTls)
        if (current == captured) {
            onValueChanged(
                DataSource.Remote(
                    host,
                    port.toInt(),
                    deviceName,
                    useTls,
                )
            )
        }
    }
    LaunchedEffect(port, deviceName) {
        state.valid = portValid && deviceName.isNotBlank()
    }

    DisposableEffect(true) {
        onDispose {
            onValueChanged(
                DataSource.Remote(
                    host,
                    port.toInt(),
                    deviceName,
                    useTls,
                )
            )
        }
    }

    Column(modifier) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text(stringResource(Res.string.host_para)) },
            enabled = actualEnabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaddingMedium))
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text(stringResource(Res.string.port_para)) },
            enabled = actualEnabled,
            singleLine = true,
            isError = !portValid,
            trailingIcon = { InvalidFieldIndicator(portValid) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaddingMedium))
        OutlinedTextField(
            value = deviceName,
            onValueChange = { deviceName = it },
            label = { Text(stringResource(Res.string.this_device_name_para)) },
            enabled = actualEnabled,
            singleLine = true,
            isError = deviceName.isBlank(),
            trailingIcon = { InvalidFieldIndicator(deviceName.isNotBlank()) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaddingMedium))
        content(actualEnabled)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(Res.string.use_transport_layer_security_para),
                style = MaterialTheme.typography.bodyLarge.let { if (actualEnabled) it else it.disabled },
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = useTls,
                onCheckedChange = { useTls = it },
                enabled = actualEnabled
            )
        }
    }
}

@Composable
private fun InvalidFieldIndicator(valid: Boolean) {
    AnimatedVisibility(!valid, enter = fadeIn(), exit = fadeOut()) {
        Icon(imageVector = Icons.Default.Error, contentDescription = "invalid port number")
    }
}

private val TextStyle.disabled
    @Composable
    get() = copy(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))