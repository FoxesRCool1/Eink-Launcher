package io.github.foxesrcool1.einklauncher.ui.split

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import io.github.foxesrcool1.einklauncher.core.eink.EinkDevices
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.design.EinkColors
import io.github.foxesrcool1.einklauncher.design.EinkDimens
import io.github.foxesrcool1.einklauncher.design.components.IconPressButton
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import kotlinx.coroutines.flow.first
import android.os.SystemClock
import androidx.compose.runtime.withFrameNanos
import io.github.foxesrcool1.einklauncher.core.speed.SpeedWatch

/**
 * The split screen of the activity around the main half, for the button that
 * opens it. Null where the screen has none, such as the device test.
 */
val LocalSplit = staticCompositionLocalOf<SplitState?> { null }

/**
 * What a page inside the second half knows about it. A data class, so an
 * equal one does not make every page in the half compose again.
 */
data class PaneContext(val split: SplitState, val sideBySide: Boolean)

/**
 * Set only inside the second half. A page reads it to put the controls of the
 * split screen where the icon that turns the screen would be: one screen needs
 * one of those, not two.
 */
val LocalPane = staticCompositionLocalOf<PaneContext?> { null }

/** Side by side when the tablet is on its side, one above the other when it is upright. */
@Composable
fun halvesSideBySide(): Boolean {
    val configuration = LocalConfiguration.current
    return configuration.screenWidthDp > configuration.screenHeightDp
}

/**
 * The two halves of a screen.
 *
 * [main] is the screen itself. It keeps its place in the composition whether
 * the split is open or not, and whichever side it is on, so opening, closing
 * and swapping never lose what it holds: the page of the book, the half
 * written sentence. Only where it is placed changes. [pane] is not composed
 * at all while the split is closed, so a screen that is never split pays
 * nothing for it.
 *
 * The system bars are padded here, once, for both halves. Inside, each page
 * pads them again as if it had the whole screen, and that second padding
 * finds nothing left, which is what keeps the lower half from leaving room
 * for a status bar it does not touch.
 */
@Composable
fun SplitLayout(
    split: SplitState?,
    modifier: Modifier = Modifier,
    main: @Composable () -> Unit,
    pane: @Composable () -> Unit,
) {
    val open = split?.isOpen == true
    val swapped = split?.swapped == true
    val sideBySide = halvesSideBySide()

    if (split != null) RefreshOnSplitChange(split)

    CompositionLocalProvider(LocalSplit provides split) {
        Layout(
            modifier = modifier.systemBarsPadding(),
            contents = listOf(
                main,
                { if (open) Box(modifier = Modifier.background(EinkColors.Ink)) },
                {
                    if (open && split != null) {
                        CompositionLocalProvider(
                            LocalSplit provides null,
                            LocalPane provides PaneContext(split, sideBySide),
                        ) { pane() }
                    }
                },
            ),
        ) { (mains, lines, panes), constraints ->
            val width = constraints.maxWidth
            val height = constraints.maxHeight
            if (!open) {
                val placed = mains.map { it.measure(Constraints.fixed(width, height)) }
                return@Layout layout(width, height) { placed.forEach { it.place(0, 0) } }
            }
            val line = EinkDimens.rule.roundToPx()
            val along = if (sideBySide) width else height
            val first = (along - line) / 2
            val second = along - line - first
            fun box(length: Int) = if (sideBySide) Constraints.fixed(length, height) else Constraints.fixed(width, length)

            val mainLength = if (swapped) second else first
            val paneLength = if (swapped) first else second
            val mainPlaced = mains.map { it.measure(box(mainLength)) }
            val linePlaced = lines.map { it.measure(box(line)) }
            val panePlaced = panes.map { it.measure(box(paneLength)) }

            layout(width, height) {
                val mainAt = if (swapped) paneLength + line else 0
                val paneAt = if (swapped) 0 else mainLength + line
                val lineAt = if (swapped) paneLength else mainLength
                fun place(list: List<androidx.compose.ui.layout.Placeable>, at: Int) = list.forEach {
                    if (sideBySide) it.place(at, 0) else it.place(0, at)
                }
                place(mainPlaced, mainAt)
                place(linePlaced, lineAt)
                place(panePlaced, paneAt)
            }
        }
    }
}

/**
 * The button that opens and closes the split screen. It is there on every
 * screen that has a split screen, beside the icon that turns the screen, and
 * nowhere else.
 */
@Composable
fun SplitButton() {
    val split = LocalSplit.current ?: return
    IconPressButton(
        icon = if (halvesSideBySide()) Lucide.SquareSplitHorizontal else Lucide.SquareSplitVertical,
        // Not black while the split is open: the second half itself says
        // that, and a black circle is a lot of ink to change on e-ink.
        label = if (split.isOpen) "Close the split screen" else "Split screen",
        onClick = split::toggle,
    )
}

/**
 * The two controls of the second half: swap the halves, and close the split.
 * Every page shows them at the end of its top line when it stands in the
 * second half, and nothing when it does not.
 */
@Composable
fun PaneControls() {
    val pane = LocalPane.current ?: return
    IconPressButton(
        icon = if (pane.sideBySide) Lucide.ArrowLeftRight else Lucide.ArrowUpDown,
        label = "Swap the two halves",
        onClick = pane.split::swap,
    )
    IconPressButton(icon = Lucide.X, label = "Close the split screen", onClick = pane.split::close)
}

/**
 * The second half on its own, for a screen built from views and not from
 * Compose: the EPUB reader. [SplitViewLayout] places it, and this gives the
 * page inside what [SplitLayout] would give it.
 */
@Composable
fun DetachedPane(split: SplitState, host: PaneHost) {
    RefreshOnSplitChange(split)
    if (!split.isOpen) return
    CompositionLocalProvider(LocalPane provides PaneContext(split, halvesSideBySide())) {
        // The book beside it does not leave room for the system bars either.
        Box(modifier = Modifier.consumeWindowInsets(WindowInsets.systemBars)) {
            SplitPane(split, host)
        }
    }
}

/**
 * Cleans the whole panel after the split screen opened, closed, swapped or
 * changed page, when the user asked for that in Settings. E-ink rule 8. It
 * also times the change.
 */
@Composable
internal fun RefreshOnSplitChange(split: SplitState) {
    val context = LocalContext.current
    val key = Triple(split.isOpen, split.swapped, if (split.isOpen) split.page else null)
    var seen by remember { mutableStateOf(key) }
    LaunchedEffect(key) {
        if (key == seen) return@LaunchedEffect
        seen = key
        // The change is on the glass one frame from now. Its time counts
        // against the same budget as a change of tab.
        val asked = split.changedAt
        if (asked > 0L) {
            withFrameNanos { }
            SpeedWatch.check("A change of the split screen", SystemClock.uptimeMillis() - asked, SpeedWatch.Budget.SCREEN_CHANGE)
        }
        if (split.changedByMain) return@LaunchedEffect
        if (SettingsStore(context).fullRefreshOnBigChange.first()) EinkDevices.get(context).fullRefresh()
    }
}
