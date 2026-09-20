package io.github.foxesrcool1.einklauncher.design

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.support.captureTo
import io.github.foxesrcool1.einklauncher.design.components.EinkIcon
import io.github.foxesrcool1.einklauncher.design.components.IconPressButton
import io.github.foxesrcool1.einklauncher.design.components.PlantArt
import io.github.foxesrcool1.einklauncher.design.components.Plants
import io.github.foxesrcool1.einklauncher.design.icons.Lucide
import io.github.foxesrcool1.einklauncher.design.icons.LucideIcon
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialogContent
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.ui.demo.DesignDemoScreen
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Screenshot tests for the design system.
 *
 * The screen size matches the ViWoods AiPaper Mini: 480 dp by 640 dp at
 * xxhdpi, which is 1440 by 1920 pixels. The palette is pure black and pure
 * white, so the PNG holds no colour and reads like the panel.
 *
 * Run `./gradlew recordRoborazziViwoodsDebug` to write the files into
 * `app/build/outputs/roborazzi/`.
 */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw480dp-w480dp-h640dp-port-xxhdpi")
class DesignSystemScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    private fun capture(name: String) {
        compose.onRoot().captureTo(name)
    }

    /** Every Lucide icon in the app, so a broken path shows up as a hole in the picture. */
    @Test
    fun everyIcon() {
        val icons = Lucide::class.java.declaredMethods
            .filter { it.returnType == LucideIcon::class.java }
            .sortedBy { it.name }
            .map { it.invoke(Lucide) as LucideIcon }
        assertTrue("the icon sheet is empty", icons.size > 50)

        compose.setContent {
            EinkTheme {
                Column(modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin)) {
                    icons.chunked(8).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
                            line.forEach { icon -> EinkIcon(icon = icon, size = 40.dp) }
                        }
                        Spacer(modifier = Modifier.height(EinkDimens.targetGap))
                    }
                }
            }
        }
        capture("lucide_icons")
    }

    @Test
    fun iconButtons() {
        compose.setContent {
            EinkTheme {
                Row(
                    modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin),
                    horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap),
                ) {
                    IconPressButton(icon = Lucide.PenLine, label = "Pen", onClick = {})
                    IconPressButton(icon = Lucide.Eraser, label = "Eraser", selected = true, onClick = {})
                    IconPressButton(icon = Lucide.Plus, label = "Add", bordered = true, onClick = {})
                    IconPressButton(icon = Lucide.Trash2, label = "Delete", enabled = false, onClick = {})
                    IconPressButton(icon = Lucide.RotateCwSquare, label = "Turn", quiet = true, onClick = {})
                }
            }
        }
        capture("icon_buttons")
    }

    @Test
    fun capsLabelAndDividers() {
        compose.setContent {
            EinkTheme {
                Column(
                    modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin),
                ) {
                    CapsLabel(text = "Saturday 19 September")
                    Spacer(modifier = Modifier.height(12.dp))
                    HairlineDivider(thickness = EinkDimens.hairline)
                    Spacer(modifier = Modifier.height(12.dp))
                    HairlineDivider(thickness = EinkDimens.rule)
                    Spacer(modifier = Modifier.height(24.dp))
                    EinkText(text = "Body text in Literata at the reading size.")
                    Spacer(modifier = Modifier.height(12.dp))
                    EinkText(text = "A title in Bodoni Moda", style = EinkType.title)
                }
            }
        }
        capture("caps_label_and_dividers")
    }

    @Test
    fun buttonsAtRest() {
        compose.setContent { EinkTheme { ButtonRow() } }
        capture("buttons_at_rest")
    }

    @Test
    fun buttonPressedInverts() {
        compose.setContent { EinkTheme { ButtonRow() } }
        compose.onNodeWithText("PRESS ME").performTouchInput { down(center) }
        compose.waitForIdle()
        capture("buttons_pressed")
    }

    @Test
    fun pagedListFirstPage() {
        compose.setContent { EinkTheme { DemoPagedList() } }
        capture("paged_list_page_1")
    }

    @Test
    fun pagedListSecondPage() {
        compose.setContent { EinkTheme { DemoPagedList() } }
        compose.onNodeWithContentDescription("Next page").performClick()
        compose.waitForIdle()
        capture("paged_list_page_2")
    }

    @Test
    fun confirmDialog() {
        compose.setContent {
            EinkTheme {
                Box(
                    modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin),
                    contentAlignment = Alignment.Center,
                ) {
                    ConfirmDialogContent(
                        title = "Delete the note?",
                        message = "The file goes away and cannot come back.",
                        confirmText = "Delete",
                        cancelText = "Keep",
                        onConfirm = {},
                        onDismiss = {},
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
        capture("confirm_dialog")
    }

    @Test
    fun plants() {
        val all = listOf(
            Plants.Home, Plants.Reading, Plants.Writing, Plants.Journal, Plants.Apps, Plants.Settings, Plants.Other,
        )
        compose.setContent {
            EinkTheme {
                Column(modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin)) {
                    all.chunked(3).forEach { line ->
                        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.blockGap)) {
                            line.forEach { plant -> PlantArt(plant = plant) }
                        }
                        Spacer(modifier = Modifier.height(EinkDimens.blockGap))
                    }
                }
            }
        }
        capture("plants")
    }

    @Test
    fun designDemoScreen() {
        compose.setContent { EinkTheme { DesignDemoScreen() } }
        capture("design_demo_screen")
    }
}

@Composable
private fun ButtonRow() {
    Column(modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin)) {
        Row(horizontalArrangement = Arrangement.spacedBy(EinkDimens.targetGap)) {
            InvertPressButton(text = "Press me", onClick = {})
            InvertPressButton(text = "Delete", onClick = {})
            InvertPressButton(text = "Off", onClick = {}, enabled = false)
        }
    }
}

@Composable
private fun DemoPagedList() {
    Column(modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin)) {
        PagedList(
            items = (1..13).map { "Sample row $it" },
            pageSize = 6,
            rowHeight = EinkDimens.rowTwoLines,
            modifier = Modifier.fillMaxSize(),
        ) { index, row ->
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
                EinkText(text = row, style = EinkType.rowTitle, maxLines = 1)
                CapsLabel(text = "Index $index", style = EinkType.capsSmall)
            }
        }
    }
}
