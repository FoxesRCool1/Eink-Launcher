package io.github.foxesrcool1.einklauncher.core.eink

import android.content.Context
import io.github.foxesrcool1.einklauncher.BuildConfig
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import java.io.File

private const val TAG = "EinkDevices"

/**
 * Hands out the one [EinkDevice] of this process.
 *
 * The `viwoods` flavour looks for the vendor class and falls back to
 * [GenericEinkDevice] when it is not there, so the same APK still runs on a
 * phone. The `generic` flavour never looks at all: it must hold no hidden API
 * call, because it is the one that may go to a store.
 */
object EinkDevices {

    @Volatile
    private var cached: EinkDevice? = null

    fun get(context: Context): EinkDevice {
        cached?.let { return it }
        return synchronized(this) {
            cached ?: create(context.applicationContext).also { cached = it }
        }
    }

    fun guard(context: Context): FastPenGuard =
        FastPenGuard(File(context.applicationContext.filesDir, "eink"))

    /** Call once from the application class, before anything touches the pen. */
    fun settleAfterStart(context: Context) {
        val crashed = guard(context).settleAfterStart()
        crashed.forEach {
            AppLog.e(TAG, "Fast pen path ${it.name} killed the app last time. It is now switched off.")
        }
    }

    private fun create(context: Context): EinkDevice {
        if (BuildConfig.FLAVOR != "viwoods") {
            AppLog.i(TAG, "Flavour ${BuildConfig.FLAVOR}: generic device layer")
            return GenericEinkDevice()
        }
        val viwoods = ViwoodsEinkDevice(guard = guard(context))
        return if (viwoods.hasVendorControl) {
            AppLog.i(TAG, "ViWoods vendor class found")
            viwoods
        } else {
            AppLog.i(TAG, "No ViWoods vendor class on this device: generic device layer")
            GenericEinkDevice()
        }
    }
}
