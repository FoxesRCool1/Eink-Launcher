package io.github.foxesrcool1.einklauncher.support

import androidx.compose.ui.test.SemanticsNodeInteraction
import com.github.takahirom.roborazzi.captureRoboImage
import java.io.File

/**
 * Where the screenshot tests write.
 *
 * `app/build.gradle.kts` passes the folder in as an absolute path. A relative
 * path would depend on the working directory of the test JVM, which is not
 * something a test should have to know.
 */
object Screenshots {

    private val directory: File =
        File(System.getProperty("eink.screenshot.dir") ?: "build/outputs/roborazzi")

    fun pathFor(name: String): String = File(directory, "$name.png").absolutePath
}

/** Writes this node to `<name>.png` in the screenshot folder. */
fun SemanticsNodeInteraction.captureTo(name: String) {
    captureRoboImage(Screenshots.pathFor(name))
}
