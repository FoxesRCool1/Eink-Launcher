package io.github.foxesrcool1.margin.core.update

/**
 * A version of the app, like 0.1.2.
 *
 * It is read from a release tag ("v0.1.2") and from the version name of the
 * running build ("0.1.2-viwoods"). Anything after the three numbers is
 * ignored, so the flavour suffix does no harm.
 *
 * [code] is the Android version code. `app/build.gradle.kts` works it out
 * with the same sum, so a newer version always has a larger code and Android
 * accepts it as an update.
 */
data class AppVersion(val major: Int, val minor: Int, val patch: Int) : Comparable<AppVersion> {

    val code: Int get() = major * 10_000 + minor * 100 + patch

    override fun compareTo(other: AppVersion): Int = code.compareTo(other.code)

    override fun toString(): String = "$major.$minor.$patch"

    companion object {
        private val PATTERN = Regex("""^v?(\d{1,4})\.(\d{1,2})\.(\d{1,2})(?:[-+].*)?$""")

        /** Null when the text is not a version. Minor and patch stop at 99, as the code sum needs. */
        fun parse(text: String): AppVersion? {
            val match = PATTERN.matchEntire(text.trim()) ?: return null
            val (major, minor, patch) = match.destructured
            return AppVersion(major.toInt(), minor.toInt(), patch.toInt())
        }
    }
}
