package io.github.foxesrcool1.einklauncher.ui.common

import android.content.Context
import android.content.Intent
import android.net.Uri
import io.github.foxesrcool1.einklauncher.core.log.AppLog

private const val TAG = "WebLinks"

/**
 * The two web addresses the app can hand to the browser.
 *
 * Handing an address to the browser is not the app going online: the app
 * itself still goes online in one place only, `core/update/`. Decision 0020.
 */
object WebLinks {

    /** Where a person can help pay for the work. The app never asks. */
    const val KO_FI = "https://ko-fi.com/foxesrcool"

    const val SOURCE = "https://github.com/FoxesRCool1/Eink-Launcher"

    /** Opens [url] in the browser of the tablet. False when no app could take it. */
    fun open(context: Context, url: String): Boolean = runCatching {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        AppLog.i(TAG, "Handed $url to the browser")
        true
    }.getOrElse {
        AppLog.w(TAG, "No browser could open $url", it)
        false
    }
}
