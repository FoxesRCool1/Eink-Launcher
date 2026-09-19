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
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.support.captureTo
import io.github.foxesrcool1.einklauncher.design.components.BotanicalCorner
import io.github.foxesrcool1.einklauncher.design.components.BotanicalSprig
import io.github.foxesrcool1.einklauncher.design.components.CapsLabel
import io.github.foxesrcool1.einklauncher.design.components.ConfirmDialogContent
import io.github.foxesrcool1.einklauncher.design.components.EinkText
import io.github.foxesrcool1.einklauncher.design.components.HairlineDivider
import io.github.foxesrcool1.einklauncher.design.components.InvertPressButton
import io.github.foxesrcool1.einklauncher.design.components.PagedList
import io.github.foxesrcool1.einklauncher.design.components.WordMenu
import io.github.foxesrcool1.einklauncher.design.components.WordMenuItem
import io.github.foxesrcool1.einklauncher.ui.demo.DesignDemoScreen
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

    @Test
    fun wordMenu() {
        compose.setContent {
            EinkTheme {
                Box(modifier = Modifier.fillMaxSize().padding(EinkDimens.screenMargin)) {
                    WordMenu(
                        items = listOf(
                            WordMenuItem("Read"),
                            WordMenuItem("Write"),
                            WordMenuItem("Journal"),
                            WordMenuItem("Apps"),
                        ),
                        selectedIndex = 0,
                        onSelect = {},
                    )
                }
            }
        }
        capture("word_menu")
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
        compose.onNodeWithText("NEXT").performClick()
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
    fun botanicalCorners() {
        compose.setContent {
            EinkTheme {
                Box(modifier = Modifier.fillMaxSize()) {
                    BotanicalSprig(
                        corner = BotanicalCorner.TopStart,
                        modifier = Modifier.align(Alignment.TopStart),
                    )
                    BotanicalSprig(
                        corner = BotanicalCorner.TopEnd,
                        modifier = Modifier.align(Alignment.TopEnd),
                    )
                    BotanicalSprig(
                        corner = BotanicalCorner.BottomStart,
                        modifier = Modifier.align(Alignment.BottomStart),
                    )
                    BotanicalSprig(
                        corner = BotanicalCorner.BottomEnd,
                        modifier = Modifier.align(Alignment.BottomEnd),
                    )
                }
            }
        }
        capture("botanical_corners")
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
            modifier = Modifier.fillMaxSize(),
        ) { index, row ->
            Column(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                EinkText(text = row, style = EinkType.rowTitle, maxLines = 1)
                CapsLabel(text = "Index $index", style = EinkType.capsSmall)
            }
        }
    }
}
