package io.github.foxesrcool1.margin.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.margin.design.EinkTheme
import io.github.foxesrcool1.margin.support.captureTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalTestApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw480dp-w480dp-h640dp-port-xxhdpi")
class EinkTextFieldTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `typing reaches the caller and a value set from outside reaches the field`() {
        var text by mutableStateOf("")
        val heard = mutableListOf<String>()

        compose.setContent {
            EinkTheme {
                Column(modifier = Modifier.padding(24.dp)) {
                    EinkTextField(
                        value = text,
                        onValueChange = {
                            heard += it
                            text = it
                        },
                        modifier = Modifier.fillMaxWidth().height(120.dp).testTag("field"),
                    )
                }
            }
        }

        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performTextInput("Walden")
        compose.waitForIdle()
        assertEquals("Walden", text)

        // A note that finishes loading arrives from outside, not from the keyboard.
        heard.clear()
        text = "Loaded from the file"
        compose.waitForIdle()
        assertEquals("the field must not echo an outside change back", emptyList<String>(), heard)

        compose.onNodeWithTag("field").performTextInput(" and more")
        compose.waitForIdle()
        assertEquals("Loaded from the file and more", text)

        compose.onRoot().captureTo("text_field_with_static_cursor")
    }

    @Test
    fun `letters typed while the caller is still catching up are kept`() {
        var text by mutableStateOf("")
        // A slow caller: it takes each change a moment later, one at a time.
        val notTakenYet = ArrayDeque<String>()

        compose.setContent {
            EinkTheme {
                EinkTextField(
                    value = text,
                    onValueChange = { notTakenYet += it },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("field"),
                )
            }
        }
        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performTextInput("h")
        compose.onNodeWithTag("field").performTextInput("t")
        compose.waitForIdle()
        assertEquals(listOf("h", "ht"), notTakenYet.toList())

        // The caller takes "h" while the field already says "ht".
        text = notTakenYet.removeFirst()
        compose.waitForIdle()
        compose.onNodeWithTag("field").assertTextEquals("ht")

        text = notTakenYet.removeFirst()
        compose.onNodeWithTag("field").performTextInput("tp")
        compose.waitForIdle()
        compose.onNodeWithTag("field").assertTextEquals("http")
    }

    @Test
    fun `a value from outside still wins over what was typed`() {
        var text by mutableStateOf("")
        compose.setContent {
            EinkTheme {
                EinkTextField(
                    value = text,
                    onValueChange = { text = it },
                    modifier = Modifier.fillMaxWidth().height(120.dp).testTag("field"),
                )
            }
        }
        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performTextInput("draft")
        compose.waitForIdle()

        // The same text as an earlier state of the field, set from outside.
        text = ""
        compose.waitForIdle()
        compose.onNodeWithTag("field").assertTextEquals("")
    }

    @Test
    fun `enter in a field of one line submits, from the screen keyboard and from a real one`() {
        var submitted = 0

        compose.setContent {
            EinkTheme {
                Column(modifier = Modifier.padding(24.dp)) {
                    EinkTextField(
                        value = "",
                        onValueChange = {},
                        singleLine = true,
                        onSubmit = { submitted++ },
                        modifier = Modifier.fillMaxWidth().testTag("field"),
                    )
                }
            }
        }

        compose.onNodeWithTag("field").performClick()
        compose.onNodeWithTag("field").performImeAction()
        compose.waitForIdle()
        assertEquals(1, submitted)

        compose.onNodeWithTag("field").performKeyInput { pressKey(Key.Enter) }
        compose.waitForIdle()
        assertEquals(2, submitted)
    }
}

class ReportedValuesTest {

    @Test
    fun `a reported value is an echo once`() {
        val reported = ReportedValues()
        reported.record("h")
        assertEquals(true, reported.isEcho("h"))
        assertEquals(false, reported.isEcho("h"))
    }

    @Test
    fun `a caller that skips values does not leave the old ones behind`() {
        val reported = ReportedValues()
        listOf("h", "ht", "htt").forEach(reported::record)
        assertEquals(true, reported.isEcho("ht"))
        assertEquals(false, reported.isEcho("h"))
        assertEquals(true, reported.isEcho("htt"))
    }

    @Test
    fun `a value nobody typed is not an echo`() {
        val reported = ReportedValues()
        reported.record("h")
        assertEquals(false, reported.isEcho("Loaded from the file"))
    }

    @Test
    fun `the list does not grow for ever`() {
        val reported = ReportedValues(limit = 3)
        (1..10).forEach { reported.record("v$it") }
        assertEquals(false, reported.isEcho("v7"))
        assertEquals(true, reported.isEcho("v8"))
    }
}
