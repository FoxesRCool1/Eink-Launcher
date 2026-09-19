# 0005. Storage layer

Date: 2026-09-19
Status: accepted, with one measurement still open
Step: 4

## The shape

```
FileStore            what the app may do with the data folder
  LocalFileStore     plain java.io.File. The only one today.
  (SafFileStore)     Storage Access Framework. Not written yet. See below.

StorageLayout        which folder and which file name, for every kind of thing
RelativePaths        the rules a path must follow
DataRepository       the one way in. Screens never see a raw path.
BackupArchive        zip out, zip in
LibraryIndex         a list of what is in the folder, thrown away and rebuilt
StorageBenchmark     the measurement this decision still needs
```

Everything above holds no Android class except `DataRoot`, which only decides
which folder to start from. That is why the whole layer is covered by plain
JUnit tests rather than Robolectric.

## Plain files are the truth

The user can open the folder with a file manager, copy it to a computer, sync
it, and read a note with anything. Nothing the app writes needs the app to be
read.

The index is not part of that promise. It is a faster way to ask what is in
the folder, it lives outside the user folder, and Settings has a button that
throws it away and builds it again. If it were ever the truth, a file the user
copied in by hand would be invisible, and that would break the promise.

## Where the folder is, and the measurement that is still open

Today the root is `Android/data/<package>/files/EinkLauncher` on the memory
card. It needs no permission, no picker, and no user decision on first run.

It has one real problem: on Android 11 and later, no other app can read
`Android/data/<package>`, so a sync program such as Syncthing cannot reach it.
Syncing plain files is one of the reasons to keep plain files at all.

The other way is a folder the user picks, through the Storage Access
Framework. That folder can sit anywhere, so a sync program can reach it. The
cost is speed: every read and write goes through a content provider, and how
slow that is on this tablet cannot be guessed from a computer.

So the plan asks for a measurement with 500 files before the choice is made.
`StorageBenchmark` does exactly that and the Dev screen runs it. The owner
must run it on the tablet and send back the numbers.

**Until then:** `LocalFileStore` is used, and backup to zip is the way data
leaves the tablet. If the measurement says the Storage Access Framework is
fast enough, write `SafFileStore` behind the same interface and change which
one `DataRoot` builds. Nothing else in the app has to change.

## Safe writes

Every write goes to a temporary file next to the target and is renamed over
it. A rename inside one folder is atomic on every file system this app will
meet, so a reader sees the old file or the new one, never half of one. A
tablet that runs out of battery in the middle of an autosave keeps the last
good version of the note.

## Paths are checked, not repaired

`RelativePaths.normalise` refuses `..`, a leading separator, a Windows drive
letter and a name with stray spaces. `LocalFileStore` checks a second time
against the canonical path, in case a symbolic link points out of the folder.

This matters most on restore. A zip file can name an entry `../../somewhere`,
and a restore that trusted the name would write outside the data folder. Those
entries are refused and counted, and the count is shown to the user.

## Room is not here yet

The plan names Room for the index. It is not in this step, for one reason: KSP
is what Room needs, AGP 9 compiles Kotlin itself now, and the KSP version has
to match the Kotlin version that AGP brings. That pairing cannot be tested on
the machine that wrote this step, which cannot reach the Android build servers.
Guessing it would have cost another round of red CI for a part that is, by
design, a throwaway cache.

`LibraryIndex` holds the same shape Room would: a list of rows with a kind, a
path, a title, a time and a size, built by walking the folder. Swapping in Room
means replacing one object and keeping `IndexEntry` and `IndexSnapshot`.

Do it in a session where the build runs locally. KSP has supported AGP built-in
Kotlin since KSP 2.3.1. Read the Kotlin version out of the app log first: the
app writes `Kotlin <version>` at start-up for exactly this reason.

## What the owner must test on the tablet

1. Open Settings. The data folder path should be under `Android/data`.
2. Press "Back up". A file picker should open. Save the zip.
3. Open the zip on a computer. It should hold `notes/`, `journal/` and so on,
   with readable file names.
4. Press "Restore" and pick that zip. It should ask first, then say how many
   files it read.
5. Press "Rebuild index" and write down how long it takes.
6. Open the Dev screen and press "Time 500 files". Send me every number.
