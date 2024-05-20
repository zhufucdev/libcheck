@file:OptIn(ExperimentalResourceApi::class, ExperimentalMaterial3Api::class)

package ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.sqlmaster.proto.LibraryOuterClass.ReaderTier
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import model.*
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.PaddingLarge
import ui.PaddingMedium
import ui.PaddingSmall
import ui.component.LocalDataSourcePreferences
import ui.component.NavigateUpButton
import ui.component.PreferenceState
import ui.component.RemoteDataSourcePreferences
import ui.stringRes
import kotlin.time.Duration.Companion.seconds

@Composable
fun PreferencesApp(model: Configurations, navigator: NavigationModel) {
    val topBarState = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val coroutineScope = rememberCoroutineScope()
    Scaffold(
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(Res.string.preferences_para)) },
                navigationIcon = {
                    NavigateUpButton {
                        coroutineScope.launch {
                            model.save()
                            navigator.pop()
                        }
                    }
                },
                scrollBehavior = topBarState
            )
        }
    ) {
        Column(
            Modifier.padding(it)
                .nestedScroll(topBarState.nestedScrollConnection)
                .verticalScroll(rememberScrollState())
        ) {
            Caption(stringResource(Res.string.data_source_para))
            DataSourcePreferences(model)
            Caption(stringResource(Res.string.reader_tiers))
            UserTiers(model)
        }
    }
}

@Composable
fun DataSourcePreferences(config: Configurations, modifier: Modifier = Modifier) {
    val sources by remember(config) { derivedStateOf { config.sources.entries.sortedBy { it.key } } }
    Column(modifier = modifier) {
        sources.forEachIndexed { index, (type, source) ->
            val state = remember { PreferenceState(loading = true) }
            Column {
                Surface(
                    onClick = { config.currentSourceType = type },
                    modifier = Modifier.fillMaxWidth(),
                    color = Color.Transparent
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = PaddingMedium)
                    ) {
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
            if (index != sources.lastIndex) {
                Spacer(Modifier.height(PaddingLarge))
            }
        }
    }
}

@Composable
private fun UserTiers(model: Configurations) {
    var uniTransform by remember { mutableStateOf(model.unifiedCreditTransformer.expr) }

    DisposableEffect(Unit) {
        onDispose {
            model.unifiedCreditTransformer = CreditTransformer(uniTransform)
        }
    }

    LaunchedEffect(uniTransform) {
        val captured = uniTransform
        delay(1.seconds)
        if (captured == uniTransform) {
            model.unifiedCreditTransformer = CreditTransformer(captured)
        }
    }

    Column(Modifier.padding(start = PaddingLarge + PaddingMedium, top = PaddingMedium, end = PaddingLarge)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(Res.string.use_same_creditability_transformation_for_each_tier_para),
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.weight(1f)
            )
            Switch(
                checked = model.useUnifiedTierModel,
                onCheckedChange = { model.useUnifiedTierModel = it }
            )
        }
        OutlinedTextField(
            value = uniTransform,
            onValueChange = { uniTransform = it },
            enabled = model.useUnifiedTierModel,
            label = { Text(stringResource(Res.string.transformation_para)) },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(PaddingLarge))
        ReaderTier.entries.slice(1 until ReaderTier.UNRECOGNIZED.ordinal).forEach { tier ->
            Row {
                Text(
                    text = tier.stringRes(),
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.End,
                    modifier = Modifier.width(100.dp).padding(top = PaddingLarge)
                )
                Spacer(Modifier.width(PaddingLarge))
                UserTierPreference(
                    model = model.tiers[tier]!!,
                    unified = model.useUnifiedTierModel,
                    onValueChanged = {
                        model.tiers[tier] = it
                    },
                    modifier = Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(PaddingSmall))
        }
    }
}

@Composable
private fun UserTierPreference(
    model: TierModel,
    unified: Boolean,
    onValueChanged: (TierModel) -> Unit,
    modifier: Modifier = Modifier,
) {
    var baseCredit by remember { mutableStateOf(model.baseCredit.toString()) }
    var creditParsed by remember { mutableFloatStateOf(model.baseCredit) }
    var expr by remember { mutableStateOf(model.transformer.expr) }

    DisposableEffect(true) {
        onDispose {
            if (creditParsed < 0f) {
                return@onDispose
            }
            onValueChanged(
                TierModel(creditParsed, CreditTransformer(expr))
            )
        }
    }

    LaunchedEffect(creditParsed, expr) {
        if (creditParsed < 0) {
            return@LaunchedEffect
        }
        val credit = creditParsed
        val expression = expr
        delay(1.seconds)
        if (credit == credit && expression == expr) {
            onValueChanged(TierModel(creditParsed, CreditTransformer(expr)))
        }
    }

    Column(modifier) {
        OutlinedTextField(
            value = baseCredit,
            onValueChange = {
                baseCredit = it
                creditParsed = it.toFloatOrNull() ?: -1f
            },
            isError = creditParsed < 0,
            label = { Text(stringResource(Res.string.base_creditability_para)) },
            modifier = Modifier.fillMaxWidth()
        )
        AnimatedVisibility(!unified, enter = expandVertically(), exit = shrinkVertically()) {
            OutlinedTextField(
                value = expr,
                onValueChange = { expr = it },
                label = { Text(stringResource(Res.string.creditability_transformation_para)) },
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun Caption(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(horizontal = PaddingLarge + PaddingMedium)
            .padding(top = PaddingLarge)
            .then(modifier)
    )
}
