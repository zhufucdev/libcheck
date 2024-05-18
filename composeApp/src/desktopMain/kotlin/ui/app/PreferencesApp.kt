@file:OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import kotlinx.coroutines.launch
import model.Configurations
import model.DataSource
import model.DataSourceType
import model.NavigationModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.navigate_up_span
import resources.preferences_para
import ui.PaddingLarge
import ui.PaddingMedium
import ui.component.LocalDataSourcePreferences
import ui.component.PreferenceState
import ui.component.RemoteDataSourcePreferences

@Composable
fun PreferencesApp(model: Configurations, navigator: NavigationModel) {
    val topBarState = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.preferences_para)) },
                navigationIcon = {
                    TooltipBox(
                        positionProvider = rememberComponentRectPositionProvider(),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip { Text(stringResource(Res.string.navigate_up_span)) }
                        }
                    ) {
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    model.save()
                                    navigator.pop()
                                }
                            }
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "navigation up"
                            )
                        }
                    }
                },
                scrollBehavior = topBarState
            )
        }
    ) {
        Box(Modifier.padding(it)) {
            DataSourcePreferences(model, modifier = Modifier.nestedScroll(topBarState.nestedScrollConnection))
        }
    }
}

@Composable
fun DataSourcePreferences(config: Configurations, modifier: Modifier = Modifier) {
    val sources by remember(config) { derivedStateOf { config.sources.entries.sortedBy { it.key } } }
    LazyColumn(modifier) {
        items(sources.size, { sources[it].key }) { index ->
            val type = sources[index].key
            val source = sources[index].value
            val state = remember { PreferenceState(loading = true) }
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
                        Spacer(Modifier.weight(1f))

                        if (state.loading) {
                            CircularProgressIndicator(Modifier.size(24.dp).padding(end = PaddingMedium))
                        }
                    }
                }
                Spacer(Modifier.height(PaddingMedium))

                val enabled by remember(config) { derivedStateOf { config.currentSourceType == type } }
                when (type) {
                    DataSourceType.Local ->
                        LocalDataSourcePreferences(
                            enabled = enabled,
                            state = state,
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
                            state = state,
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
