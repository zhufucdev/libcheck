@file:OptIn(ExperimentalResourceApi::class)
@file:Suppress("FunctionName")

package ui.app

import androidx.compose.animation.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircleOutline
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import model.Configurations
import model.DataSource
import model.DataSourceType
import model.SetUpAppModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.PaddingLarge
import ui.PaddingMedium
import ui.WindowSize
import ui.component.*

@Composable
fun SetUpApp(windowSize: WindowSize, model: SetUpAppModel) {
    if (windowSize >= WindowSize.WIDE) {
        Image(
            painter = painterResource(Res.drawable.flat_mountains),
            contentScale = ContentScale.Crop,
            contentDescription = "generic background",
            modifier = Modifier.fillMaxSize()
        )
    }
    Scaffold(
        topBar = {
            Popup {
                AnimatedVisibility(model.working, enter = fadeIn(), exit = fadeOut()) {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
        },
        containerColor = Color.Transparent
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(it)
        ) {
            when (windowSize) {
                WindowSize.WIDE ->
                    Card(Modifier.fillMaxHeight().width(800.dp).padding(PaddingLarge)) {
                        SimplePager(
                            step = model.step,
                            {
                                Setup(
                                    model = model,
                                    Modifier.padding(horizontal = PaddingLarge * 4)
                                )
                            },
                            {
                                Ready(
                                    model = model,
                                    modifier = Modifier.padding(horizontal = PaddingLarge * 4)
                                )
                            }
                        )
                    }

                else ->
                    SimplePager(
                        step = model.step,
                        {
                            Setup(
                                model = model,
                                modifier = Modifier.padding(horizontal = PaddingLarge)
                            )
                        },
                        {
                            Ready(
                                model = model,
                                modifier = Modifier.padding(horizontal = PaddingLarge)
                            )
                        }
                    )
            }
        }
    }
}

@Composable
private fun Setup(
    model: SetUpAppModel,
    modifier: Modifier = Modifier,
) {
    val configurations = model.configurations
    val sources by remember(configurations) { derivedStateOf { configurations.sources.entries.sortedBy { it.key } } }
    val states =
        remember { mutableStateMapOf(*(sources.map { (type, _) -> type to PreferenceState(loading = true) }).toTypedArray()) }
    val currentState by remember { derivedStateOf { states[configurations.currentSourceType]!! } }
    val coroutine = rememberCoroutineScope()

    var sourceCtx by remember { mutableStateOf(configurations.dataSourceContext) }
    val library by remember(configurations, sourceCtx) { derivedStateOf {
        configurations
            .sources[configurations.currentSourceType]!!
            .initialize(sourceCtx)
    } }

    DisposableEffect(library) {
        onDispose {
            library.close()
        }
    }

    Scaffold(
        containerColor = Color.Transparent,
        topBar = {
            WelcomeHeader(Modifier.padding(top = PaddingLarge * 4, bottom = PaddingLarge * 2))
        },
        bottomBar = {
            BottomBar(
                enabled = currentState.valid && !currentState.loading && !model.working,
                onPositiveClick = {
                    coroutine.launch {
                        model.working = true
                        withContext(Dispatchers.IO) {
                            try {
                                library.connect()
                            } catch (e: Exception) {
                                model.connectionException = e
                                e.printStackTrace()
                            }
                            configurations.save()
                        }
                        model.working = false
                        if (model.connectionException == null) {
                            model.step = 1
                        }
                    }
                },
                positive = {
                    Text(stringResource(Res.string.next_para))
                }
            )
        },
        modifier = modifier
    ) {
        LazyColumn(Modifier.padding(it)) {
            items(sources.size, { i -> sources[i].key }) { index ->
                val type = sources[index].key
                val selected = type == configurations.currentSourceType
                val state = states[type]!!
                Surface(color = Color.Transparent, onClick = { configurations.currentSourceType = type }) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().padding(PaddingLarge)
                    ) {
                        Icon(imageVector = type.icon, contentDescription = type.name)
                        Spacer(Modifier.width(PaddingLarge))
                        Text(text = stringResource(type.titleStrRes), style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.weight(1f))
                        if (selected) {
                            if (state.loading) {
                                CircularProgressIndicator(Modifier.size(24.dp))
                            } else {
                                Icon(Icons.Default.Check, "selected")
                            }
                        }
                    }
                }
                AnimatedVisibility(selected, enter = expandVertically(), exit = shrinkVertically()) {
                    Column(Modifier.padding(horizontal = PaddingLarge)) {
                        Text(
                            text = stringResource(type.descriptionStrRes),
                            style = MaterialTheme.typography.labelMedium
                        )
                        Spacer(Modifier.height(PaddingMedium))
                        when (type) {
                            DataSourceType.Local ->
                                LocalSource(configurations, !model.working, state) { ctx -> sourceCtx = ctx }

                            DataSourceType.Remote ->
                                RemoteSource(configurations, !model.working, state) { ctx -> sourceCtx = ctx }
                        }
                    }
                }
                Spacer(Modifier.height(PaddingLarge))
            }
        }
    }

    model.connectionException?.let {
        ConnectionAlertDialog(
            exception = it,
            onDismissRequest = { model.connectionException = null },
            confirmButton = {
                TextButton(
                    onClick = { model.connectionException = null }
                ) {
                    Text(stringResource(Res.string.ok_caption))
                }
            }
        )
    }
}

@Composable
private fun Ready(model: SetUpAppModel, modifier: Modifier = Modifier) {
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            BottomBar(
                true,
                positive = {
                    Text(stringResource(Res.string.launch_app_para))
                },
                onPositiveClick = {
                    coroutineScope.launch {
                        model.launchHome()
                    }
                },
                negative = {
                    Text(stringResource(Res.string.back_para))
                },
                onNegativeClick = {
                    model.step = 0
                }
            )
        },
        modifier = modifier
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(it).fillMaxSize()) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = Icons.Default.CheckCircleOutline,
                    contentDescription = "library is ready",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(78.dp).padding(bottom = PaddingLarge)
                )
                Text(
                    text = stringResource(Res.string.you_are_all_set_para),
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}

@Composable
private fun LocalSource(
    model: Configurations,
    enabled: Boolean,
    state: PreferenceState,
    onContextChanged: (DataSource.Context) -> Unit,
) {
    LaunchedEffect(model) {
        onContextChanged(model.dataSourceContext)
    }

    LocalDataSourcePreferences(
        enabled = enabled,
        state = state,
        source = model.sources[DataSourceType.Local]!! as DataSource.Local,
        onValueChanged = { model.sources[DataSourceType.Local] = it },
    )
}

@Composable
private fun RemoteSource(
    model: Configurations,
    enabled: Boolean,
    state: PreferenceState,
    onContextChanged: (DataSource.Context) -> Unit,
) {
    var password by remember { mutableStateOf("") }

    LaunchedEffect(password, model) {
        onContextChanged(object : DataSource.Context.WithPassword, DataSource.Context.WithToken {
            override val password: String = password
            override var token: ByteArray? = null

            override suspend fun save() {
                model.token = token
            }
        })
    }

    PasswordRemoteDataSourcePreferences(
        enabled = enabled,
        source = model.sources[DataSourceType.Remote]!! as DataSource.Remote,
        state = state,
        onValueChanged = { model.sources[DataSourceType.Remote] = it },
        password = password,
        onPasswordChanged = {
            password = it
        }
    )
}

@Composable
private fun WelcomeHeader(modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth().then(modifier)
    ) {
        Image(
            painter = painterResource(Res.drawable.waving_hands),
            contentDescription = "waving hands",
            modifier = Modifier.size(60.dp).padding(end = PaddingLarge)
        )
        Text(
            stringResource(Res.string.welcome_to_libcheck_para),
            style = MaterialTheme.typography.headlineLarge
        )
    }
}

@Composable
private fun BottomBar(
    enabled: Boolean,
    onPositiveClick: () -> Unit,
    onNegativeClick: (() -> Unit)? = null,
    positive: @Composable () -> Unit,
    negative: @Composable (() -> Unit)? = null,
) {
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier.fillMaxWidth().padding(bottom = PaddingLarge * 2)
    ) {
        if (negative != null) {
            TextButton(
                onClick = {
                    onNegativeClick?.invoke()
                },
                enabled = enabled
            ) {
                negative()
            }
        }
        TextButton(
            onClick = {
                onPositiveClick()
            },
            enabled = enabled
        ) {
            positive()
        }
    }
}