package io.github.foxesrcool1.einklauncher.ui.ink

import android.app.Activity
import android.graphics.Rect
import android.view.View
import io.github.foxesrcool1.einklauncher.core.eink.EinkDevice
import io.github.foxesrcool1.einklauncher.core.eink.EinkDevices
import io.github.foxesrcool1.einklauncher.core.eink.FastPenPath
import io.github.foxesrcool1.einklauncher.core.eink.PenTool
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore

private const val TAG = "FastPenSession"

/**
 * Ties an [InkCanvasView] to the fast pen of the tablet for as long as one
 * screen is open.
 *
 * The screen calls [start] once the canvas has a size, [stop] when it leaves,
 * and [pause] around anything that must not be drawn on, such as a dialog.
 * When the fast pen cannot start, the canvas simply paints for itself, so the
 * screen never has to know which case it is in.
 */
class FastPenSession(
    private val activity: Activity,
    private val canvas: InkCanvasView,
    private val mode: String,
    private val redrawDelayMillis: Long,
) {
    private val device: EinkDevice = EinkDevices.get(activity)
    private var running = false

    private val path: FastPenPath? = when (mode) {
        SettingsStore.FAST_PEN_WRITING -> FastPenPath.Writing
        SettingsStore.FAST_PEN_AUTODRAW -> FastPenPath.AutoDraw
        else -> null
    }

    /** [keepOut] are views the tablet must not draw on: toolbars and buttons. */
    fun start(keepOut: List<View> = emptyList()) {
        val wanted = path ?: return
        if (running || canvas.width == 0 || !device.hasVendorControl) return

        val result = device.startFastPen(activity, wanted, screenBox(canvas), keepOut.map(::screenBox))
        running = result.ok
        canvas.deviceDrawsLive = result.ok
        canvas.redrawDelayMillis = redrawDelayMillis
        if (result.ok) {
            applyWidth(canvas.penWidth)
            canvas.onErasingChanged = { erasing ->
                device.setPenTool(if (erasing) PenTool.Eraser else PenTool.Pen)
            }
        } else {
            AppLog.w(TAG, "Fast pen did not start, this app paints instead: ${result.detail}")
        }
    }

    fun applyWidth(penWidth: Float) {
        if (!running) return
        val (low, high) = PenWidths.vendorRange(penWidth)
        device.setPenWidthRange(low, high)
    }

    /** Stops the tablet from drawing, and pays what the canvas owes right away. */
    fun stop() {
        if (!running) return
        running = false
        device.stopFastPen()
        canvas.deviceDrawsLive = false
        canvas.onErasingChanged = null
        canvas.settle()
    }

    val isRunning: Boolean get() = running

    private fun screenBox(view: View): Rect {
        val corner = IntArray(2)
        view.getLocationOnScreen(corner)
        return Rect(corner[0], corner[1], corner[0] + view.width, corner[1] + view.height)
    }
}
