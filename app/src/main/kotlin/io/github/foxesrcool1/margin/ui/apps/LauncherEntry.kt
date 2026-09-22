package io.github.foxesrcool1.margin.ui.apps

/** One app the user can start. */
data class LauncherEntry(
    val packageName: String,
    val className: String,
    val label: String,
) {
    /** The stable id used for pinning. */
    val key: String get() = "$packageName/$className"

    companion object {
        fun keyOf(packageName: String, className: String): String = "$packageName/$className"
    }
}

/**
 * A way out of this launcher.
 *
 * Plan section 3.2: never trap the user. The Apps tab always shows these, even
 * when the app list fails to load.
 */
data class EscapeEntry(
    val label: String,
    val hint: String,
    val kind: EscapeKind,
    val packageName: String? = null,
    val className: String? = null,
)

enum class EscapeKind {
    /** Another home app, so the user can get back to the stock launcher. */
    OtherHome,

    /** The ViWoods settings app: front light, refresh mode, Wi-Fi. */
    DeviceSettings,

    /** The standard Android settings app, if this build has one. */
    AndroidSettings,
}
