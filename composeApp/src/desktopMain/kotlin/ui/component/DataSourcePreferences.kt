@file:Suppress("FunctionName")
@file:OptIn(ExperimentalResourceApi::class)

package ui.component

import AesCipher
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import kotlinx.coroutines.delay
import model.Configurations
import model.DataSource
import model.DataSourceType
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.PaddingLarge
import ui.PaddingMedium
import kotlin.time.Duration.Companion.seconds

@Composable
fun DataSourcePreferences(config: Configurations, modifier: Modifier = Modifier) {
    val sources by remember(config) { derivedStateOf { config.sources.entries.sortedBy { it.key } } }
    LazyColumn(modifier) {
        items(sources.size, { sources[it].key }) { index ->
            val type = sources[index].key
            val source = sources[index].value
            Column {
                Surface(
                    onClick = { config.currentSourceType = type },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        RadioButton(
                            selected = type == config.currentSourceType,
                            onClick = { config.currentSourceType = type }
                        )

                        Text(stringResource(type.titleStrRes), style = MaterialTheme.typography.titleMedium)
                    }
                }
                Spacer(Modifier.height(PaddingMedium))

                val enabled = config.currentSourceType == type
                when (type) {
                    DataSourceType.Local ->
                        LocalDataSourcePreferences(
                            enabled = enabled,
                            state = remember { PreferenceState() },
                            source = source as DataSource.Local,
                            onValueChanged = {
                                config.sources[type] = it
                            },
                            context = config,
                            modifier = Modifier.padding(start = PaddingLarge * 4, end = PaddingLarge)
                        )

                    DataSourceType.Remote ->
                        RemoteDataSourcePreferences(
                            enabled = enabled,
                            state = remember { PreferenceState() },
                            source = source as DataSource.Remote,
                            onValueChanged = {
                                config.sources[type] = it
                            },
                            modifier = Modifier.padding(start = PaddingLarge * 4, end = PaddingLarge)
                        )
                }
            }
            Spacer(Modifier.height(PaddingLarge))
        }
    }
}

@Stable
class PreferenceState(valid: Boolean = true) {
    var valid: Boolean by mutableStateOf(valid)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LocalDataSourcePreferences(
    enabled: Boolean,
    state: PreferenceState,
    source: DataSource.Local,
    onValueChanged: (DataSource.Local) -> Unit,
    context: Configurations,
    modifier: Modifier = Modifier,
) {
    var rootPath by remember { mutableStateOf(source.rootPath ?: context.defaultRootPath) }
    LaunchedEffect(rootPath) {
        val captured = rootPath
        delay(1.seconds)
        if (rootPath == captured) {
            onValueChanged(source.copy(rootPath = rootPath.takeIf { it.isNotBlank() }))
        }
    }
    LaunchedEffect(rootPath) {
        state.valid = rootPath.isNotBlank()
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
fun RemoteDataSourcePreferences(
    enabled: Boolean,
    source: DataSource.Remote,
    state: PreferenceState,
    onValueChanged: (DataSource.Remote) -> Unit,
    modifier: Modifier = Modifier,
) {
    val cipher = remember { AesCipher() }

    var host by remember { mutableStateOf(source.remoteHost) }
    var port by remember { mutableStateOf(source.remotePort.toString()) }
    var deviceName by remember { mutableStateOf(source.deviceName) }
    var useTls by remember { mutableStateOf(source.useTransportSecurity) }
    var password by remember { mutableStateOf(cipher.decrypt(source.password).decodeToString()) }

    val portValid by remember { derivedStateOf { port.toShortOrNull()?.let { it > 0 } == true } }

    LaunchedEffect(host, port, deviceName, useTls, password) {
        val captured = listOf(host, port, deviceName, useTls, password)
        delay(1.seconds)
        if (!state.valid) {
            return@LaunchedEffect
        }
        val current = listOf(host, port, deviceName, useTls, password)
        if (current == captured) {
            onValueChanged(
                DataSource.Remote(
                    host,
                    port.toInt(),
                    deviceName,
                    useTls,
                    cipher.encrypt(password.encodeToByteArray())
                )
            )
        }
    }
    LaunchedEffect(port, deviceName) {
        state.valid = portValid && deviceName.isNotBlank()
    }

    Column(modifier) {
        OutlinedTextField(
            value = host,
            onValueChange = { host = it },
            label = { Text(stringResource(Res.string.host_para)) },
            enabled = enabled,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaddingMedium))
        OutlinedTextField(
            value = port,
            onValueChange = { port = it },
            label = { Text(stringResource(Res.string.port_para)) },
            enabled = enabled,
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
            enabled = enabled,
            singleLine = true,
            isError = deviceName.isBlank(),
            trailingIcon = { InvalidFieldIndicator(deviceName.isNotBlank()) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaddingMedium))
        var showPassword by remember { mutableStateOf(false) }
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text(stringResource(Res.string.password_para)) },
            enabled = enabled,
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPassword = !showPassword }) {
                    Icon(
                        imageVector = if (showPassword) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "show / hide password"
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(PaddingMedium))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                stringResource(Res.string.use_transport_layer_security_para),
                style = MaterialTheme.typography.bodyLarge.let { if (enabled) it else it.disabled }
            )
            Spacer(Modifier.weight(1f))
            Switch(
                checked = useTls,
                onCheckedChange = { useTls = it },
                enabled = enabled
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