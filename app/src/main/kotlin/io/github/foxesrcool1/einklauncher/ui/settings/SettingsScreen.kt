package io.github.foxesrcool1.einklauncher.ui.settings

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.einklauncher.BuildConfig
import io.github.foxesrcool1.einklauncher.core.launcher.DefaultLauncher
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import kotlinx.coroutines.launch

private const val TAG = "SettingsScreen"

/**
 * The settings screen.
 *
 * Step 1 and Step 3 fill the parts that exist today: the default launcher
 * flow, the corner art switch, the log viewer and the design demo. The data
 * folder, backup and refresh behaviour arrive with Step 4 and Step 2.
 */
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenLog: () -> Unit,
    onOpenDemo: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings = remember(context) { SettingsStore(context) }

    val artOn by settings.botanicalArt.collectAsStateWithLifecycle(initialValue = true)
    var isDefault by remember { mutableStateOf(DefaultLauncher.isDefault(context)) }
    var showManualSteps by remember { mutableStateOf(false) }
    var lastMessage by remember { mutableStateOf<String?>(null) }

    val roleLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult(),
    ) {
        isDefault = DefaultLauncher.isDefault(context)
        AppLog.i(TAG, "Home role request came back. Default now: $isDefault")
    }

    ScreenScaffold(
        title = "Settings",
        overline = "Eink Launcher ${BuildConfig.VERSION_NAME}",
        corner = null,
        modifier = modifier,
    ) {
        CapsLabel(text = "Home app", style = EinkType.capsSmall)
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(10.dp))

        EinkText(
            text = if (isDefault) {
                "Eink Launcher is the home app on this tablet."
            } else {
                "Eink Launcher is not the home app yet."
            },
            style = EinkType.body,
        )
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))

        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(
                text = "Set as home",
                enabled = !isDefault,
                onClick = {
                    lastMessage = setAsHome(
                        context = context,
                        startRoleRequest = { intent -> roleLauncher.launch(intent) },
                        showSteps = { showManualSteps = true },
                    )
                },
            )
            InvertPressButton(
                text = "Check again",
                onClick = { isDefault = DefaultLauncher.isDefault(context) },
            )
        }

        if (lastMessage != null) {
            Spacer(modifier = Modifier.height(10.dp))
            CapsLabel(
                text = lastMessage.orEmpty(),
                style = EinkType.capsSmall.copy(color = EinkColors.Faded),
                maxLines = 2,
            )
        }

        if (showManualSteps) {
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            Column(modifier = Modifier.fillMaxWidth()) {
                DefaultLauncher.manualSteps.forEachIndexed { index, step ->
                    EinkText(
                        text = "${index + 1}. $step",
                        style = EinkType.body,
                        modifier = Modifier.padding(vertical = 4.dp),
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        CapsLabel(text = "Look", style = EinkType.capsSmall)
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(10.dp))

        InvertPressButton(
            text = if (artOn) "Corner drawing is on" else "Corner drawing is off",
            onClick = { scope.launch { settings.setBotanicalArt(!artOn) } },
        )

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        StorageSection()

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
        CapsLabel(text = "Trouble shooting", style = EinkType.capsSmall)
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(10.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(text = "Log", onClick = onOpenLog)
            InvertPressButton(text = "Design demo", onClick = onOpenDemo)
        }

        Spacer(modifier = Modifier.weight(1f))
        HairlineDivider(color = EinkColors.Faded)
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            CapsLabel(
                text = "Build ${BuildConfig.VERSION_NAME}",
                style = EinkType.capsSmall.copy(color = EinkColors.Faded),
            )
            InvertPressButton(text = "Today", onClick = onBack, bordered = false)
        }
    }
}

/**
 * The three ways to become the home app, in order. Returns the line to show
 * the user.
 */
private fun setAsHome(
    context: android.content.Context,
    startRoleRequest: (Intent) -> Unit,
    showSteps: () -> Unit,
): String {
    DefaultLauncher.roleRequestIntent(context)?.let { intent ->
        AppLog.i(TAG, "Asking for the home role")
        return runCatching {
            startRoleRequest(intent)
            "The system is asking. Choose Eink Launcher."
        }.getOrElse {
            AppLog.w(TAG, "The home role request would not start", it)
            openSettingsOrSteps(context, showSteps)
        }
    }
    return openSettingsOrSteps(context, showSteps)
}

private fun openSettingsOrSteps(
    context: android.content.Context,
    showSteps: () -> Unit,
): String {
    val intent = DefaultLauncher.homeSettingsIntent(context)
        ?: DefaultLauncher.allSettingsIntent(context)

    if (intent != null) {
        val started = runCatching { context.startActivity(intent) }.isSuccess
        if (started) {
            AppLog.i(TAG, "Opened a settings screen to pick the home app")
            return "Settings is open. Pick Eink Launcher as the home app."
        }
    }

    AppLog.w(TAG, "No settings route worked. Showing the written steps.")
    showSteps()
    return "This tablet hides the settings app. Follow the steps below."
}
