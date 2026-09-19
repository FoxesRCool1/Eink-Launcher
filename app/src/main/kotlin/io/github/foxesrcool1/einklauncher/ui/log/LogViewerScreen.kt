package io.github.foxesrcool1.einklauncher.ui.log

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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.log.LogLine
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.rememberPagedListState
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold

/**
 * The in-app log viewer.
 *
 * There is no logcat on this tablet, so this screen is the only way the owner
 * can see what the app did. "Copy to Download" writes every log file into
 * `Download/EinkLauncher/`, where a file manager can reach it.
 */
@Composable
fun LogViewerScreen(
    modifier: Modifier = Modifier,
    linesProvider: () -> List<LogLine> = { AppLog.recentLines() },
    onCopyToDownloads: (() -> Int)? = null,
    onBack: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    var refreshToken by remember { mutableStateOf(0) }
    var status by remember { mutableStateOf<String?>(null) }

    val lines = remember(refreshToken) { linesProvider().asReversed() }
    val state = rememberPagedListState()

    ScreenScaffold(
        title = "Log",
        overline = "Newest first",
        corner = null,
        modifier = modifier,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
        ) {
            if (onBack != null) {
                InvertPressButton(text = "Back", onClick = onBack, bordered = false)
            }
            InvertPressButton(text = "Refresh", onClick = { refreshToken++ })
            InvertPressButton(
                text = "Copy to download",
                onClick = {
                    val copied = onCopyToDownloads?.invoke()
                        ?: AppLog.copyAllToDownloads(context)
                    status = "Copied $copied file(s) to Download/EinkLauncher"
                    refreshToken++
                },
            )
        }

        if (status != null) {
            Spacer(modifier = Modifier.height(EinkDimens.targetGap))
            CapsLabel(text = status.orEmpty(), style = EinkType.capsSmall)
        }

        Spacer(modifier = Modifier.height(EinkDimens.blockGap))

        PagedList(
            items = lines,
            pageSize = 12,
            state = state,
            emptyText = "No log lines yet",
            modifier = Modifier.weight(1f),
        ) { _, line ->
            LogRow(line)
        }
    }
}

@Composable
private fun LogRow(line: LogLine) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        CapsLabel(
            text = "${line.level.short}  ${line.tag}",
            style = EinkType.capsSmall.copy(
                color = if (line.level.short == 'E') EinkColors.Ink else EinkColors.Faded,
            ),
        )
        EinkText(
            text = line.message,
            style = EinkType.body,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(modifier = Modifier.height(4.dp))
        HairlineDivider(color = EinkColors.Faded)
    }
}
