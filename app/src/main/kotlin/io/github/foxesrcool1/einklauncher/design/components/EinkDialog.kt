package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider

/**
 * A dialog window that obeys the e-ink rules.
 *
 * A platform dialog does two things on its own that a phone wants and this
 * panel does not: it fades in and out, and it lays a grey wash over the screen
 * behind it. The fade is an animation. The wash turns the whole panel into
 * dithered grey, which means a full repaint when it comes and another when it
 * goes. Both are turned off here, and again in the dialog theme in
 * `themes.xml`, because the theme is read before the window first shows.
 *
 * Every dialog in the app goes through this. Do not call `Dialog` directly.
 */
@Composable
fun EinkDialog(
    onDismissRequest: () -> Unit,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    content: @Composable () -> Unit,
) {
    Dialog(onDismissRequest = onDismissRequest, properties = properties) {
        val window = (LocalView.current.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setWindowAnimations(0)
            window?.setDimAmount(0f)
        }
        content()
    }
}
