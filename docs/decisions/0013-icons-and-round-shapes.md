# 0013. Lucide icons, round shapes, and a new Settings

Date: 2026-09-20. The owner used the first build and asked for these changes.
They go against plan section 5 as it was written, which said "Icons: almost
none. Use words." The owner decides the look, so the plan was changed to match.

## What the owner asked for

1. Lucide icons instead of words, for the app icon and the plants as well.
2. No square corners. "The harsh 90 degree angles are not calming."
3. "Home", not "Today".
4. A Settings page that is organised, has short text, and has help text.

## Icons

- **Every control that was a word is an icon now**, from Lucide 1.47.0. ISC
  licence, and MIT for the icons that come from Feather. Both allow a closed
  and a sold app. See `ASSETS.md`.
- **No icon library is in the APK.** `tools/lucide.py` reads a pinned Lucide
  release, checks its SHA-256, and writes the path data of the icons in
  `tools/lucide-icons.txt` into `design/icons/LucideIcons.kt`. The app draws
  them in `EinkIcon`. Two reasons: the rule about new dependencies, and the
  line width. A vector drawable scales its line with the icon, so a 56 dp
  icon on Home would get a 5 dp line. `EinkIcon` keeps the line at 2 dp at
  every size, which is e-ink rule 5.
- **Words stay where a picture is not safe.** The answer to "Delete this?" is
  a word. A row in a menu has an icon and a word. A value, such as "150 %" or
  "Page 3 of 90", stays text. Settings rows are words, because the owner asked
  for help text there.
- **Every icon control has a label** that is never shown. A screen reader
  speaks it, and the tests find controls by it.
- **Home shows four icons and no words.** Settings, Look and screen, "Names
  under the icons" puts the words back under them. It starts off, because the
  owner asked for icons in place of the words.
- **The plants are Lucide icons too**, drawn large with a 1 dp line. One plant
  per tab. Home has it in the top corner, as before. Every other screen has a
  small one beside the title, when the tablet is upright and the title is
  short. The old setting turns all of them off.

## Shapes

`design/EinkShapes.kt`. A control is a pebble: fully round ends. A dialog has
24 dp corners, a text field and a pressed row have 16 dp. There is still one
flat colour and one line, no shadow and no gradient, so e-ink rule 5 holds.
A control has a 1 dp line around it now, and only where the line helps: most
icon controls have none. A dialog keeps the 2 dp line, because nothing else
parts it from the page under it.

## Settings

The first Settings was six tabs of buttons, with the state written into the
button text ("Corner drawing is on"). Now:

- A menu of six groups. Each group is a row with an icon, a name and one line
  that says what is in it.
- Each group is a page of rows. A row has a name, one or two lines of help in
  plain words, and at its end a switch, the value it has now, or an arrow. The
  whole row is the target.
- A setting with more than two values opens a short list to choose from.
- Back goes up one level.

## Found on the way

- **`PagedList` took a fixed page size, and screens asked for more rows than
  fit.** The Apps tab asked for eight and six fit, so the last two apps of
  every page could not be seen. The device test asked for seven. Lists now
  take a row height and work out the page size from the room they have. That
  is also what makes landscape and the split screen possible.
- **Compose drops the last line of a text that is one pixel too tall for its
  box**, and ends the line before with three dots. A row height must have a
  few dp to spare.
- **A screenshot of a screen that loads from the disk was a race.** See
  `core/threads/AppDispatchers.kt`. The old note in the Journal test, "fails
  once a month for no reason anyone can find", was this.
