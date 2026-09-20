# 0010. The PDF renderer

Date: 2026-09-20. Step 8.

## What this decides

Which code turns a PDF page into pixels, and how the reader keeps memory and
the e-ink panel under control.

## The choice: the platform `PdfRenderer`

Plan section 4 gave three candidates. MuPDF was already out: it is AGPL.

| | Platform `PdfRenderer` | PdfiumAndroid, direct | Readium PDFium adapter |
| --- | --- | --- | --- |
| Licence | Part of Android | Apache-2.0 and BSD | BSD-3, plus the row to the left |
| New dependency | None | A native library per CPU type | The same, and more of Readium |
| APK size | 0 | About 5 MB per CPU type | More |
| Who fixes security bugs | The tablet vendor, with the OS | This project, by updating | This project |
| Render a part of a page | Yes, with a matrix | Yes | Hidden behind its own view |
| Text selection | No | Possible | Possible |
| Password PDFs | No | Yes | Yes |

The platform renderer wins for version 1:

- A PDF is a file from somewhere else, and a PDF engine is a large pile of
  native code that reads it. The one in the OS is sandboxed by the platform
  and patched with it. Shipping a second one makes this project the party
  that has to watch for PDFium security fixes.
- It does everything version 1 asks for. Text selection in PDF is on the
  "later" list of the plan, not in version 1.
- The Readium adapter brings its own paged view, and the plan asks for an own
  paged view with an ink layer on top. Only the renderer is needed.

What is given up: password protected PDFs do not open, and there is no text
layer. If text selection in PDF moves into scope, PdfiumAndroid fits behind
`PdfPages`, which is the only class that knows the renderer.

On Android 13, which the tablet runs, the platform renderer is PDFium.

## Memory

The tablet has 4 GB. A scanned book can have pages of 30 million pixels.

- A page is never rendered whole at zoom. Only the part on the screen is
  rendered, with a matrix, straight into a bitmap the size of the view. That
  is about 11 MB at every zoom step, 200 % included.
- One page picture is alive at a time. The one before is recycled as soon as
  the new one is on the canvas.
- The margin search renders the page 160 pixels wide, once per page.
- `PdfRenderer` allows one open page at a time and is not thread safe. All
  of it runs on one worker thread, and `PdfPages` is synchronized as well.
- A newer request makes an older one give up before it renders, so holding
  Next down does not queue twenty renders.

There is no read-ahead cache yet. On e-ink the panel refresh is likely to be
the slow part, not the render. The log records every screen that took more
than 400 ms. If the log says the render is the slow part, render the next
screen into a second bitmap while the reader reads.

## Paginate, do not scroll

E-ink rule 2. A page that is larger than the screen at the chosen zoom is
cut into screens, left to right and then top to bottom. Next walks through
the screens and then moves to the next page. Two neighbouring screens share
8 % of the screen, so a line that is cut at the edge of one is whole on the
next. The zoom steps are the four fixed ones from the plan. There is no
pinch zoom: it needs an animation.

`PdfViewport` holds that arithmetic and has no Android in it.

## Crop margins

`CropDetector` looks for the first and last row and column that are not
paper white, adds 1.5 % of room, and leaves the page alone when the crop
would keep more than 97 % of it. It is found per page, because a scanned
book does not have the same margins on every page.

## The ink

The ink is an `InkCanvasView` over the page picture, the same one the
notebooks use. Strokes are kept in PDF points with the origin at the top
left of the page, one file per page:
`annotations/<book-id>/page-0001.strokes`. Zoom, crop and screen only change
which part of the page the canvas shows, never the numbers in the strokes.
So ink made at 200 % is in the right place at fit page, and the other way
round. There is a test for exactly that.

The pen is as wide on the glass as it is on a note page. A PDF page is about
600 points wide where a note page is 1440 units wide, so the canvas has a
`unitScale` that makes the stroke narrower in page units by that ratio.

The PDF file is never written to. A page with no ink left has no sidecar
file, so the folder lists exactly the pages that are worth listing.

## Handwritten note cards on EPUB highlights

Also step 8. A highlight can carry a card: an ordinary `.inknote` file at
`annotations/<book-id>/note-<highlight-id>.inknote`, opened in the same ink
screen as a notebook. The ink does not go onto the EPUB page itself, as plan
section 6 says: the text moves when the font size changes, and ink would not.
