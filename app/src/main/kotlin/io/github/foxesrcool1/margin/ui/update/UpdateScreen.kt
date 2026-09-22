package io.github.foxesrcool1.margin.ui.update

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.margin.BuildConfig
import io.github.foxesrcool1.margin.core.update.ApkInstaller
import io.github.foxesrcool1.margin.core.update.AppVersion
import io.github.foxesrcool1.margin.core.update.UpdateManager
import io.github.foxesrcool1.margin.core.update.UpdateState
import io.github.foxesrcool1.margin.core.update.UpdateToken
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.CapsLabel
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.HairlineDivider
import io.github.foxesrcool1.margin.design.components.IconPressButton
import io.github.foxesrcool1.margin.design.components.InvertPressButton
import io.github.foxesrcool1.margin.design.icons.Lucide
import io.github.foxesrcool1.margin.design.components.TextPromptDialog
import io.github.foxesrcool1.margin.ui.common.ScreenScaffold

/**
 * Settings, Help, "Check for updates".
 *
 * Opening the screen is the user asking, so it checks at once. After that it
 * only shows what [UpdateManager] is doing. The download goes on if the user
 * leaves, and this screen picks the state up again when they come back.
 */
@Composable
fun UpdateScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val state by UpdateManager.state.collectAsStateWithLifecycle()

    var installAllowed by remember { mutableStateOf(ApkInstaller.isAllowed(context)) }
    var hasToken by remember { mutableStateOf(UpdateToken.read(context) != null) }
    var askForToken by remember { mutableStateOf(false) }

    // The user comes back from the system screen where they allowed installs.
    LifecycleResumeEffect(Unit) {
        installAllowed = ApkInstaller.isAllowed(context)
        onPauseOrDispose { }
    }

    // The button that leads here says "Check for updates", so it checks,
    // also when an answer from an hour ago is still standing. A download or
    // a finished download is left alone.
    LaunchedEffect(Unit) {
        val now = UpdateManager.state.value
        if (now !is UpdateState.Downloading && now !is UpdateState.Ready) UpdateManager.check(context)
    }

    // One press covers "download" and "install". The install only starts from
    // here, while this screen is in front, because Android does not let an
    // app that is out of sight open the install window.
    LaunchedEffect(state) {
        val ready = state as? UpdateState.Ready
        if (ready != null && !ready.handedOver) UpdateManager.install(context)
    }

    UpdateContent(
        state = state,
        installAllowed = installAllowed,
        hasToken = hasToken,
        onBack = onBack,
        onCheck = { UpdateManager.check(context) },
        onDownload = { UpdateManager.download(context) },
        onInstall = { UpdateManager.install(context) },
        onAllowInstalls = { ApkInstaller.openPermissionScreen(context) },
        onToken = { askForToken = true },
        modifier = modifier,
    )

    if (askForToken) {
        TextPromptDialog(
            title = "GitHub access token",
            // The saved token is not shown again. An empty answer removes it.
            initialValue = "",
            allowEmpty = true,
            onConfirm = { typed ->
                askForToken = false
                UpdateToken.save(context, typed)
                hasToken = UpdateToken.read(context) != null
                UpdateManager.check(context)
            },
            onDismiss = { askForToken = false },
        )
    }
}

/** The screen with nothing behind it, so a screenshot test can show every state. */
@Composable
fun UpdateContent(
    state: UpdateState,
    installAllowed: Boolean,
    hasToken: Boolean,
    onBack: () -> Unit,
    onCheck: () -> Unit,
    onDownload: () -> Unit,
    onInstall: () -> Unit,
    onAllowInstalls: () -> Unit,
    onToken: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val busy = state is UpdateState.Checking || state is UpdateState.Downloading

    ScreenScaffold(
        title = "Updates",
        overline = "Margin ${BuildConfig.VERSION_NAME}",
        modifier = modifier,
        onBack = onBack,
        backIcon = Lucide.ArrowLeft,
        backLabel = "Back",
        actions = {
            IconPressButton(icon = Lucide.RefreshCw, label = "Check again", enabled = !busy, bordered = true, onClick = onCheck)
        },
    ) {

        // Cut off at the footer if a long message ever grows too tall. The
        // footer holds the permission and the token, and must stay in reach.
        Column(modifier = Modifier.fillMaxWidth().weight(1f).clipToBounds()) {
            EinkText(text = UpdateStrings.headline(state), style = EinkType.rowTitle)
            UpdateStrings.detail(state)?.let { detail ->
                Spacer(modifier = Modifier.height(6.dp))
                // Black, not grey: e-ink rule 4 keeps grey for large inactive
                // text, and the reason an update failed is neither.
                EinkText(text = detail, style = EinkType.body, maxLines = 5, overflow = TextOverflow.Ellipsis)
            }

            when (state) {
                is UpdateState.Available -> {
                    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    InvertPressButton(text = "Download and install", icon = Lucide.Download, onClick = onDownload)
                    val notes = UpdateStrings.plainNotes(state.release.notes)
                    if (notes.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
                        CapsLabel(text = "What is new", style = EinkType.capsSmall)
                        HairlineDivider(color = EinkColors.Faded)
                        Spacer(modifier = Modifier.height(6.dp))
                        EinkText(
                            text = notes,
                            style = EinkType.body,
                            maxLines = 9,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }

                is UpdateState.Ready -> {
                    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    InvertPressButton(text = "Install", icon = Lucide.Download, onClick = onInstall)
                }

                is UpdateState.Failed -> if (state.tokenProblem) {
                    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    InvertPressButton(text = "Enter access token", onClick = onToken)
                }

                else -> Unit
            }
        }

        // The footer shows only when it has something to say. The repository
        // is public, so a token is only ever for a private fork, and the way
        // to enter one is the button that comes with the "not found" failure.
        if (!installAllowed || hasToken) {
            HairlineDivider(color = EinkColors.Faded)
            Spacer(modifier = Modifier.height(10.dp))
            if (!installAllowed) {
                EinkText(
                    text = "Android must allow this app to install its updates. Press the button, " +
                        "turn the switch on, then come back.",
                    style = EinkType.body,
                )
                Spacer(modifier = Modifier.height(10.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                if (!installAllowed) InvertPressButton(text = "Allow installs", onClick = onAllowInstalls)
                if (hasToken) InvertPressButton(text = "Change token", onClick = onToken)
            }
            if (hasToken) {
                Spacer(modifier = Modifier.height(10.dp))
                CapsLabel(
                    text = "An access token is saved",
                    style = EinkType.capsSmall.copy(color = EinkColors.Faded),
                )
            }
        }
    }
}

/** The words of the update screen. Kept apart so a plain test can read them. */
object UpdateStrings {

    fun headline(state: UpdateState): String = when (state) {
        UpdateState.Idle -> "Not checked yet."
        UpdateState.Checking -> "Asking GitHub."
        is UpdateState.UpToDate -> "This is the newest version."
        is UpdateState.Available -> "Version ${state.release.version} is ready."
        is UpdateState.Downloading -> "Downloading version ${state.release.version}. ${state.percent} percent."
        is UpdateState.Ready ->
            if (state.handedOver) "Version ${state.release.version} is downloaded." else "Starting the install."
        is UpdateState.Failed -> "That did not work."
    }

    fun detail(state: UpdateState): String? = when (state) {
        UpdateState.Idle, UpdateState.Checking -> null
        is UpdateState.UpToDate ->
            if (state.newest == null) "GitHub has no release for this build yet." else "GitHub has ${state.newest}."
        is UpdateState.Available -> "${megabytes(state.asset.sizeBytes)}. Your notes, books and settings stay."
        is UpdateState.Downloading -> "You can leave this screen. The download goes on."
        is UpdateState.Ready -> state.note
        is UpdateState.Failed -> state.message
    }

    fun megabytes(bytes: Long): String = "${(bytes + 524_288) / 1_048_576} MB"

    /**
     * Release notes are Markdown. The screen shows plain text, so this takes
     * off the heading marks, joins a wrapped line back to its start, and
     * drops the blank lines.
     */
    fun plainNotes(markdown: String): String {
        val lines = mutableListOf<String>()
        markdown.lines().forEach { raw ->
            val line = raw.trim().trimStart('#').trim()
            when {
                line.isEmpty() -> Unit
                // A wrapped bullet: the next line starts with spaces, not with a mark.
                raw.startsWith("  ") && !line.startsWith("- ") && lines.isNotEmpty() ->
                    lines[lines.size - 1] = lines.last() + " " + line
                else -> lines += line
            }
        }
        // The headline already names the version. A first line that only
        // repeats it is dropped.
        if (lines.firstOrNull()?.let(AppVersion::parse) != null) lines.removeAt(0)
        return lines.joinToString("\n")
    }
}
