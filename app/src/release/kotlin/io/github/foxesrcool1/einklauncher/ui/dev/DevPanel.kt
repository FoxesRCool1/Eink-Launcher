package io.github.foxesrcool1.einklauncher.ui.dev

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Release builds have no dev panel. The debug source set holds the real one.
 */
@Composable
fun DevPanel(modifier: Modifier = Modifier) {
    // Nothing on purpose.
}

/** Release builds never show the dev entry. */
const val DEV_PANEL_AVAILABLE: Boolean = false
