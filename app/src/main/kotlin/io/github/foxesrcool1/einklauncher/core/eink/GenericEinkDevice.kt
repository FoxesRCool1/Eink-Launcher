package io.github.foxesrcool1.einklauncher.core.eink

import android.content.Context
import android.graphics.Rect
import io.github.foxesrcool1.einklauncher.core.log.AppLog

private const val TAG = "GenericEinkDevice"

/**
 * A device with no vendor control: a phone, an emulator, or an e-ink tablet
 * from a vendor this app does not know yet.
 *
 * Every call answers "not supported" and does nothing, so the rest of the app
 * never has to ask which device it runs on.
 */
class GenericEinkDevice : EinkDevice {

    override val name: String = "Generic"

    override val hasVendorControl: Boolean = false

    override val activeFastPen: FastPenPath? = null

    override fun deviceInfo(): List<Pair<String, String>> = listOf(
        "Device layer" to name,
        "Manufacturer" to android.os.Build.MANUFACTURER.orEmpty(),
        "Model" to android.os.Build.MODEL.orEmpty(),
    )

    override fun refreshMode(): RefreshMode? = null

    override fun setRefreshMode(mode: RefreshMode): EinkCallResult =
        unsupported("setRefreshMode(${mode.name})")

    override fun fullRefresh(): EinkCallResult = unsupported("fullRefresh")

    override fun startFastPen(
        context: Context,
        path: FastPenPath,
        drawRegion: Rect,
        excluded: List<Rect>,
    ): EinkCallResult = unsupported("startFastPen(${path.name})")

    override fun stopFastPen(): EinkCallResult = unsupported("stopFastPen")

    override fun setPenTool(tool: PenTool): EinkCallResult = unsupported("setPenTool(${tool.name})")

    override fun setPenWidthRange(min: Int, max: Int): EinkCallResult =
        unsupported("setPenWidthRange($min, $max)")

    private fun unsupported(call: String): EinkCallResult {
        val result = EinkCallResult(call, ok = false, detail = "not supported on this device")
        AppLog.d(TAG, result.toString())
        return result
    }
}
