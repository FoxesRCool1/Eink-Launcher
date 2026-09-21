package io.github.foxesrcool1.einklauncher.ui.split

import android.content.Context
import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import io.github.foxesrcool1.einklauncher.core.books.LibraryBook
import io.github.foxesrcool1.einklauncher.core.ink.PageTemplate
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.ui.apps.AppsRepository
import io.github.foxesrcool1.einklauncher.ui.apps.LauncherEntry
import io.github.foxesrcool1.einklauncher.ui.ink.InkNoteActivity
import io.github.foxesrcool1.einklauncher.ui.reading.epub.EpubReaderActivity
import io.github.foxesrcool1.einklauncher.ui.reading.pdf.PdfReaderActivity
import io.github.foxesrcool1.einklauncher.ui.writing.NoteEditorActivity

private const val TAG = "PageOpener"

/**
 * How a page opens a note, a book or an app.
 *
 * On a screen of its own, each opens a new screen, as it always did. In the
 * second half of the split screen, a note opens in that same half, and an app
 * is asked to open beside this one. The pages do not know which case they are
 * in: they ask [rememberPageOpener], and the split screen puts its own opener
 * in the composition for the half it owns.
 */
interface PageOpener {
    fun openTypedNote(path: String)
    fun openInkNote(path: String, title: String, template: PageTemplate = PageTemplate.Blank)
    fun openBook(book: LibraryBook)

    /** False when the app could not be started. */
    fun openApp(entry: LauncherEntry): Boolean
}

/** Null on a plain screen, where [rememberPageOpener] makes the usual one. */
val LocalPageOpener = staticCompositionLocalOf<PageOpener?> { null }

@Composable
fun rememberPageOpener(): PageOpener {
    val context = LocalContext.current
    return LocalPageOpener.current ?: remember(context) { ScreenPageOpener(context) }
}

/**
 * Opens each thing in a screen of its own.
 *
 * [carry] is the split screen to take along, when there is one: a book or a
 * note opened from Home while a page stands beside it opens with that page
 * still beside it. [afterCarry] runs once a screen took the split screen with
 * it, so the screen it came from can let go of its own copy.
 */
class ScreenPageOpener(
    private val context: Context,
    private val carry: () -> SplitCarry? = { null },
    private val afterCarry: () -> Unit = {},
) : PageOpener {

    override fun openTypedNote(path: String) {
        start("the note $path") { NoteEditorActivity.intent(context, path) }
    }

    override fun openInkNote(path: String, title: String, template: PageTemplate) {
        start("the ink note $path") { InkNoteActivity.intent(context, path, title, template) }
    }

    override fun openBook(book: LibraryBook) {
        start(book.path) { bookIntent(context, book) }
    }

    override fun openApp(entry: LauncherEntry): Boolean = AppsRepository(context).launch(entry)

    private fun start(what: String, make: () -> Intent) {
        val taken = carry()
        runCatching {
            context.startActivity(make().withSplit(taken))
            if (taken != null) afterCarry()
        }.onFailure { AppLog.e(TAG, "Could not open $what", it) }
    }
}

fun bookIntent(context: Context, book: LibraryBook): Intent =
    if (book.isPdf) {
        PdfReaderActivity.intent(context, book.path, book.bookId, book.metadata.title)
    } else {
        EpubReaderActivity.intent(context, book.path, book.bookId, book.metadata.title)
    }

/** Puts the split screen into an intent, for the screen it starts. Nothing when [carry] is null. */
fun Intent.withSplit(carry: SplitCarry?): Intent {
    if (carry != null) PanePageCodec.write(carry).forEach { (key, value) -> putExtra(key, value) }
    return this
}

/** The split screen a screen was started with, or null. */
fun Intent.splitCarry(): SplitCarry? = PanePageCodec.read { key -> getStringExtra(key) }
