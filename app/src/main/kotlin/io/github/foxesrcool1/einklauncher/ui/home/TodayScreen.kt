package io.github.foxesrcool1.einklauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.BotanicalCorner
import io.github.foxesrcool1.einklauncher.design.components.BotanicalSprig
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.WordMenu
import io.github.foxesrcool1.einklauncher.design.components.WordMenuItem
import java.time.LocalDateTime

/**
 * The home screen.
 *
 * Date and time at the top centre in small tracked capitals. Four large serif
 * words on the left. A status line and a way into Settings at the bottom. One
 * corner drawing. A lot of white space and nothing that moves.
 */
@Composable
fun TodayScreen(
    onOpenTab: (LauncherRoute) -> Unit,
    onOpenSettings: () -> Unit,
    onQuickNote: () -> Unit = {},
    nextRoutineLabel: String? = null,
    onStartNext: () -> Unit = {},
    modifier: Modifier = Modifier,
    now: LocalDateTime? = null,
    use24Hour: Boolean = true,
    battery: Int? = null,
    wifi: Boolean? = null,
) {
    val context = LocalContext.current
    val clock by rememberMinuteClock()
    val time = now ?: clock

    // Both are read once a minute, with the clock, and not on a timer of their
    // own. E-ink rule 7: as few screen changes as possible.
    val batteryPercent = remember(time.minute, battery) { battery ?: batteryPercent(context) }
    val onWifi = remember(time.minute, wifi) { wifi ?: onWifi(context) }

    Box(modifier = modifier.fillMaxSize()) {
        BotanicalSprig(
            corner = BotanicalCorner.BottomEnd,
            modifier = Modifier.align(Alignment.BottomEnd),
            drawingSize = 150.dp,
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(EinkDimens.screenMargin),
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            CapsLabel(
                text = HomeStrings.date(time),
                style = EinkType.caps.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(modifier = Modifier.height(6.dp))
            CapsLabel(
                text = HomeStrings.time(time, use24Hour),
                style = EinkType.caps.copy(textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(modifier = Modifier.height(EinkDimens.blockGap))
            HairlineDivider()
            Spacer(modifier = Modifier.height(56.dp))

            WordMenu(
                items = LauncherRoute.tabs.map { WordMenuItem(it.title) },
                selectedIndex = null,
                onSelect = { index -> onOpenTab(LauncherRoute.tabs[index]) },
            )

            Spacer(modifier = Modifier.weight(1f))

            // The next thing in the user's own routine, if they have one. It
            // is left out entirely when the routine is empty or finished,
            // rather than showing an empty box: a blank line on e-ink is a
            // repaint for nothing.
            if (nextRoutineLabel != null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        CapsLabel(text = "Next", style = EinkType.capsSmall)
                        Spacer(modifier = Modifier.height(4.dp))
                        EinkText(
                            text = nextRoutineLabel,
                            style = EinkType.rowTitle,
                            maxLines = 1,
                        )
                    }
                    InvertPressButton(text = "Start", onClick = onStartNext)
                }
                Spacer(modifier = Modifier.height(EinkDimens.blockGap))
            }

            HairlineDivider(color = EinkColors.Faded)
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CapsLabel(
                    text = HomeStrings.status(batteryPercent, onWifi),
                    style = EinkType.capsSmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                    InvertPressButton(
                        text = "Quick note",
                        onClick = onQuickNote,
                        bordered = false,
                    )
                    InvertPressButton(
                        text = "Settings",
                        onClick = onOpenSettings,
                        bordered = false,
                    )
                }
            }
        }
    }
}
