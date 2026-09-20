package io.github.foxesrcool1.einklauncher.core.settings

/**
 * How every window of the app is set up.
 *
 * [landscapeFlipped] turns the landscape screen the other way round. The
 * tablet has keys on one edge, and only the person holding it knows which
 * hand they should end up under.
 */
data class WindowSettings(
    val landscape: Boolean = false,
    val landscapeFlipped: Boolean = false,
    val statusBarHidden: Boolean = true,
)
