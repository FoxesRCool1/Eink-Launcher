package io.github.foxesrcool1.margin.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.BuildConfig
import io.github.foxesrcool1.margin.design.EinkColors
import io.github.foxesrcool1.margin.design.EinkType
import io.github.foxesrcool1.margin.design.components.EinkText
import io.github.foxesrcool1.margin.design.components.HairlineDivider
import io.github.foxesrcool1.margin.design.components.PagedList

/** A name, four lines of detail, and the rule. */
private val CreditRowHeight = 132.dp

/** One line of the credits: what it is, and under which licence it is used. */
data class Credit(val name: String, val detail: String)

/**
 * Who and what this app stands on. Kept next to `ASSETS.md` and
 * `licenses/DEPENDENCIES.md`: a new dependency or asset gets a line in all
 * three, in the same change.
 */
object Credits {
    val all: List<Credit> = listOf(
        Credit("Margin", "Free and open source, Apache License 2.0. The source is on GitHub, FoxesRCool1/Margin-Eink-Launcher. Help pay for it at ko-fi.com/foxesrcool."),
        Credit("The idea", "\"Prose: The distraction-free, e-ink laptop that should exist\" by Micah Daigle, CC BY-SA 4.0. No image and no layout from it is used here."),
        Credit("ViWoods display and pen facts", "The public notes in jdkruzr/ViwoodsAppDev. Used as a reference only. No code was copied."),
        Credit("Readium Kotlin Toolkit", "The EPUB reader engine. BSD 3-Clause. Copyright Readium Foundation."),
        Credit("AndroidX, Jetpack Compose, Kotlin, kotlinx.coroutines", "Apache License 2.0. Google and JetBrains."),
        Credit("Guava, Okio, Timber, koi, JSpecify", "Apache License 2.0. They arrive with Readium."),
        Credit("jsoup", "MIT License. Jonathan Hedley. It arrives with Readium."),
        Credit("Bodoni Moda", "The large words. SIL Open Font License 1.1."),
        Credit("Jost", "The small capitals. SIL Open Font License 1.1."),
        Credit("Literata", "Reading and writing. SIL Open Font License 1.1."),
        Credit("Lucide", "Every icon, the plant drawings and the app icon. ISC License, Lucide Icons and Contributors. Some of the icons come from Feather, MIT License, Cole Bemis."),
    )
}

@Composable
fun AboutSection(modifier: Modifier = Modifier) {
    EinkText(
        text = "Version ${BuildConfig.VERSION_NAME}. It collects nothing. It goes online only when you check for updates.",
        style = EinkType.body,
    )
    Spacer(modifier = Modifier.height(10.dp))
    // Every credit gets the same room, and the room is enough for the longest
    // one in full: a credit that is cut off in the middle is not a credit.
    PagedList(items = Credits.all, pageSize = 2, rowHeight = CreditRowHeight, modifier = modifier) { _, credit ->
        EinkText(text = credit.name, style = EinkType.rowTitle, maxLines = 1)
        EinkText(text = credit.detail, style = EinkType.help, maxLines = 4, modifier = Modifier.padding(bottom = 6.dp))
        HairlineDivider(color = EinkColors.Faded)
    }
}
