# 0002. File logging without adb

Date: 2026-09-19
Status: accepted
Step: 1

## Problem

`adb shell` returns "not support command" on this tablet. `adb install` and
`adb pull` fail. Logcat is out of reach. When the app misbehaves on the device
there is no way to see why.

## Decision

The app keeps its own log.

- `AppLog` writes one file per day to `filesDir/logs/log-YYYY-MM-DD.txt`.
- Lines also go to an in-memory ring buffer of 600 lines. The log viewer reads
  that buffer, so opening it costs no disk read.
- A single background thread does the file writes. Logging never blocks the UI.
- Files older than 14 days are deleted at start-up.
- `CrashHandler` catches an uncaught exception, writes
  `crash-YYYY-MM-DD-<millis>.txt`, copies it straight into
  `Download/EinkLauncher/`, then hands over to the handler that was there
  before, so the normal crash path still runs.

## Why the log is not written to Download all the time

`filesDir` is a plain file path and a write costs almost nothing.
`Download/EinkLauncher/` is only reachable through MediaStore on API 29 and
above, and every write goes through a content provider. Mirroring every line
would be slow and would spam the media database.

So: the app logs to `filesDir`, and the owner presses "Copy to download" in the
log viewer to move the files somewhere a file manager can reach. A crash file
is copied at once, because the owner may not be able to open the app again.

## Consequence

Every module must log through `AppLog`, not `android.util.Log`. `AppLog`
mirrors to logcat as well, which helps on a normal device or an emulator.
