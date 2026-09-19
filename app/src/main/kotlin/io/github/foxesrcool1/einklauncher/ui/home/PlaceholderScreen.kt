package io.github.foxesrcool1.einklauncher.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold

/**
 * A tab that is not built yet.
 *
 * It says which step builds it, so the owner always knows where the work is.
 */
@Composable
fun PlaceholderScreen(
    route: LauncherRoute,
    plannedStep: String,
    summary: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ScreenScaffold(
        title = route.title,
        overline = "Not built yet",
        corner = null,
        modifier = modifier,
    ) {
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.Start,
            ) {
                CapsLabel(text = plannedStep, style = EinkType.capsSmall)
                Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                EinkText(text = summary, style = EinkType.body.copy(color = EinkColors.Faded))
            }
        }
        InvertPressButton(text = "Back to today", onClick = onBack)
    }
}
