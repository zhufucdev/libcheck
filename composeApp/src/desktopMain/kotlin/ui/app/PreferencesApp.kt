@file:OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import model.Configurations
import model.NavigationModel
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.Res
import resources.navigate_up_span
import resources.preferences_para
import ui.component.DataSourcePreferences

@Composable
fun PreferencesApp(model: Configurations, navigator: NavigationModel) {
    val topBarState = TopAppBarDefaults.enterAlwaysScrollBehavior()
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
                        IconButton(onClick = { navigator.pop() }) {
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