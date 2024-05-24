@file:Suppress("FunctionName")

package ui.app

import androidx.compose.animation.*
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.awaitLongPressOrCancellation
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.rememberComponentRectPositionProvider
import com.sqlmaster.proto.LibraryOuterClass.Session
import com.sqlmaster.proto.LibraryOuterClass.UserRole
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import library.toEpochMilli
import model.AccountCapability
import model.AppViewModel
import model.ModAccountCapability
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import resources.*
import ui.*
import ui.component.OutlinedPasswordTextField
import ui.component.shimmerBackground
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.seconds

@ExperimentalMaterial3Api
@Composable
fun AccountsApp(model: AppViewModel) {
    val coroutine = rememberCoroutineScope()

    val component = model.library.components.of<AccountCapability>() ?: return
    var openChangePasswordDialog by remember { mutableStateOf(false) }
    var invitationState by remember { mutableStateOf<InvitationState?>(null) }
    var revokeSession by remember { mutableStateOf<Session?>(null) }

    Scaffold(floatingActionButton = {
        val cp = model.library.components.of<ModAccountCapability>() ?: return@Scaffold

        ExtendedFloatingActionButton(
            onClick = { invitationState = InvitationState(cp) },
            icon = {
                Icon(imageVector = Icons.Default.Add, contentDescription = "invite new user")
            },
            text = {
                Text(stringResource(Res.string.invite_user_para))
            }
        )
    }) { it ->
        Column(Modifier.verticalScroll(rememberScrollState()).padding(it).padding(PaddingLarge)) {
            AccountCard(
                component = component,
                model = model,
                onChangePasswordRequested = { openChangePasswordDialog = true }
            )
            Spacer(Modifier.height(PaddingLarge * 2))
            Text(stringResource(Res.string.my_devices_para), style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(start = PaddingMedium))
            Spacer(Modifier.height(PaddingLarge))
            SessionsCards(
                component = component,
                revoke = revokeSession,
                onRevokeRequested = { revokeSession = it }
            )
        }
    }

    if (openChangePasswordDialog) {
        val state by remember { mutableStateOf(ChangePasswordState()) }
        ChangePasswordDialog(state = state, onChangeRequested = {
            coroutine.launch {
                state.loading = true
                val res = component.changePassword(it.oldPassword, it.newPassword)
                state.latestResult = res
                state.loading = false
            }
        }, onDismissRequested = { openChangePasswordDialog = false })
    }

    invitationState?.let {
        InvitationDialog(
            state = it,
            onDismissRequested = { invitationState = null }
        )
    }

    revokeSession?.let {
        RevokeSessionDialog(
            onRevokeRequested = {
                coroutine.launch {
                    component.revokeSession(it)
                }
            },
            onDismissRequested = { revokeSession = null }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AccountCard(component: AccountCapability, model: AppViewModel, onChangePasswordRequested: () -> Unit) {
    val coroutine = rememberCoroutineScope()
    Card {
        Column(Modifier.padding(PaddingLarge * 2).fillMaxWidth()) {
            FlowRow(verticalArrangement = Arrangement.Center) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "account card",
                    modifier = Modifier.padding(end = PaddingLarge).size(64.dp)
                )
                AnimatedContent(component.account) { user ->
                    if (user == null) {
                        Column {
                            Box(Modifier.size(280.dp, 40.dp).shimmerBackground())
                            Spacer(Modifier.height(PaddingMedium))
                            Box(Modifier.size(120.dp, 26.dp).shimmerBackground())
                        }
                    } else {
                        Column {
                            BasicTextField(
                                value = user.deviceName,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = LocalContentColor.current),
                                onValueChange = {},
                                readOnly = true
                            )
                            Spacer(Modifier.height(PaddingMedium))
                            Text(user.role.string())
                        }
                    }
                }

                Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
                    TooltipBox(
                        positionProvider = rememberComponentRectPositionProvider(),
                        state = rememberTooltipState(),
                        tooltip = {
                            PlainTooltip {
                                Text(stringResource(Res.string.sign_out_para))
                            }
                        },
                    ) {
                        IconButton(onClick = {
                            model.configurations.token = null
                            model.configurations.firstLaunch = true
                            coroutine.launch {
                                component.session?.let {
                                    component.revokeSession(it)
                                }
                                model.configurations.save()
                            }
                        }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.OpenInNew,
                                contentDescription = "sign out"
                            )
                        }
                    }
                }
                Spacer(Modifier.height(PaddingMedium))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    OutlinedButton(onClick = onChangePasswordRequested) {
                        Text(stringResource(Res.string.change_password_para))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SessionsCards(component: AccountCapability, revoke: Session?, onRevokeRequested: (Session) -> Unit) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(PaddingLarge),
        verticalArrangement = Arrangement.spacedBy(PaddingLarge)
    ) {
        component.sessions.forEach { session ->
            SessionCard(
                model = session,
                modifier = Modifier.weight(1f).widthIn(min = 150.dp),
                isRevoking = revoke == session,
                isCurrent = session.id == component.session?.id,
                onRevokeRequested = onRevokeRequested
            )
        }
    }
}

@Composable
private fun SessionCard(
    model: Session,
    isRevoking: Boolean,
    isCurrent: Boolean,
    onRevokeRequested: (Session) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lastAccess by remember(model) {
        derivedStateOf {
            Instant.ofEpochMilli(model.lastAccess.toEpochMilli()).atZone(ZoneId.systemDefault())
        }
    }
    val now = rememberNow().atZone(ZoneId.systemDefault())
    var expand by remember { mutableStateOf(false) }
    val cropProgress by animateFloatAsState(
        if (expand || isRevoking) 1f else 0f,
        tween(durationMillis = LocalViewConfiguration.current.longPressTimeoutMillis.toInt())
    )

    ElevatedCard(modifier) {
        Box {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.pointerInput(isCurrent) {
                    if (isCurrent) {
                        return@pointerInput
                    }
                    awaitEachGesture {
                        val pointer = awaitFirstDown().id
                        expand = true
                        val pressed = awaitLongPressOrCancellation(pointer)
                        if (pressed != null) {
                            onRevokeRequested(model)
                        }
                        expand = false
                    }
                }
            ) {
                Column(Modifier.padding(PaddingLarge).fillMaxSize()) {
                    OsAvatar(model, Modifier.size(64.dp).padding(PaddingLarge).align(Alignment.CenterHorizontally))
                    Text(model.os, style = MaterialTheme.typography.titleMedium)
                    Text(
                        text = if (!isCurrent) lastAccess.relativeTo(now) else stringResource(Res.string.current_session_span),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (cropProgress > 0f)
                Surface(
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.matchParentSize().drawWithContent {
                        if (layoutDirection == LayoutDirection.Rtl) {
                            clipRect(left = size.width * cropProgress) {
                                this@drawWithContent.drawContent()
                            }
                        } else {
                            clipRect(right = size.width * cropProgress) {
                                this@drawWithContent.drawContent()
                            }
                        }
                    }
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "revoke session")
                        Text(stringResource(Res.string.revoke_session_header))
                    }
                }
        }
    }
}

@Composable
private fun OsAvatar(model: Session, modifier: Modifier = Modifier) {
    if (model.os.startsWith("windows", ignoreCase = true)) {
        Icon(
            painter = painterResource(Res.drawable.windows_logo),
            contentDescription = "windows logo",
            modifier = modifier
        )
    } else if (model.os.startsWith("macos", ignoreCase = true)) {
        Icon(
            painter = painterResource(Res.drawable.apple_logo),
            contentDescription = "apple logo",
            modifier = modifier
        )
    } else if (model.os.startsWith("linux", ignoreCase = true)) {
        Image(
            painter = painterResource(Res.drawable.linux_logo),
            contentDescription = "linux logo",
            modifier = modifier
        )
    } else {
        Icon(
            imageVector = Icons.Default.DeviceUnknown,
            contentDescription = "unknown os",
            modifier = modifier
        )
    }
}

@Stable
private data class ChangePasswordModel(val oldPassword: String, val newPassword: String)
private class ChangePasswordState {
    var loading: Boolean by mutableStateOf(false)
    var latestResult: AccountCapability.ChangePasswordResult? by mutableStateOf(null)
    var willClose: Boolean by mutableStateOf(false)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChangePasswordDialog(
    state: ChangePasswordState,
    onDismissRequested: () -> Unit,
    onChangeRequested: (ChangePasswordModel) -> Unit,
) {
    var old by remember { mutableStateOf("") }
    var new by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf("") }
    val notMatch by remember { derivedStateOf { new != repeat } }
    val canProceed by remember { derivedStateOf { !notMatch && repeat.isNotBlank() && !state.willClose } }
    var showOld by remember { mutableStateOf(false) }
    var showNew by remember { mutableStateOf(false) }
    var showRepeat by remember { mutableStateOf(false) }

    BasicAlertDialog(onDismissRequested) {
        Card {
            AnimatedVisibility(
                state.loading, enter = fadeIn() + expandVertically(), exit = fadeOut() + shrinkVertically()
            ) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
            Column(Modifier.padding(PaddingLarge * 2).padding(top = PaddingMedium)) {
                Icon(
                    imageVector = Icons.Default.Password,
                    contentDescription = "change password",
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                )
                Spacer(Modifier.height(PaddingLarge))
                Text(
                    stringResource(Res.string.change_password_para),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = PaddingLarge).align(Alignment.CenterHorizontally)
                )
                state.latestResult?.let {
                    Text(
                        it.string(),
                        style = MaterialTheme.typography.bodyMedium.copy(color = MaterialTheme.colorScheme.error)
                    )
                    LaunchedEffect(true) {
                        state.willClose = true
                        delay(3.seconds)
                        onDismissRequested()
                    }
                }
                Spacer(Modifier.height(PaddingLarge))
                OutlinedPasswordTextField(value = old,
                    onValueChange = { old = it },
                    label = { Text(stringResource(Res.string.old_password_para)) },
                    enabled = !state.loading,
                    showPassword = showOld,
                    onShowPasswordChanged = { showOld = it })
                OutlinedPasswordTextField(value = new,
                    onValueChange = { new = it },
                    label = { Text(stringResource(Res.string.new_password_para)) },
                    enabled = !state.loading,
                    showPassword = showNew,
                    onShowPasswordChanged = { showNew = it })
                OutlinedPasswordTextField(value = repeat,
                    onValueChange = { repeat = it },
                    label = { Text(stringResource(Res.string.new_password_repeated_para)) },
                    enabled = !state.loading,
                    isError = notMatch,
                    showPassword = showRepeat,
                    onShowPasswordChanged = { showRepeat = it },
                    supportingText = {
                        if (notMatch) Text(stringResource(Res.string.passwords_mismatch_para))
                    })
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(
                        onClick = {
                            onChangeRequested(ChangePasswordModel(old, new))
                        }, enabled = canProceed
                    ) {
                        Text(stringResource(Res.string.ok_caption))
                    }
                }
            }
        }
    }
}

@Stable
private class InvitationState(val component: ModAccountCapability) {
    var role: UserRole by mutableStateOf(UserRole.ROLE_READER)
    var password: ModAccountCapability.TemporaryPassword? by mutableStateOf(null)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun InvitationDialog(
    state: InvitationState,
    onDismissRequested: () -> Unit,
) {
    val now = rememberNow()
    val remainingRatioTarget by remember(
        now,
        state.password
    ) {
        derivedStateOf {
            state.password?.let {
                ChronoUnit.SECONDS.between(
                    now,
                    it.createTime.plus(
                        it.expireSeconds.toLong(),
                        ChronoUnit.SECONDS
                    )
                ).toFloat() / it.expireSeconds
            } ?: 1f
        }
    }
    val ratio by animateFloatAsState(remainingRatioTarget, tween(1000, easing = LinearEasing))

    LaunchedEffect(state.role) {
        val res = state.component.inviteUser(state.role)
        res.collect {
            state.password = it
        }
        if (state.password == null) {
            onDismissRequested()
        }
    }

    BasicAlertDialog(onDismissRequested) {
        Card {
            Column(Modifier.padding(PaddingLarge * 2)) {
                Text(
                    stringResource(Res.string.invite_user_para),
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(vertical = PaddingLarge)
                )
                Text(
                    stringResource(Res.string.under_same_server_use_following_password),
                    textAlign = TextAlign.Start
                )
                Spacer(Modifier.height(PaddingLarge * 2))
                state.password.let {
                    if (it == null) {
                        Box(Modifier.size(300.dp, 66.dp).shimmerBackground())
                    } else {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            BasicTextField(
                                value = it.password,
                                textStyle = MaterialTheme.typography.titleLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                                onValueChange = {},
                                readOnly = true
                            )
                            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                                CircularProgressIndicator(progress = { ratio }, modifier = Modifier.size(32.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RevokeSessionDialog(onRevokeRequested: () -> Unit, onDismissRequested: () -> Unit) {
    AlertDialog(
        icon = { Icon(imageVector = Icons.Default.DeleteForever, contentDescription = "revoke session") },
        title = { Text(stringResource(Res.string.revoke_session_para)) },
        text = { Text(stringResource(Res.string.this_operation_cannot_be_undone_para)) },
        confirmButton = {
            TextButton(onClick = {
                onRevokeRequested()
                onDismissRequested()
            }) {
                Text(stringResource(Res.string.ok_caption))
            }
        },
        dismissButton = { TextButton(onClick = onDismissRequested) { Text(stringResource(Res.string.cancel_para)) } },
        onDismissRequest = onDismissRequested
    )
}