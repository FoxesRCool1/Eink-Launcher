package io.github.foxesrcool1.margin.ui.ink

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import io.github.foxesrcool1.margin.core.ink.InkNote
import io.github.foxesrcool1.margin.core.ink.InkNotesRepository
import io.github.foxesrcool1.margin.core.ink.InkPageData
import io.github.foxesrcool1.margin.core.ink.InkTestData
import io.github.foxesrcool1.margin.core.ink.PageTemplate
import io.github.foxesrcool1.margin.core.storage.DataRepository
import io.github.foxesrcool1.margin.core.storage.LocalFileStore
import io.github.foxesrcool1.margin.design.EinkTheme
import io.github.foxesrcool1.margin.support.captureTo
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "sw480dp-w480dp-h640dp-port-xxhdpi")
class InkNoteScreenshotTest {

    @get:Rule
    val compose = createComposeRule()

    @get:Rule
    val temporary = TemporaryFolder()

    @Test
    fun inkNoteScreen() {
        val data = DataRepository(LocalFileStore(temporary.newFolder("Margin")))
        val note = InkNote(
            template = PageTemplate.Lined,
            pages = listOf(
                InkPageData(InkTestData.fullPage(240)),
                InkPageData(emptyList()),
            ),
        )
        val controller = InkNoteController(InkNotesRepository(data), "notes/test.inknote", note, mayWrite = true)

        compose.setContent {
            EinkTheme {
                InkNoteScreen(
                    title = "Ideas",
                    controller = controller,
                    problem = null,
                    onCanvas = { controller.attach(it) },
                    onDialog = {},
                    onWidth = {},
                    onExport = { "" },
                    onClose = {},
                )
            }
        }
        compose.onRoot().captureTo("ink_note_screen")
    }
}
