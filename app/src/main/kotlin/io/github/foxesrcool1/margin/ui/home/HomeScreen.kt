package io.github.foxesrcool1.margin.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.EinkShapes
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.CapsLabel
import io.github.foxesrcool1.margin.design.components.EinkIcon
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.HairlineDivider
import io.github.foxesrcool1.margin.design.components.IconPressButton
import io.github.foxesrcool1.margin.design.components.PlantArt
import io.github.foxesrcool1.margin.design.components.Plants
import io.github.foxesrcool1.margin.design.einkClickable
import io.github.foxesrcool1.margin.design.icons.Lucide
import io.github.foxesrcool1.margin.design.icons.LucideIcon
import io.github.foxesrcool1.margin.ui.common.RotateButton
import io.github.foxesrcool1.margin.ui.split.SplitButton
import java.time.LocalDateTime

/** The icon that stands for a tab on Home. */
fun LauncherRoute.icon(): LucideIcon = when (this) {
    LauncherRoute.Reading -> Lucide.BookOpen
    LauncherRoute.Writing -> Lucide.PenLine
    LauncherRoute.Journal -> Lucide.Notebook
    LauncherRoute.Apps -> Lucide.LayoutGrid
    LauncherRoute.Settings -> Lucide.Settings
    else -> Lucide.House
}

/**
 * The home screen.
 *
 * Date and time at the top centre in small tracked capitals. Four large line
 * icons: read, write, journal, apps. A status line and the way into Settings
 * at the bottom. One plant in a corner. A lot of white space and nothing that
 * moves.
 *
 * Upright, the four icons stand two by two. With the tablet on its side they
 * stand in one row, because there the height is what runs out.
 */
@Composable
fun HomeScreen(
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
    /** The name of each tab under its icon. It is a setting, and it starts off. */
    showLabels: Boolean = false,
) {
    val context = LocalContext.current
    val clock by rememberMinuteClock()
    val time = now ?: clock

    // Both are read once a minute, with the clock, and not on a timer of their
    // own. E-ink rule 7: as few screen changes as possible.
    val batteryPercent = remember(time.minute, battery) { battery ?: batteryPercent(context) }
    val onWifi = remember(time.minute, wifi) { wifi ?: onWifi(context) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight

        // Top corner. The bottom edge holds the controls, and a drawing under
        // a control makes both harder to read. A narrow half of the split
        // screen has no corner to spare: the plant would sit on the date.
        if (maxWidth >= 420.dp) PlantArt(
            plant = Plants.Home,
            size = if (wide) 96.dp else 120.dp,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .systemBarsPadding()
                .padding(top = 20.dp, end = 24.dp),
        )
        RotateButton(
            modifier = Modifier
                .align(Alignment.TopStart)
                .systemBarsPadding()
                .padding(start = 12.dp, top = 12.dp),
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(
                    horizontal = EinkDimens.screenMargin,
                    vertical = if (wide) 16.dp else EinkDimens.screenMargin,
                ),
        ) {
            Spacer(modifier = Modifier.height(8.dp))
            if (wide) {
                CapsLabel(
                    text = HomeStrings.date(time) + "   .   " + HomeStrings.time(time, use24Hour),
                    style = EinkType.caps.copy(textAlign = TextAlign.Center),
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
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
            }

            // The four icons get whatever room is left between the header and
            // the controls, and get smaller when that is not enough for them.
            // The controls at the bottom are never the ones that get squeezed:
            // a home screen with a cut off Settings control is a trap.
            BoxWithConstraints(
                modifier = Modifier.fillMaxWidth().weight(1f),
                contentAlignment = Alignment.Center,
            ) {
                val rows = if (wide) 1 else 2
                val gap = 20.dp
                val cell = minOf(
                    (maxHeight - gap * (rows - 1)) / rows,
                    (maxWidth - gap * (4 / rows - 1)) / (4 / rows),
                    148.dp,
                )
                Column(verticalArrangement = Arrangement.spacedBy(gap)) {
                    LauncherRoute.tabs.chunked(4 / rows).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(gap)) {
                            line.forEach { tab ->
                                HomeTab(
                                    route = tab,
                                    side = cell,
                                    showLabel = showLabels,
                                    onClick = { onOpenTab(tab) },
                                )
                            }
                        }
                    }
                }
            }

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
                    IconPressButton(
                        icon = Lucide.Play,
                        label = "Start",
                        onClick = onStartNext,
                        bordered = true,
                    )
                }
                Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            }

            HairlineDivider(color = EinkColors.Faded)
            Spacer(modifier = Modifier.height(if (wide) 4.dp else EinkDimens.targetGap))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StatusLine(
                    batteryPercent = batteryPercent,
                    onWifi = onWifi,
                    modifier = Modifier.weight(1f),
                )
                IconPressButton(icon = Lucide.SquarePen, label = "Quick note", onClick = onQuickNote)
                Spacer(modifier = Modifier.width(4.dp))
                SplitButton()
                Spacer(modifier = Modifier.width(4.dp))
                IconPressButton(icon = Lucide.Settings, label = "Settings", onClick = onOpenSettings)
            }
        }
    }
}

/** One of the four large icons. The whole cell is the target, and the whole cell inverts. */
@Composable
private fun HomeTab(
    route: LauncherRoute,
    side: Dp,
    showLabel: Boolean,
    onClick: () -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()
    val foreground = if (pressed) EinkColors.Paper else EinkColors.Ink

    Column(
        modifier = Modifier
            .size(side)
            .clip(EinkShapes.panel)
            .background(if (pressed) EinkColors.Ink else EinkColors.Paper)
            .einkClickable(interactionSource = interactionSource, onClick = onClick)
            .semantics(mergeDescendants = true) {
                contentDescription = route.title
                role = Role.Button
            },
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        EinkIcon(
            icon = route.icon(),
            size = if (side < 110.dp) 44.dp else EinkDimens.homeIcon,
            color = foreground,
        )
        if (showLabel) {
            Spacer(modifier = Modifier.height(12.dp))
            CapsLabel(
                text = route.title,
                style = EinkType.capsSmall.copy(color = foreground),
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}

/** Wi-Fi and battery, as two icons and one number. A part that is not known is left out. */
@Composable
private fun StatusLine(
    batteryPercent: Int?,
    onWifi: Boolean,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = HomeStrings.status(batteryPercent, onWifi)
        },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        EinkIcon(
            icon = if (onWifi) Lucide.Wifi else Lucide.WifiOff,
            size = 20.dp,
            color = if (onWifi) EinkColors.Ink else EinkColors.Faded,
        )
        if (batteryPercent != null) {
            EinkIcon(icon = HomeStrings.batteryIcon(batteryPercent), size = 20.dp)
            CapsLabel(
                text = "$batteryPercent%",
                style = EinkType.capsSmall,
                modifier = Modifier.clearAndSetSemantics { },
            )
        }
    }
}
