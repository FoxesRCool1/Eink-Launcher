package io.github.foxesrcool1.einklauncher.core.update

import android.content.Context
import androidx.core.content.edit

/**
 * The GitHub access token, for the case where the repository is private.
 *
 * A public repository needs none, and then this stays empty. The token is a
 * secret, so it does not live with the other settings: it has a file of its
 * own, `update_private.xml`, and both backup rule files leave that file out.
 * It is never written to the log.
 */
object UpdateToken {

    private const val FILE = "update_private"
    private const val KEY_TOKEN = "github_token"
    private const val KEY_LAST_VERSION = "last_version_code"

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun read(context: Context): String? =
        prefs(context).getString(KEY_TOKEN, null)?.takeIf { it.isNotBlank() }

    fun save(context: Context, token: String) {
        // A token has no spaces in it. A pasted one often has one at the end.
        val clean = token.filterNot { it.isWhitespace() }
        prefs(context).edit {
            if (clean.isEmpty()) remove(KEY_TOKEN) else putString(KEY_TOKEN, clean)
        }
    }

    fun lastVersionCode(context: Context): Int = prefs(context).getInt(KEY_LAST_VERSION, 0)

    fun setLastVersionCode(context: Context, code: Int) {
        prefs(context).edit { putInt(KEY_LAST_VERSION, code) }
    }
}
