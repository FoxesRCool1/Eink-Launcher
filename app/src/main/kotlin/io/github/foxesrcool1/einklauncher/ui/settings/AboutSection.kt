package io.github.foxesrcool1.einklauncher.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.BuildConfig
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkType
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.PagedList

/** One line of the credits: what it is, and under which licence it is used. */
data class Credit(val name: String, val detail: String)

/**
 * Who and what this app stands on. Kept next to `ASSETS.md` and
 * `licenses/DEPENDENCIES.md`: a new dependency or asset gets a line in all
 * three, in the same change.
 */
object Credits {
    val all: List<Credit> = listOf(
        Credit("Eink Launcher", "Apache License 2.0. The source is on GitHub, FoxesRCool1/Eink-Launcher."),
        Credit("The idea", "\"Prose: The distraction-free, e-ink laptop that should exist\" by Micah Daigle, CC BY-SA 4.0. No image and no layout from it is used here."),
        Credit("ViWoods display and pen facts", "The public notes in jdkruzr/ViwoodsAppDev. Used as a reference only. No code was copied."),
        Credit("Readium Kotlin Toolkit", "The EPUB reader engine. BSD 3-Clause. Copyright Readium Foundation."),
        Credit("AndroidX, Jetpack Compose, Kotlin, kotlinx.coroutines", "Apache License 2.0. Google and JetBrains."),
        Credit("Guava, Okio, Timber, koi, JSpecify", "Apache License 2.0. They arrive with Readium."),
        Credit("jsoup", "MIT License. Jonathan Hedley. It arrives with Readium."),
        Credit("Bodoni Moda", "The large words. SIL Open Font License 1.1."),
        Credit("Jost", "The small capitals. SIL Open Font License 1.1."),
        Credit("Literata", "Reading and writing. SIL Open Font License 1.1."),
        Credit("The corner drawing and the icon", "Original work for this app. Apache License 2.0."),
    )
}

@Composable
fun AboutSection(modifier: Modifier = Modifier) {
    CapsLabel(text = "About and licences", style = EinkType.capsSmall)
    HairlineDivider(color = EinkColors.Faded)
    Spacer(modifier = Modifier.height(10.dp))
    EinkText(
        text = "Version ${BuildConfig.VERSION_NAME}. It collects nothing. It goes online only when you check for updates.",
        style = EinkType.body,
    )
    Spacer(modifier = Modifier.height(10.dp))
    // One credit a page. Two do not fit under the tabs with their text in full,
    // and a credit that is cut off in the middle is not a credit.
    PagedList(items = Credits.all, pageSize = 1, modifier = modifier) { _, credit ->
        EinkText(text = credit.name, style = EinkType.rowTitle, maxLines = 1, modifier = Modifier.padding(top = 8.dp))
        EinkText(text = credit.detail, style = EinkType.body, maxLines = 5, modifier = Modifier.padding(bottom = 8.dp))
        HairlineDivider(color = EinkColors.Faded)
    }
}
