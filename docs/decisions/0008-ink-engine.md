# 0008. The ink engine

Date: 2026-09-20. Step 5.

## What this decides

How handwriting is held in memory, painted, and saved.

## The plan said Jetpack Ink. This does not use it. Why.

The plan names `androidx.ink` for the stroke model, the geometry, the storage
format and the fallback renderer. Step 5 uses the app's own small model
instead, and keeps Jetpack Ink only as a debug dependency for the latency
baseline on the device test screen. Three reasons:

1. **Tests.** Jetpack Ink does its work in a native library. The unit tests
   of this project run on the JVM with no tablet and no adb, and the plan asks
   for unit tests of the file format, the eraser geometry and the undo stack.
   With the app's own model all three are plain JUnit tests that run in a
   second.
2. **The hand-over.** On the ViWoods tablet the fast pen paints the line, and
   then the app has to paint the real stroke on top so that nothing jumps.
   That needs full control of width and timing in the painting code. Jetpack
   Ink's in-progress view paints with its own front buffer and its own idea of
   a brush.
3. **The files belong to the user.** The plan says plain files are the truth
   and the user can read them with something else. The Jetpack Ink storage
   format is a compressed protocol buffer that only that library reads. The
   format here is twenty lines of documentation.

Cost: no stroke smoothing or prediction from the library. On e-ink the panel
is the slow part, not the input, so that is a small loss.

**To go back:** `InkStroke` holds x, y and pressure per point, which is what
a Jetpack Ink `StrokeInputBatch` holds. A converter is a short function.

## The model

- `InkStroke`: tool, width at full pressure, and three float arrays. It never
  changes after it is made.
- Numbers are in page units, not screen pixels. A handwritten page is 1440
  units wide. A PDF page uses PDF points. So strokes stay in place at every
  zoom and crop, which Step 8 needs.
- A new note takes the height of the canvas it is first shown on, so the page
  fills the screen. Once it holds ink the shape never changes again.
- `InkPageEditor`: the strokes of one page with undo and redo. Every change is
  "added" or "removed", so the eraser and the pen share one history. 200
  steps.
- The eraser takes whole strokes. Rubbing out part of a line would repaint it
  many times, and on e-ink every repaint shows.

## The files

`.strokes`, one page of ink, big endian:

```
"EINK", u16 version (1), i32 stroke count,
per stroke: u8 tool, f32 width, i32 point count,
per point:  f32 x, f32 y, u8 pressure
```

`.inknote`, a zip: `meta.json`, `pages/0001.strokes`, `pages/0001.png`.

Both refuse a version newer than they know. Both refuse sizes that are not
believable before they allocate anything. A stroke from an unknown tool is
dropped and the rest of the page is kept. A note that cannot be read is shown
empty and is **never saved over**.

## Painting

- Finished ink lives in one bitmap. `onDraw` is one bitmap copy.
- While the pen is down only the newest piece is painted, and only its box is
  invalidated. Undo, redo and erase repaint only the box around the strokes
  that changed. E-ink rule 7.
- Pen width follows pressure, from 40 % to 100 % of the chosen width.
  Neighbouring pieces of nearly the same width go into one path, so a page of
  2 000 strokes is a few thousand draw calls and not a hundred thousand.
- The highlighter is a flat grey painted in DARKEN mode. Grey over white is
  grey, grey over grey is the same grey, grey over black stays black. So it
  never hides pen ink or PDF text, painting order does not matter, and "drawn
  under the pen strokes" holds with no second layer.

## Input

- Only a stylus draws. The eraser end erases. So does the side button while
  it is held, if the tablet reports it.
- A finger never draws. A finger swipe turns the page, unless the pen touched
  or hovered in the last 700 ms. That is the palm rejection.
- `requestUnbufferedDispatch` on pen-down, so points arrive as they are made
  and not once per frame.

## Fast pen hand-over

`deviceDrawsLive` on the canvas. When it is on, the canvas records the points
and paints nothing while the pen is down. Strokes go into the model at once,
so undo and save never wait. Painting waits until the pen has been up for the
redraw wait (900 ms to start with, a setting), and every new stroke inside
the wait starts the wait again. That matches what the public notes say about
the overlay: it clears 800 ms after the last pen-up.

`FastPenSession` starts and stops the device side. It stops before a dialog
opens and when the screen pauses, so the tablet never draws on a dialog.

The fast pen is **off by default** until the device test has shown which
path works. It is a setting under "Look and pen".

## Saving

A save copies the stroke lists on the main thread, which is cheap, and does
the rest on one background thread: preview pictures for the pages that
changed, the zip, and the atomic write. It runs 2.5 seconds after the last
stroke, and when the screen stops.

## Measured on the dev machine

- Reading 2 000 strokes of 50 points: a few milliseconds. About 900 KB.
- Painting them through the test sandbox: well under a second.

The real numbers come from the tablet. The app writes the read time and the
paint time into the log for any page over 200 strokes.
