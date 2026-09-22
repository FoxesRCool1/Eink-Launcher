package io.github.foxesrcool1.margin.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkDimens

/**
 * A fine rule across the page.
 *
 * E-ink rule 5: lines are 1 dp or 2 dp. There is no shadow and no gradient to
 * separate two blocks, so a rule does that job.
 */
@Composable
fun HairlineDivider(
    modifier: Modifier = Modifier,
    thickness: Dp = EinkDimens.hairline,
    color: Color = EinkColors.Ink,
    startInset: Dp = 0.dp,
    endInset: Dp = 0.dp,
) {
    Box(
        modifier = modifier
            .padding(start = startInset, end = endInset)
            .fillMaxWidth()
            .height(thickness)
            .background(color),
    )
}
