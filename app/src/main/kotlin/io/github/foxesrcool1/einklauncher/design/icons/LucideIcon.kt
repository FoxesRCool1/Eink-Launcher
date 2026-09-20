package io.github.foxesrcool1.einklauncher.design.icons

import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.vector.PathParser

/**
 * One Lucide icon: its name, and its shapes as SVG path data in a 24 by 24 box.
 *
 * The app holds the path data and draws it itself, see
 * [io.github.foxesrcool1.einklauncher.design.components.EinkIcon]. That keeps
 * an icon library out of the APK, and it lets the line stay 2 dp wide at any
 * size. A vector drawable would scale the line with the icon, and a large
 * icon would get a heavy line, which is not the calm look this design wants.
 *
 * Every icon here is line art. [fills] holds the few shapes that are filled
 * as well as drawn, such as a solid dot.
 */
class LucideIcon(
    val name: String,
    val strokes: List<String>,
    val fills: List<String> = emptyList(),
) {
    /** Parsed on first use and then kept: an icon is drawn many times and never changes. */
    val strokePaths: List<Path> by lazy { strokes.map(::parse) }

    val fillPaths: List<Path> by lazy { fills.map(::parse) }

    private fun parse(data: String): Path = PathParser().parsePathString(data).toPath()

    companion object {
        /** The side of the box every Lucide icon is drawn in. */
        const val VIEWPORT = 24f
    }
}
