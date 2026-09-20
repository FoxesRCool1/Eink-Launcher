package io.github.foxesrcool1.einklauncher.ui.home

/**
 * Where the launcher is right now.
 *
 * There is no navigation library. A launcher has a handful of screens, and a
 * `when` on this value starts faster, holds no back stack the Home key could
 * fight with, and cannot play a transition.
 */
enum class LauncherRoute(val title: String) {
    Home("Home"),
    Reading("Read"),
    Writing("Write"),
    Journal("Journal"),
    Apps("Apps"),
    Settings("Settings"),
    Log("Log"),
    Update("Updates"),
    ;

    /** The four tabs on Home, in the order they are shown. */
    companion object {
        val tabs: List<LauncherRoute> = listOf(Reading, Writing, Journal, Apps)
    }
}
