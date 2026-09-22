package io.github.foxesrcool1.margin.design.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens
import io.github.foxesrcool1.margin.design.LocalBotanicalArtEnabled
import io.github.foxesrcool1.margin.design.icons.Lucide
import io.github.foxesrcool1.margin.design.icons.LucideIcon

/**
 * One plant, as fine line art.
 *
 * The plants are Lucide icons, like every other drawing in the app, so the
 * whole app is drawn by one hand. A plant is drawn large and with the fine
 * 1 dp line, which is what makes it read as a drawing and not as a control.
 * It is pure black line on white, so it needs no dither.
 *
 * The user can turn the plants off, and [LocalBotanicalArtEnabled] carries
 * that setting.
 */
@Composable
fun PlantArt(
    plant: LucideIcon,
    modifier: Modifier = Modifier,
    size: Dp = 120.dp,
    color: Color = EinkColors.Ink,
) {
    if (!LocalBotanicalArtEnabled.current) return
    EinkIcon(
        icon = plant,
        modifier = modifier,
        size = size,
        color = color,
        strokeWidth = EinkDimens.hairline,
    )
}

/** Which plant grows on which screen. */
object Plants {
    val Home: LucideIcon = Lucide.Sprout
    val Reading: LucideIcon = Lucide.TreeDeciduous
    val Writing: LucideIcon = Lucide.Leaf
    val Journal: LucideIcon = Lucide.Flower2
    val Apps: LucideIcon = Lucide.Clover
    val Settings: LucideIcon = Lucide.Shrub
    val Other: LucideIcon = Lucide.Wheat
}
