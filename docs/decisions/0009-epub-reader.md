# 0009. The EPUB reader

Date: 2026-09-20. Step 7.

## What this decides

Which library reads EPUB files, and what the app does around it to keep an
e-ink panel calm.

## Readium Kotlin Toolkit 3.4.0

The plan names it. Checked on 2026-09-20:

- Licence: BSD-3-Clause. Its runtime dependencies are Apache-2.0 (AndroidX,
  kotlinx, Guava, okio, Timber, koi, jspecify) and MIT (jsoup). No GPL, no
  AGPL. It does not bring Compose Material or the Material view library.
- It needs min SDK 24, compile SDK 37 and core library desugaring. The app
  has min SDK 29 and compile SDK 37, and desugaring is now on.
- **It is compiled with Kotlin 2.4.** AGP 9.4.0 compiles Kotlin with the
  Kotlin Gradle plugin 2.2.10 it was built against, and that compiler cannot
  read Kotlin 2.4 classes. The root `build.gradle.kts` now puts the Kotlin
  Gradle plugin 2.4.20 on the build classpath, which is the documented way
  to make AGP's built-in Kotlin use a newer compiler. Keep that version the
  same as the Compose compiler plugin in the version catalogue.
- Modules used: `readium-shared`, `readium-streamer`, `readium-navigator`.
  Not used: OPDS, LCP, the PDF adapters.

`EpubMetadata` from step 7 part one stays. The library list needs a title and
an author for every book, and opening each book with Readium to get them
would make the list slow. It is the fast path, as the hand-off note allowed.

## What the app adds for e-ink

Readium is made for phones. Four things in it move, and each one is stopped:

1. **Page turns.** Every `goForward`, `goBackward` and `go` call passes
   `animated = false`.
2. **Swipes.** Left alone, the book view lets the page follow the finger and
   then slides it into place. `SwipeInterceptLayout` sits around the book
   view. Once a drag is clearly sideways it takes the gesture away from the
   book view, and on finger-up it asks for one page turn. Taps and long
   presses pass through, so tap zones and text selection still work. While
   text is selected the layout stands down, because dragging a selection
   handle is also a sideways drag.
3. **The selection toolbar.** The system toolbar floats and fades. The
   selection action mode gets an empty menu, which keeps that toolbar away,
   and a plain bar of our own with Highlight, Note and Cancel comes up at the
   bottom.
4. **The menu.** Nothing is drawn over the page until the middle of the page
   is tapped. The reading time in the menu is worked out when the menu opens
   and does not tick while it is open.

The preferences handed to Readium always say: paginated, one column, black on
white, light theme. The user chooses font, size, margins, line spacing and
justify. Line spacing and justify need the book's own styles turned off, so
they are hidden when the font is "The book's own".

Literata is served to the book view from the assets. The font file already
lives in `res/font`, so `app/build.gradle.kts` adds that folder as a second
assets folder. There is no second copy of the file in the repository.

## Highlights

Two styles, a setting: an underline, which is pure black and the default, or
a grey block. The plan says to test both on the panel. Grey may dither.

## Where the data goes

`annotations/<book-id>.json`: the last position, the progress for the library
list, and the highlights with their notes. Positions and highlight places are
Readium locators, stored as they are. `core/reading/` never looks inside
them, so it has no Readium in it and its tests are plain JUnit.

An annotations file that cannot be read is copied to
`<book-id>.unreadable.json` before anything is saved, so a stray comma cannot
cost the user their notes.

`annotations/reading-log.csv`: one line per reading session. The timer only
counts while the reader is on screen and stops five minutes after the last
page turn. The day changes at the hour set for the Journal, 04:00 by default.

## The activity never restores its fragments

`super.onCreate(null)`. Readium's book view cannot be rebuilt before the book
is open, and opening is not instant. The app saves the position itself, so
after the system kills the reader it starts again from the intent and lands
on the same page.

## What is still a risk

Plan section 8: "Readium WebView is slow or ghosts on e-ink". Nothing here
could be judged on the panel. The owner should read a chapter before any more
is built on it. If it ghosts, turn on the full refresh every N pages in the
Text menu first.
