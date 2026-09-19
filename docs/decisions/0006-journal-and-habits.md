# 0006. Journal and habits

Date: 2026-09-19
Status: accepted
Step: 9

## Two files, not one

`habits/habits.json` holds the list of habits. `habits/log.csv` holds one line
per day done.

Splitting them does three things. The log only ever grows at the end, so
writing it is cheap. A year of ticks stays out of the file a user is most
likely to open and edit by hand. And a spreadsheet can open the log directly,
which is the whole point of keeping plain files.

Both are readable and editable by hand. If a user renames a habit in a text
editor, the app must not mind, so a broken habit or a broken line is dropped
and everything else still loads.

## The day does not start at midnight

Someone who reads until half past one has not started tomorrow. A tracker that
tells them they broke a 40 day streak is wrong about the only thing it does.

`DayBoundary` moves the line, with 04:00 as the default, and the hour is a
setting as the plan asks. It takes the moment and the time zone as arguments
rather than reading the clock, so the tests can ask what happens on the day
the clocks change and in a different country.

## A day that is not finished does not break a streak

If yesterday is marked and today is not, the streak stands and the user still
has the rest of today. `needsDoingToday` says so, and the screen can nudge
without lying about the number.

The alternative, resetting the count at one minute past the boundary, punishes
someone for not having done the thing yet. That is the behaviour that makes
people stop using a tracker.

## Archive, never delete

`setArchived` hides a habit. There is no delete.

The log keeps every day that was done. Deleting a habit would leave lines in
the log pointing at nothing, and a year of work would be gone to one tap on a
touch screen the user is holding in one hand.

## The month view has no fill

A day with an entry carries a short rule under its number. Not a filled cell,
not a grey wash. A filled cell on this panel is a large black block that
ghosts into the next screen, and grey dithers into dirt. Rule 3 and rule 7.

The whole month fits on one screen by definition, so nothing scrolls.

## What is not here

- **The handwritten entry.** It needs the ink engine, which is step 5 and is
  tagged for Fable. The "Handwrite" control exists and is disabled.
- **The routine list and the "Next" line on Today.** Still to build.
- **A screenshot of the whole screen.** `JournalScreen` loads from disk in the
  background, and a screenshot test that waits on a background job fails once
  a month for a reason nobody can find. `MonthView` and `HabitRow` take their
  data as arguments and are captured instead.
