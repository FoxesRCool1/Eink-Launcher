package io.github.foxesrcool1.einklauncher.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.foxesrcool1.einklauncher.core.eink.EinkDevices
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import kotlinx.coroutines.launch

/**
 * The pen and the panel.
 *
 * Both depend on what the device test found, so both start in the safe
 * state: this app paints the ink, and nothing asks for a full refresh.
 */
@Composable
fun PenSection(settings: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val device = remember { EinkDevices.get(context) }
    val fastPen by settings.fastPenMode.collectAsStateWithLifecycle(initialValue = SettingsStore.FAST_PEN_OFF)
    val delay by settings.inkRedrawDelayMillis.collectAsStateWithLifecycle(initialValue = SettingsStore.DEFAULT_INK_DELAY_MILLIS)

    CapsLabel(text = "Pen", style = EinkType.capsSmall)
    HairlineDivider(color = EinkColors.Faded)
    Spacer(modifier = Modifier.height(10.dp))

    if (!device.hasVendorControl) {
        EinkText(
            text = "This device has no fast pen that the app knows. The app paints the ink itself.",
            style = EinkType.body,
        )
    } else {
        EinkText(
            text = "Who paints the line while the pen is down. Run the device test first, " +
                "then pick the path that worked there.",
            style = EinkType.body,
        )
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(
                text = "This app",
                selected = fastPen == SettingsStore.FAST_PEN_OFF,
                onClick = { scope.launch { settings.setFastPenMode(SettingsStore.FAST_PEN_OFF) } },
            )
            InvertPressButton(
                text = "Path A",
                selected = fastPen == SettingsStore.FAST_PEN_WRITING,
                onClick = { scope.launch { settings.setFastPenMode(SettingsStore.FAST_PEN_WRITING) } },
            )
            InvertPressButton(
                text = "Path B",
                selected = fastPen == SettingsStore.FAST_PEN_AUTODRAW,
                onClick = { scope.launch { settings.setFastPenMode(SettingsStore.FAST_PEN_AUTODRAW) } },
            )
        }
        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
        InvertPressButton(
            text = "Real stroke after $delay ms",
            enabled = fastPen != SettingsStore.FAST_PEN_OFF,
            onClick = {
                val steps = listOf(600L, 900L, 1200L, 1600L)
                val next = steps[(steps.indexOf(delay) + 1).mod(steps.size)]
                scope.launch { settings.setInkRedrawDelayMillis(next) }
            },
        )
    }

}

/** The panel: when to ask for a full refresh. */
@Composable
fun ScreenSection(settings: SettingsStore) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val device = remember { EinkDevices.get(context) }
    val fullRefresh by settings.fullRefreshOnBigChange.collectAsStateWithLifecycle(initialValue = false)

    CapsLabel(text = "Screen", style = EinkType.capsSmall)
    HairlineDivider(color = EinkColors.Faded)
    Spacer(modifier = Modifier.height(10.dp))

    Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
        InvertPressButton(
            text = if (fullRefresh) "Refresh on screen change: on" else "Refresh on screen change: off",
            enabled = device.hasVendorControl,
            onClick = { scope.launch { settings.setFullRefreshOnBigChange(!fullRefresh) } },
        )
    }
    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
    InvertPressButton(
        text = "Full refresh now",
        enabled = device.hasVendorControl,
        onClick = { device.fullRefresh() },
    )
}
