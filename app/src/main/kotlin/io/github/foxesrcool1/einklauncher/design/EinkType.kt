package io.github.foxesrcool1.einklauncher.design

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import io.github.foxesrcool1.einklauncher.R

/**
 * The type scale.
 *
 * All three families are variable fonts under the SIL Open Font Licence. We
 * load the default instance of each file and use one weight per family. Weight
 * is never used for emphasis: size, space and small capitals do that work. Thin
 * hairlines break up on a 292 PPI e-ink panel, so a heavier or lighter cut is
 * not a safe tool here. See docs/decisions/0003-typography.md.
 */
object EinkType {

    /** Bodoni Moda. Large words only. */
    val Display: FontFamily = FontFamily(Font(R.font.bodoni_moda))

    /** Jost. Capitals with wide tracking, for labels. */
    val Label: FontFamily = FontFamily(Font(R.font.jost))

    /** Literata. Reading and writing body text. */
    val Body: FontFamily = FontFamily(Font(R.font.literata))

    /** The four big words on Today. */
    val word = TextStyle(
        fontFamily = Display,
        fontSize = 52.sp,
        lineHeight = 68.sp,
        color = EinkColors.Ink,
    )

    /** A screen title. */
    val title = TextStyle(
        fontFamily = Display,
        fontSize = 34.sp,
        lineHeight = 44.sp,
        color = EinkColors.Ink,
    )

    /** A row title inside a list. */
    val rowTitle = TextStyle(
        fontFamily = Body,
        fontSize = 20.sp,
        lineHeight = 28.sp,
        color = EinkColors.Ink,
    )

    /** Body text in the app. */
    val body = TextStyle(
        fontFamily = Body,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        color = EinkColors.Ink,
    )

    /** Body text inside the reader. */
    val reading = TextStyle(
        fontFamily = Body,
        fontSize = 19.sp,
        lineHeight = 31.sp,
        color = EinkColors.Ink,
    )

    /** Small capitals with wide tracking. Labels, status lines, controls. */
    val caps = TextStyle(
        fontFamily = Label,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.18.em,
        color = EinkColors.Ink,
    )

    /** The same, one step down, for a secondary line. */
    val capsSmall = TextStyle(
        fontFamily = Label,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.20.em,
        color = EinkColors.Ink,
    )

    /** Text inside a control. */
    val button = TextStyle(
        fontFamily = Label,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.16.em,
        textAlign = TextAlign.Center,
        color = EinkColors.Ink,
    )
}
