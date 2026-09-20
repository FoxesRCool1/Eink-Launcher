package io.github.foxesrcool1.einklauncher.design.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.unit.dp
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import io.github.foxesrcool1.einklauncher.support.captureTo
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

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
}
