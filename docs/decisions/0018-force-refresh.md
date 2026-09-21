# 0018. Force Refresh: a black flash, not only a vendor mode

Date: 2026-09-22. The owner, after build 0.2.0 on the tablet: pressing
"Clean the screen now" does not refresh the screen. He also asked for plain
names: "Force Refresh" and "Landscape Mode".

## What went wrong

`EinkDevice.fullRefresh()` set the ViWoods picture mode to 17, the full
refresh mode, and set the old mode back 700 ms later. A picture mode only says
how the next change of the screen is painted. After a press on a settings row
almost nothing on the screen changes, so there was nothing to paint, and the
panel did nothing. Decision 0007, part 6, did not see that.

## What it does now

`core/eink/ScreenRefresh.run(activity)`:

1. If the vendor mode can be read, it is set to the full refresh mode.
2. A plain black view covers the whole window at once.
3. After 450 ms the black view goes.
4. 700 ms later the old vendor mode comes back. A panel found in mode 17
   already, left there by a refresh that never finished, goes to mode 3.

Black over the whole window changes every pixel, and taking it away changes
every pixel again. That clears the grey marks on any e-ink panel, with or
without vendor control, so Force Refresh and Auto Refresh now work on every
device and are never greyed out. When the vendor mode cannot be read, it is
left alone, because nobody could put it back.

It is one change to black and one back, with no fade in between, so e-ink
rule 1 holds. A tap during the black lands on the black view and does
nothing. A second call while the black is up does nothing.

## Where it is used

- Settings, Look and screen, "Force Refresh".
- "Auto Refresh": after a change of tab and after a change of the split
  screen. It waits for the frame of the new page first. The first screen after
  a start is not a change and gets no flash.
- The readers' "Full refresh every N pages".
- The pen test page, and a new row "4b" on the device test screen.

`EinkDevice.fullRefresh()` stays, for row 4 of the device test, which tests
the vendor call alone.

## The numbers are guesses

450 ms is about one full e-ink update. If the tablet shows no black, or only a
grey flicker, the black is too short: raise `ScreenRefresh.BLACK_MILLIS`. If
the flash feels long, lower it. The log writes one line per refresh, tag
`ScreenRefresh`, with the vendor mode it found.
