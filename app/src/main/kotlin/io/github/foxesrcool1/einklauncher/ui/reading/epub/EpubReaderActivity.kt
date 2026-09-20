package io.github.foxesrcool1.einklauncher.ui.reading.epub

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.commitNow
import androidx.lifecycle.lifecycleScope
import io.github.foxesrcool1.einklauncher.core.eink.EinkDevices
import io.github.foxesrcool1.einklauncher.core.habits.DayBoundary
import io.github.foxesrcool1.einklauncher.core.habits.HabitsRepository
import io.github.foxesrcool1.einklauncher.core.log.AppLog
import io.github.foxesrcool1.einklauncher.core.reading.BookAnnotations
import io.github.foxesrcool1.einklauncher.core.reading.Highlight
import io.github.foxesrcool1.einklauncher.core.reading.ReadingLog
import io.github.foxesrcool1.einklauncher.core.reading.ReadingRepository
import io.github.foxesrcool1.einklauncher.core.reading.ReadingTimer
import io.github.foxesrcool1.einklauncher.core.settings.ReaderSettings
import io.github.foxesrcool1.einklauncher.core.settings.SettingsStore
import io.github.foxesrcool1.einklauncher.core.storage.DataRoot
import io.github.foxesrcool1.einklauncher.core.storage.RelativePaths
import io.github.foxesrcool1.einklauncher.design.EinkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.readium.r2.navigator.DecorableNavigator
import org.readium.r2.navigator.Decoration
import org.readium.r2.navigator.epub.EpubNavigatorFactory
import org.readium.r2.navigator.epub.EpubNavigatorFragment
import org.readium.r2.navigator.epub.css.FontStyle
import org.readium.r2.navigator.input.InputListener
import org.readium.r2.navigator.input.TapEvent
import org.readium.r2.shared.ExperimentalReadiumApi
import org.readium.r2.shared.publication.Link
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.Publication
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

private const val TAG = "EpubReader"
private const val NAVIGATOR_TAG = "epub-navigator"
private const val HIGHLIGHT_GROUP = "highlights"

/** One line of the table of contents. */
data class TocRow(val title: String, val depth: Int, val link: Link)

/** What is showing on top of the page. */
sealed interface ReaderPanel {
    data object None : ReaderPanel
    data object Menu : ReaderPanel
    data object Contents : ReaderPanel
    data object Notes : ReaderPanel
    data object Text : ReaderPanel
    data class HighlightOptions(val highlightId: String) : ReaderPanel
    data class NoteEditor(val highlightId: String) : ReaderPanel
}

/** Everything the reader's Compose layer shows. The activity writes it, the composables read it. */
class ReaderUiState {
    var loading by mutableStateOf(true)
    var failure by mutableStateOf<String?>(null)
    var title by mutableStateOf("")
    var panel by mutableStateOf<ReaderPanel>(ReaderPanel.None)
    var selecting by mutableStateOf(false)
    var annotations by mutableStateOf(BookAnnotations())
    var settings by mutableStateOf(ReaderSettings())
    var contents by mutableStateOf<List<TocRow>>(emptyList())
    var placeLabel by mutableStateOf("")
    var secondsToday by mutableStateOf(0L)
    var notice by mutableStateOf<String?>(null)
}

/**
 * The EPUB reader, on the Readium toolkit.
 *
 * Its own activity, as the plan says, so Home and Recents behave.
 *
 * What is done here for e-ink and nowhere in Readium itself:
 *
 * - Every page turn asks for `animated = false`.
 * - Sideways drags never reach the book view, see [SwipeInterceptLayout].
 * - The system text selection toolbar is kept empty and a plain bar of our
 *   own is shown instead, because that toolbar fades and floats.
 * - Nothing on the page repaints by itself. The menu comes up on a tap in the
 *   middle and goes away on the next one.
 */
@OptIn(ExperimentalReadiumApi::class)
class EpubReaderActivity : FragmentActivity() {

    private val ui = ReaderUiState()
    private lateinit var reading: ReadingRepository
    private lateinit var settingsStore: SettingsStore
    private lateinit var root: SwipeInterceptLayout

    private var bookId = ""
    private var bookPath = ""
    private var publication: Publication? = null
    private var navigator: EpubNavigatorFragment? = null
    private var selectionMode: ActionMode? = null

    private val timer = ReadingTimer()
    private var pageTurns = 0
    private var saveJob: Job? = null
    private var today: LocalDate = LocalDate.now()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Never restore fragments. The book view cannot be rebuilt before the
        // book is open, and the place in the book is saved by this app anyway.
        super.onCreate(null)

        bookPath = intent.getStringExtra(EXTRA_PATH).orEmpty()
        bookId = intent.getStringExtra(EXTRA_BOOK_ID).orEmpty()
        ui.title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        if (bookPath.isBlank() || bookId.isBlank()) {
            AppLog.e(TAG, "Opened with no book")
            finish()
            return
        }

        val data = DataRoot.repository(this)
        reading = ReadingRepository(data)
        settingsStore = SettingsStore(this)

        root = SwipeInterceptLayout(this).apply {
            setBackgroundColor(Color.WHITE)
            onSwipe = { direction -> turnPage(direction) }
        }
        val container = FragmentContainerView(this).apply { id = CONTAINER_ID }
        root.addView(container, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        val chrome = ComposeView(this).apply {
            setContent {
                EinkTheme(botanicalArt = false) {
                    ReaderChrome(
                        state = ui,
                        actions = actions,
                    )
                }
            }
        }
        root.addView(chrome, FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        setContentView(root)

        lifecycleScope.launch { openBook() }
    }

    private suspend fun openBook() {
        val started = System.currentTimeMillis()
        ui.settings = settingsStore.reader.first()

        val file = File(DataRoot.folder(this), RelativePaths.normalise(bookPath))
        val (opened, annotations, day) = withContext(Dispatchers.IO) {
            val data = DataRoot.repository(this@EpubReaderActivity)
            val hour = runCatching { HabitsRepository(data).load().dayBoundaryHour }.getOrDefault(DayBoundary.DEFAULT_HOUR)
            Triple(
                EpubOpener.open(this@EpubReaderActivity, file),
                reading.load(bookId),
                DayBoundary(hour).dateOf(Instant.now(), ZoneId.systemDefault()),
            )
        }
        today = day

        if (opened == null) {
            ui.loading = false
            ui.failure = "This book could not be opened. The log says why."
            return
        }
        publication = opened
        if (ui.title.isBlank()) ui.title = opened.metadata.title.orEmpty()
        ui.annotations = annotations.copy(bookPath = bookPath, title = ui.title)
        ui.contents = flatten(opened.tableOfContents, 0)
        loggedSecondsToday = withContext(Dispatchers.IO) { reading.secondsReadOn(today) }
        ui.secondsToday = loggedSecondsToday

        val factory = EpubNavigatorFactory(opened)
        supportFragmentManager.fragmentFactory = factory.createFragmentFactory(
            initialLocator = annotations.position?.toLocator(),
            initialPreferences = ui.settings.toEpubPreferences(),
            configuration = EpubNavigatorFragment.Configuration {
                servedAssets = servedAssets + LITERATA_ASSET
                addFontFamilyDeclaration(LITERATA_FAMILY) {
                    addFontFace {
                        addSource(LITERATA_ASSET, preload = true)
                        setFontStyle(FontStyle.NORMAL)
                        setFontWeight(200..900)
                    }
                }
                selectionActionModeCallback = selectionCallback
            },
        )
        supportFragmentManager.commitNow {
            add(CONTAINER_ID, EpubNavigatorFragment::class.java, Bundle(), NAVIGATOR_TAG)
        }
        val made = supportFragmentManager.findFragmentByTag(NAVIGATOR_TAG) as EpubNavigatorFragment
        navigator = made

        made.addInputListener(tapListener)
        made.addDecorationListener(HIGHLIGHT_GROUP, decorationListener)
        lifecycleScope.launch { made.currentLocator.collect { onPlaceChanged(it) } }
        showHighlights()

        ui.loading = false
        timer.resume(nowSeconds())
        AppLog.i(TAG, "Opened $bookPath in ${System.currentTimeMillis() - started} ms, ${annotations.highlights.size} highlights")
    }

    private fun flatten(links: List<Link>, depth: Int): List<TocRow> =
        links.flatMap { link ->
            listOf(TocRow(link.title?.trim().orEmpty().ifBlank { "Untitled" }, depth, link)) +
                flatten(link.children, depth + 1)
        }

    // -- Turning pages -------------------------------------------------------------

    private fun turnPage(direction: Int) {
        val current = navigator ?: return
        if (ui.panel != ReaderPanel.None || ui.selecting) return
        val moved = if (direction > 0) current.goForward(animated = false) else current.goBackward(animated = false)
        if (!moved) return
        timer.activity(nowSeconds())

        pageTurns++
        val every = ui.settings.refreshEveryPages
        if (every > 0 && pageTurns % every == 0) EinkDevices.get(this).fullRefresh()
    }

    private val tapListener = object : InputListener {
        override fun onTap(event: TapEvent): Boolean {
            if (ui.selecting) return false
            val width = root.width.takeIf { it > 0 } ?: return false
            val x = event.point.x
            when {
                ui.panel != ReaderPanel.None -> ui.panel = ReaderPanel.None
                x < width * 0.3f -> turnPage(-1)
                x > width * 0.7f -> turnPage(1)
                else -> {
                    ui.secondsToday = todaySeconds()
                    ui.panel = ReaderPanel.Menu
                }
            }
            return true
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_SPACE, KeyEvent.KEYCODE_VOLUME_DOWN -> {
            turnPage(1)
            true
        }

        KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_VOLUME_UP -> {
            turnPage(-1)
            true
        }

        KeyEvent.KEYCODE_ESCAPE -> {
            if (ui.panel != ReaderPanel.None) ui.panel = ReaderPanel.None else finish()
            true
        }

        else -> super.onKeyDown(keyCode, event)
    }

    @Deprecated("The platform still calls it for the Back key on the Android versions this app supports.")
    override fun onBackPressed() {
        if (ui.panel != ReaderPanel.None) {
            ui.panel = ReaderPanel.None
        } else {
            @Suppress("DEPRECATION")
            super.onBackPressed()
        }
    }

    // -- The place in the book -------------------------------------------------------

    private fun onPlaceChanged(locator: Locator) {
        val progress = locator.locations.totalProgression ?: 0.0
        val position = locator.locations.position
        ui.placeLabel = buildString {
            append("${(progress * 100).toInt()} %")
            if (position != null) append("  .  position $position")
            locator.title?.takeIf { it.isNotBlank() }?.let { append("  .  ").append(it) }
        }
        ui.annotations = ui.annotations.copy(position = locator.toJsonObject(), progress = progress)

        // Not on every page turn: a write to flash is slow, and a lost minute
        // of position costs nothing. onPause saves for certain.
        if (saveJob?.isActive != true) {
            saveJob = lifecycleScope.launch {
                delay(SAVE_EVERY_MILLIS)
                saveNow()
            }
        }
    }

    private suspend fun saveNow() {
        val snapshot = ui.annotations
        withContext(Dispatchers.IO) { reading.save(bookId, snapshot) }
    }

    // -- Selection, highlights and notes ------------------------------------------------

    /**
     * The system would show a floating toolbar here. An empty menu keeps it
     * away, and [ui.selecting] brings up the plain bar instead.
     */
    private val selectionCallback = object : ActionMode.Callback2() {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            selectionMode = mode
            menu.clear()
            ui.selecting = true
            ui.panel = ReaderPanel.None
            root.swipesEnabled = false
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu): Boolean {
            menu.clear()
            return true
        }

        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean = false

        override fun onDestroyActionMode(mode: ActionMode) {
            selectionMode = null
            ui.selecting = false
            root.swipesEnabled = true
        }
    }

    private fun highlightSelection(thenWriteNote: Boolean) {
        val current = navigator ?: return
        lifecycleScope.launch {
            val selection = current.currentSelection()
            current.clearSelection()
            selectionMode?.finish()
            val locator = selection?.locator
            val json = locator?.toJsonObject()
            if (locator == null || json == null) {
                ui.notice = "Nothing was selected"
                return@launch
            }
            val highlight = Highlight(
                id = "h" + System.currentTimeMillis().toString(36),
                locator = json,
                text = locator.text.highlight.orEmpty().trim(),
                progression = locator.locations.totalProgression ?: ui.annotations.progress,
                chapter = locator.title.orEmpty(),
                createdAt = Instant.now().toString(),
            )
            ui.annotations = ui.annotations.withHighlight(highlight)
            saveNow()
            showHighlights()
            timer.activity(nowSeconds())
            if (thenWriteNote) ui.panel = ReaderPanel.NoteEditor(highlight.id)
        }
    }

    private suspend fun showHighlights() {
        val current = navigator ?: return
        val underline = ui.settings.underlineHighlights
        val decorations = ui.annotations.highlights.mapNotNull { highlight ->
            val locator = highlight.locator.toLocator() ?: return@mapNotNull null
            Decoration(
                id = highlight.id,
                locator = locator,
                style = if (underline) {
                    Decoration.Style.Underline(tint = Color.BLACK)
                } else {
                    Decoration.Style.Highlight(tint = HIGHLIGHT_GREY)
                },
            )
        }
        current.applyDecorations(decorations, HIGHLIGHT_GROUP)
    }

    private val decorationListener = object : DecorableNavigator.Listener {
        override fun onDecorationActivated(event: DecorableNavigator.OnActivatedEvent): Boolean {
            ui.panel = ReaderPanel.HighlightOptions(event.decoration.id)
            return true
        }
    }

    // -- What the Compose layer may ask for ------------------------------------------------

    private val actions = object : ReaderActions {
        override fun close() = finish()

        override fun show(panel: ReaderPanel) {
            ui.notice = null
            ui.panel = panel
        }

        override fun goTo(row: TocRow) {
            ui.panel = ReaderPanel.None
            navigator?.go(row.link, animated = false)
        }

        override fun goTo(highlight: Highlight) {
            ui.panel = ReaderPanel.None
            highlight.locator.toLocator()?.let { navigator?.go(it, animated = false) }
        }

        override fun highlight() = highlightSelection(thenWriteNote = false)

        override fun highlightWithNote() = highlightSelection(thenWriteNote = true)

        override fun cancelSelection() {
            navigator?.clearSelection()
            selectionMode?.finish()
        }

        override fun saveNote(highlightId: String, note: String) {
            val highlight = ui.annotations.highlight(highlightId) ?: return
            ui.annotations = ui.annotations.withHighlight(highlight.copy(note = note.trim()))
            ui.panel = ReaderPanel.None
            lifecycleScope.launch { saveNow() }
        }

        override fun remove(highlightId: String) {
            ui.annotations = ui.annotations.without(highlightId)
            ui.panel = ReaderPanel.None
            lifecycleScope.launch {
                saveNow()
                showHighlights()
            }
        }

        override fun change(settings: ReaderSettings) {
            val before = ui.settings
            val after = settings.clamped()
            ui.settings = after
            navigator?.submitPreferences(after.toEpubPreferences())
            lifecycleScope.launch {
                settingsStore.setReader(after)
                if (before.underlineHighlights != after.underlineHighlights) showHighlights()
            }
        }

        override fun exportNotes() {
            lifecycleScope.launch {
                val path = withContext(Dispatchers.IO) { reading.exportMarkdown(ui.annotations) }
                ui.notice = if (path != null) "Saved as $path" else "The export did not work"
            }
        }
    }

    // -- Reading time ------------------------------------------------------------------------

    private fun nowSeconds(): Long = android.os.SystemClock.elapsedRealtime() / 1000

    /** What the log already holds for today. This session is added on top when the menu opens. */
    private var loggedSecondsToday = 0L

    private fun todaySeconds(): Long = loggedSecondsToday + timer.peek(nowSeconds())

    override fun onResume() {
        super.onResume()
        if (navigator != null) timer.resume(nowSeconds())
    }

    override fun onPause() {
        timer.pause(nowSeconds())
        val seconds = timer.take()
        selectionMode?.finish()
        val snapshot = ui.annotations
        if (::reading.isInitialized && publication != null) {
            loggedSecondsToday += seconds
            // Blocking, and short. The process may be gone a moment from now.
            runCatching {
                reading.save(bookId, snapshot)
                reading.addReadingTime(bookId, today, seconds)
            }.onFailure { AppLog.e(TAG, "Could not save on the way out", it) }
        }
        super.onPause()
    }

    override fun onDestroy() {
        runCatching { publication?.close() }
        super.onDestroy()
    }

    companion object {
        const val EXTRA_PATH = "book_path"
        const val EXTRA_BOOK_ID = "book_id"
        const val EXTRA_TITLE = "book_title"

        private const val SAVE_EVERY_MILLIS = 5_000L
        private val HIGHLIGHT_GREY = Color.rgb(200, 200, 200)
        private val CONTAINER_ID = android.view.View.generateViewId()

        fun intent(context: Context, bookPath: String, bookId: String, title: String): Intent =
            Intent(context, EpubReaderActivity::class.java)
                .putExtra(EXTRA_PATH, bookPath)
                .putExtra(EXTRA_BOOK_ID, bookId)
                .putExtra(EXTRA_TITLE, title)

        /** The goal line under the menu. */
        fun goalFraction(state: ReaderUiState): Float =
            ReadingLog.goalFraction(state.secondsToday, state.settings.goalMinutes)
    }
}

/** What the reader's Compose layer can ask the activity to do. */
interface ReaderActions {
    fun close()
    fun show(panel: ReaderPanel)
    fun goTo(row: TocRow)
    fun goTo(highlight: Highlight)
    fun highlight()
    fun highlightWithNote()
    fun cancelSelection()
    fun saveNote(highlightId: String, note: String)
    fun remove(highlightId: String)
    fun change(settings: ReaderSettings)
    fun exportNotes()
}
