@file:OptIn(ExperimentalResourceApi::class)
@file:Suppress("FunctionName")

package ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import model.Configurations
import model.DataSource
import model.DataSourceType
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.next_para
import resources.waving_hands
import resources.welcome_to_libcheck_para
import ui.PaddingLarge
import ui.PaddingMedium
import ui.WindowSize
import ui.component.LocalDataSourcePreferences
import ui.component.PreferenceState
import ui.component.RemoteDataSourcePreferences

@Composable
fun SetUpApp(windowSize: WindowSize, configurations: Configurations) {
    Scaffold {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.fillMaxSize().padding(it)
        ) {
            when (windowSize) {
                WindowSize.WIDE ->
                    Card(Modifier.fillMaxHeight().width(800.dp).padding(PaddingLarge)) {
                        Column(Modifier.padding(horizontal = PaddingLarge * 4)) {
                            Content(configurations)
                        }
                    }

                else ->
                    Column(Modifier.padding(horizontal = PaddingLarge)) {
                        Content(configurations)
                    }
            }
        }
    }
}

@Composable
private fun ColumnScope.Content(configurations: Configurations) {
    val coroutine = rememberCoroutineScope()

    Spacer(Modifier.height(PaddingLarge * 4))
    WelcomeHeader()
    Spacer(Modifier.height(PaddingLarge * 2))

    val sources by remember(configurations) { derivedStateOf { configurations.sources.entries.sortedBy { it.key } } }
    val state = remember { PreferenceState() }
    LazyColumn {
        items(sources.size, { sources[it].key }) { index ->
            val type = sources[index].key
            val selected = type == configurations.currentSourceType
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
                        Icon(Icons.Default.Check, "selected")
                    }
                }
            }
            AnimatedVisibility(selected, enter = expandVertically(), exit = shrinkVertically()) {
                Column(Modifier.padding(horizontal = PaddingLarge)) {
                    Text(text = stringResource(type.descriptionStrRes), style = MaterialTheme.typography.labelMedium)
                    Spacer(Modifier.height(PaddingMedium))
                    when (type) {
                        DataSourceType.Local -> LocalSource(configurations, state)
                        DataSourceType.Remote -> RemoteSource(configurations, state)
                    }
                }
            }
            Spacer(Modifier.height(PaddingLarge))
        }
    }

    Spacer(Modifier.weight(1f))
    Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        TextButton(
            onClick = {
                configurations.firstLaunch = false
                coroutine.launch {
                    configurations.save()
                }
            },
        ) {
            Text(stringResource(Res.string.next_para))
        }
    }
    Spacer(Modifier.height(PaddingLarge * 2))
}

@Composable
private fun LocalSource(context: Configurations, state: PreferenceState) {
    LocalDataSourcePreferences(
        enabled = true,
        state = state,
        source = context.sources[DataSourceType.Local]!! as DataSource.Local,
        onValueChanged = { context.sources[DataSourceType.Local] = it },
        context = context,
    )
}

@Composable
private fun RemoteSource(context: Configurations, state: PreferenceState) {
    RemoteDataSourcePreferences(
        enabled = true,
        state = state,
        source = context.sources[DataSourceType.Remote]!! as DataSource.Remote,
        onValueChanged = { context.sources[DataSourceType.Remote] = it }
    )
}

@Composable
private fun WelcomeHeader() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
        modifier = Modifier.fillMaxWidth()
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