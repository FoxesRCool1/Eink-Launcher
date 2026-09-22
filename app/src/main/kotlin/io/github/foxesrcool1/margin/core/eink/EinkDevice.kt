package io.github.foxesrcool1.margin.core.eink

import android.content.Context
import android.graphics.Rect

/**
 * How the panel repaints.
 *
 * The numbers are the ones the ViWoods firmware uses for `setPictureMode`.
 * They come from the public notes in `jdkruzr/ViwoodsAppDev`. Another vendor
 * will have other numbers, so nothing outside [ViwoodsEinkDevice] may read
 * [vendorCode].
 */
enum class RefreshMode(val vendorCode: Int, val label: String) {
    Auto(0, "Auto"),
    Mixed(1, "Mixed (DU)"),
    Browse(2, "Browse (A2)"),
    Reading(3, "Reading (GL16)"),
    Fast(4, "Fast (pen)"),
    Full(17, "Full refresh (GC)"),
    ;

    companion object {
        fun fromVendorCode(code: Int): RefreshMode? = entries.firstOrNull { it.vendorCode == code }
    }
}

/** What the device draws with while it paints pen strokes by itself. */
enum class PenTool { Pen, Eraser }

/**
 * The two ways to the fast pen that the public notes describe.
 *
 * [Writing] is "path A": the vendor library draws the strokes inside our
 * process. [AutoDraw] is "path B": the system server draws them into
 * rectangles that we hand over.
 */
enum class FastPenPath { Writing, AutoDraw }

/** One call into the device, and how it went. Every call makes one of these. */
data class EinkCallResult(
    val call: String,
    val ok: Boolean,
    val detail: String,
) {
    override fun toString(): String = "${if (ok) "OK  " else "FAIL"} $call: $detail"
}

/**
 * Everything the app may ask of an e-ink panel.
 *
 * Two rules hold for every implementation:
 *
 * 1. No call may throw. A device that does not know a call answers with a
 *    failed [EinkCallResult].
 * 2. Every call writes its result to the log file, because the log file is
 *    the only way the developer can see the tablet.
 */
interface EinkDevice {

    /** A short name for the log and for the settings screen. */
    val name: String

    /** True when this device has any vendor control at all. */
    val hasVendorControl: Boolean

    /** Facts about the panel, for the test screen and the log. */
    fun deviceInfo(): List<Pair<String, String>>

    fun refreshMode(): RefreshMode?

    fun setRefreshMode(mode: RefreshMode): EinkCallResult

    /**
     * Repaints the whole panel once to clear ghosting, then goes back to the
     * mode that was set before.
     */
    fun fullRefresh(): EinkCallResult

    /**
     * Turns on the fast pen. [drawRegion] is where the device may draw, in
     * screen pixels. [excluded] are parts of that region it must leave alone,
     * such as a toolbar.
     */
    fun startFastPen(
        context: Context,
        path: FastPenPath,
        drawRegion: Rect,
        excluded: List<Rect> = emptyList(),
    ): EinkCallResult

    fun stopFastPen(): EinkCallResult

    /** The path that is running now, or null. */
    val activeFastPen: FastPenPath?

    fun setPenTool(tool: PenTool): EinkCallResult

    /** The vendor width range. The notes give 1 to 3 as an example. */
    fun setPenWidthRange(min: Int, max: Int): EinkCallResult
}
