# 0015. Folders on the Apps tab

Date: 2026-09-20. Asked for by the owner, to sort apps into groups.

- **The Apps tab has three pages**, picked with three icons: pinned, folders,
  and every app from A to Z. Hold an app and choose Folders to put it into
  folders, or to make a folder for it.
- **A folder is a label, not a place.** One app can be in more than one
  folder. "Reading" and "Offline" can both be true of it.
- **The folders are a plain file, `apps/folders.json`, not a setting.** The
  user makes them by hand, one app at a time. That work belongs in a backup
  and should come back on a new tablet. The pinned apps stay in the settings:
  they are eight taps to make again.
- **An app that is uninstalled stays in the file.** It is not shown, and it is
  back in its folders when it is installed again. That is what happens on a
  restore to a new tablet.
- **A file that cannot be read is kept**, as `folders.unreadable.json`, before
  a new one is written. The same rule as `habits.json`.
- The ways out of the launcher share the pinned page, as rows with an icon.
  That page is a paged list now. Before, eight pins and three ways out were
  taller than the screen.
- `core/apps/` holds the rules and the file format, with no Android in it, and
  `AppFoldersTest` checks every rule.
