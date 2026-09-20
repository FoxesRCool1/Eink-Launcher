package io.github.foxesrcool1.einklauncher.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.foxesrcool1.einklauncher.core.window.ScreenWindow
import io.github.foxesrcool1.einklauncher.core.window.findActivity
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.IconPressButton
import io.github.foxesrcool1.einklauncher.design.components.PlantArt
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.design.icons.LucideIcon

/**
 * True when the space a screen has is wider than it is tall: the tablet on
 * its side. A screen reads this to lay itself out, and not the orientation of
 * the device, because half of a split screen is tall even on a wide tablet.
 */
val LocalWideScreen = staticCompositionLocalOf { false }

/**
 * The shape every screen shares.
 *
 * One line at the top: the way back, the title, and the small icon that turns
 * the screen. Then a rule, then the content. Nothing moves and nothing
 * scrolls.
 *
 * [actions] are the controls of the screen. Upright, they get a line of their
 * own under the rule. On its side the tablet has width to spare and no height,
 * so they move up beside the title.
 */
@Composable
fun ScreenScaffold(
    title: String,
    modifier: Modifier = Modifier,
    overline: String? = null,
    plant: LucideIcon? = null,
    onBack: (() -> Unit)? = null,
    backIcon: LucideIcon = Lucide.House,
    backLabel: String = "Home",
    /** False on the device test, which is locked upright so a turn cannot wipe its results. */
    showRotate: Boolean = true,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val wide = maxWidth > maxHeight

        CompositionLocalProvider(LocalWideScreen provides wide) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .systemBarsPadding()
                    .padding(
                        horizontal = EinkDimens.screenMargin,
                        vertical = if (wide) 16.dp else EinkDimens.screenMargin,
                    ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (onBack != null) {
                        IconPressButton(icon = backIcon, label = backLabel, onClick = onBack)
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    // A long title, like a date in the Journal, gets the smaller
                    // size and the room of the plant. A title that ends in three
                    // dots tells the user nothing.
                    val longTitle = title.length > LONG_TITLE
                    Column(modifier = Modifier.weight(1f)) {
                        if (overline != null) CapsLabel(text = overline, style = EinkType.capsSmall)
                        EinkText(
                            text = title,
                            style = if (wide || longTitle) {
                                EinkType.title.copy(fontSize = 26.sp, lineHeight = 36.sp)
                            } else {
                                EinkType.title
                            },
                            maxLines = 1,
                        )
                    }
                    if (wide && actions != null) {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions,
                        )
                    }
                    if (!wide && !longTitle && plant != null) PlantArt(plant = plant, size = 52.dp)
                    if (showRotate) RotateButton()
                }

                Spacer(modifier = Modifier.height(if (wide) 6.dp else EinkDimens.targetGap))
                HairlineDivider()

                if (!wide && actions != null) {
                    Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
                        verticalAlignment = Alignment.CenterVertically,
                        content = actions,
                    )
                }

                Spacer(modifier = Modifier.height(if (wide) 8.dp else EinkDimens.targetGap))
                content()
            }
        }
    }
}

/** A title with more letters than this does not fit beside the plant at the full size. */
private const val LONG_TITLE = 12

/**
 * Turns the screen on its side, and back. Every screen has one at the top.
 * The owner asked for it to be small and easy to overlook, so it is grey, and
 * smaller than the other icons. The target is still 56 dp.
 */
@Composable
fun RotateButton(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    IconPressButton(
        icon = Lucide.RotateCwSquare,
        label = "Turn the screen",
        quiet = true,
        modifier = modifier,
        onClick = { context.findActivity()?.let(ScreenWindow::toggleLandscape) },
    )
}
