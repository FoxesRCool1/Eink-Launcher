# Assets

Every font, drawing and sample file in this repository is listed here with its
source and its licence. The project will be published, so nothing enters the
repository without a line in this file.

Rule: no GPL, no AGPL, no CC BY-NC, no "free for personal use". See `plan.md`
sections 3.5 and 4.

## Fonts

All three are variable fonts under the SIL Open Font Licence 1.1. The full
licence text of each one is in `licenses/fonts/`.

| File | Family | Source | Licence | Checked |
| --- | --- | --- | --- | --- |
| `app/src/main/res/font/bodoni_moda.ttf` | Bodoni Moda | google/fonts, `ofl/bodonimoda` | SIL OFL 1.1 | 2026-09-19 |
| `app/src/main/res/font/jost.ttf` | Jost | google/fonts, `ofl/jost` | SIL OFL 1.1 | 2026-09-19 |
| `app/src/main/res/font/literata.ttf` | Literata | google/fonts, `ofl/literata` | SIL OFL 1.1 | 2026-09-19 |

The OFL allows use, change and redistribution, including inside an app. The
font files are not renamed and the licence text travels with them.

## Drawings

| File | What it is | Source | Licence |
| --- | --- | --- | --- |
| `app/src/main/res/drawable/ic_launcher_foreground.xml` | The app icon: a stem with six leaves | Original work for this project, written by hand as vector paths | Apache-2.0, same as the app |
| `design/components/BotanicalCorner.kt` | The corner sprig, drawn in code with cubic curves | Original work for this project | Apache-2.0, same as the app |

Nothing here is traced from, copied from or derived from another drawing. The
article that inspired the project is under CC BY-SA 4.0, and its images are not
used, not traced and not reproduced.

## Sample content

| Where | What it is | Source | Licence |
| --- | --- | --- | --- |
| `ReaderChromeScreenshotTest.kt`, and so `docs/screenshots/reader_menu.png` and `reader_notes.png` | The first sentences of *Walden*, Henry David Thoreau, 1854 | Project Gutenberg, ebook 205 | Public domain |
| `EpubReaderSmokeTest.kt` | A two chapter EPUB made by the test itself | Written for this project | Apache-2.0, same as the app |
| `docs/screenshots/*.png` | Pictures of this app, made by the screenshot tests | This project | Apache-2.0, same as the app |

No book file is in the repository. When a screenshot needs more text, use
only public domain text from Standard Ebooks or Project Gutenberg, and add a
line here.

## Libraries

Fonts and drawings are on this page. Libraries are in
`licenses/DEPENDENCIES.md`.
