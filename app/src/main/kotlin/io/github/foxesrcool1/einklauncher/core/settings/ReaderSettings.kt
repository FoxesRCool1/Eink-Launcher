package io.github.foxesrcool1.einklauncher.core.settings

/**
 * What the user can change in the reader. Plain numbers, so the rules for
 * them can be tested without a reader engine.
 */
data class ReaderSettings(
    val font: String = FONT_LITERATA,
    /** 100 is the size the book asks for. */
    val fontSizePercent: Int = 100,
    /** 100 is the normal page margin. */
    val marginPercent: Int = 100,
    /** 140 means a line is 1.4 times as tall as its letters. */
    val lineHeightPercent: Int = 140,
    val justify: Boolean = true,
    /** Underline is pure black. The other choice is a grey block, which may dither on the panel. */
    val underlineHighlights: Boolean = true,
    /** A full refresh every this many page turns. Zero is never. */
    val refreshEveryPages: Int = 0,
    /** The daily reading goal. Zero is no goal. */
    val goalMinutes: Int = 30,
) {
    fun clamped(): ReaderSettings = copy(
        font = font.takeIf { it in FONTS } ?: FONT_LITERATA,
        fontSizePercent = fontSizePercent.coerceIn(60, 300),
        marginPercent = marginPercent.coerceIn(0, 300),
        lineHeightPercent = lineHeightPercent.coerceIn(100, 220),
        refreshEveryPages = refreshEveryPages.coerceIn(0, 100),
        goalMinutes = goalMinutes.coerceIn(0, 600),
    )

    fun larger(): ReaderSettings = copy(fontSizePercent = fontSizePercent + 10).clamped()

    fun smaller(): ReaderSettings = copy(fontSizePercent = fontSizePercent - 10).clamped()

    fun widerMargins(): ReaderSettings = copy(marginPercent = marginPercent + 25).clamped()

    fun narrowerMargins(): ReaderSettings = copy(marginPercent = marginPercent - 25).clamped()

    fun looserLines(): ReaderSettings = copy(lineHeightPercent = lineHeightPercent + 10).clamped()

    fun tighterLines(): ReaderSettings = copy(lineHeightPercent = lineHeightPercent - 10).clamped()

    fun nextFont(): ReaderSettings = copy(font = FONTS[(FONTS.indexOf(font) + 1).mod(FONTS.size)])

    fun nextRefresh(): ReaderSettings =
        copy(refreshEveryPages = REFRESH_STEPS[(REFRESH_STEPS.indexOf(refreshEveryPages) + 1).mod(REFRESH_STEPS.size)])

    fun nextGoal(): ReaderSettings =
        copy(goalMinutes = GOAL_STEPS[(GOAL_STEPS.indexOf(goalMinutes) + 1).mod(GOAL_STEPS.size)])

    val fontLabel: String
        get() = when (font) {
            FONT_SERIF -> "Serif"
            FONT_SANS -> "Sans"
            FONT_PUBLISHER -> "The book's own"
            else -> "Literata"
        }

    companion object {
        const val FONT_LITERATA = "literata"
        const val FONT_SERIF = "serif"
        const val FONT_SANS = "sans"
        const val FONT_PUBLISHER = "publisher"

        val FONTS = listOf(FONT_LITERATA, FONT_SERIF, FONT_SANS, FONT_PUBLISHER)
        val REFRESH_STEPS = listOf(0, 5, 10, 20)
        val GOAL_STEPS = listOf(0, 15, 30, 45, 60, 90)
    }
}
