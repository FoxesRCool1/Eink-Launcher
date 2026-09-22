package io.github.foxesrcool1.margin.ui.devicetest

import android.content.Context
import android.view.View

/**
 * The release build has no Jetpack Ink baseline. The debug build has the real
 * one under the same name.
 */
object InkBaseline {

    @Suppress("UNUSED_PARAMETER")
    fun create(context: Context): View? = null

    @Suppress("UNUSED_PARAMETER")
    fun clear(view: View?) = Unit
}
