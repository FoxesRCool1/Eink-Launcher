package io.github.foxesrcool1.margin.design.components

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

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
 * A dialog is also a window of its own, and Android decides about the status
 * bar per window. Without the lines below, the bar the app hides would come
 * back with every dialog and go again after it: two repaints for nothing.
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
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.setWindowAnimations(0)
            window?.setDimAmount(0f)
            if (window != null && hostHidesStatusBar(view.context)) {
                runCatching {
                    val controller = WindowCompat.getInsetsController(window, window.decorView)
                    controller.systemBarsBehavior =
                        WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    controller.hide(WindowInsetsCompat.Type.statusBars())
                }
            }
        }
        content()
    }
}

/** True when the activity under this dialog shows no status bar. */
private fun hostHidesStatusBar(context: Context): Boolean {
    var current: Context? = context
    while (current is ContextWrapper && current !is Activity) current = current.baseContext
    val host = (current as? Activity)?.window?.decorView ?: return false
    val insets = ViewCompat.getRootWindowInsets(host) ?: return false
    return !insets.isVisible(WindowInsetsCompat.Type.statusBars())
}
