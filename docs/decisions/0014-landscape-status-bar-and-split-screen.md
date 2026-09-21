# 0014. Landscape, the status bar, and the split screen

Date: 2026-09-20. Asked for by the owner. Plan section 3.1 said "Lock the app
to portrait for v1", and had landscape under "Later". The plan was changed.

## Landscape

- **A small grey icon at the top of every screen turns the screen**, and back.
  It is also a switch in Settings. The setting is for the whole app and it is
  kept.
- **The app does not follow a sensor.** Reviews disagree about whether the
  tablet has one, and an e-ink screen that turns by itself when the tablet is
  tilted is a full repaint nobody asked for.
- **"Turn it the other way" in Settings** flips landscape by 180 degrees. The
  tablet has keys on one edge, and only the person holding it knows which hand
  they should end up under.
- **A turn does not build the activity again.** Every user activity declares
  `orientation|screenSize` and the rest in `configChanges`. The open book, the
  ink on the page and the half typed note stay, and Compose lays the screen
  out again. The EPUB reader sends the book back to its place after the turn,
  because the book engine cuts the text into pages again and lands a page or
  two away.
- **`core/window/ScreenWindow`** sets the orientation and the status bar for
  every activity, before the first frame. It reads the setting once, blocking.
  That took 10 to 17 ms in the emulator, and the log says what it takes on the
  tablet. After that it is in memory.
- **A screen lays itself out by the room it has, not by the device.**
  `LocalWideScreen` is true when that room is wider than tall. Wide, the
  controls of a screen move up beside the title, the Journal puts the entry
  and the habits side by side, and the ink screens put their tools in a rail
  down the edge, because height is what runs out.
- **Every screen has a screenshot test in both orientations**, in
  `ScreensScreenshotTest.kt`. No screen scrolls, so this is how a control that
  falls off the bottom is caught.
- The two device test screens stay upright. A turn would wipe their results.
- A limit: from Android 16 on, a tablet 600 dp wide or more ignores a fixed
  orientation for an app that targets SDK 36. The `viwoods` build targets 30
  and the AiPaper Mini is 480 dp wide, so it is not touched. A `generic` build
  on a large tablet would follow the device, and the icon would do nothing.

## The status bar

Hidden in every activity, and in every dialog, because a dialog is a window of
its own and the bar came back with each one. A swipe down from the top edge
shows it for a moment: the tablet's own panel for the front light and Wi-Fi
hangs off that bar and must stay in reach. Home has its own clock, battery and
Wi-Fi. Settings can bring the bar back.

## The split screen

Replaced on 2026-09-22 by decision 0016: every screen has a split screen now,
any page can stand in the second half, and another app can be asked to open
beside a page. What follows is the first version, kept for the reasoning.

- **It is inside the readers, not the Android split screen.** Android's needs
  the firmware to support multi-window, and this firmware hides even its
  settings app. One icon in the EPUB menu and in the PDF toolbar opens a note
  pane beside the book: beside it when the tablet is on its side, under it
  when it is upright.
- **Each book has one typed and one handwritten note**,
  `notes/Reading notes/<book title>.md` and `.inknote`. They are plain notes,
  so the Writing tab shows them and a backup holds them. Nothing is written
  until the user types or draws.
- **The pane stands on the same code as the full screen editors**:
  `rememberNoteEditor` for typing, `InkNoteController` for ink. There is one
  place where a note can be lost, not two.
- **The fast pen serves one canvas.** The vendor call takes one box. With a
  handwritten note open beside a PDF, the fast pen goes to the note, because
  the long writing happens there. A mark on the PDF is then drawn by this app:
  slower, and still correct. This needs a look on the tablet once the fast pen
  itself works.
- **Not built:** choosing any note for the pane, and opening a book from a
  note. One note per book covers "read and take notes", and it keeps the pane
  to one row of controls.

## Checked in the emulator

Turn on Home and inside both readers. The split screen in the EPUB reader with
a real book, typed and handwritten, upright and on its side, and both files on
the disk afterwards. The PDF split, and a turn with the split open. The status
bar hidden, also under dialogs. Not checked, because only the tablet can:
ghosting after a turn, and the fast pen in the pane.
