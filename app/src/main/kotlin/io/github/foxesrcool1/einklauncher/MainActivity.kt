package io.github.foxesrcool1.einklauncher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.ui.common.ScreenScaffold
import io.github.foxesrcool1.einklauncher.ui.demo.DesignDemoScreen
import io.github.foxesrcool1.einklauncher.ui.dev.DEV_PANEL_AVAILABLE
import io.github.foxesrcool1.einklauncher.ui.dev.DevPanel
import io.github.foxesrcool1.einklauncher.ui.log.LogViewerScreen

private enum class DemoTab { Design, Log, Dev }

/**
 * Step 1 entry point. It shows the design system, the log viewer and, in a
 * debug build, the dev panel. Step 3 turns this app into a launcher and this
 * screen becomes a debug entry inside Settings.
 */
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppLog.i("MainActivity", "onCreate")
        setContent {
            EinkTheme {
                DemoHost()
            }
        }
    }
}

@Composable
private fun DemoHost() {
    var tab by remember { mutableStateOf(DemoTab.Design) }

    Column(modifier = Modifier.fillMaxSize()) {
        when (tab) {
            DemoTab.Design -> DesignDemoScreen(modifier = Modifier.weight(1f))
            DemoTab.Log -> LogViewerScreen(modifier = Modifier.weight(1f))
            DemoTab.Dev -> DevHost(modifier = Modifier.weight(1f))
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = EinkDimens.screenMargin,
                    end = EinkDimens.screenMargin,
                    bottom = EinkDimens.screenMargin,
                ),
            horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
        ) {
            InvertPressButton(text = "Design", onClick = { tab = DemoTab.Design })
            InvertPressButton(text = "Log", onClick = { tab = DemoTab.Log })
            if (DEV_PANEL_AVAILABLE) {
                InvertPressButton(text = "Dev", onClick = { tab = DemoTab.Dev })
            }
        }
    }
}

@Composable
private fun DevHost(modifier: Modifier = Modifier) {
    ScreenScaffold(title = "Dev", overline = "Debug build only", corner = null, modifier = modifier) {
        Spacer(modifier = Modifier.height(4.dp))
        DevPanel()
    }
}
