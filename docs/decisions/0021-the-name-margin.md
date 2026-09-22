# 0021. The name is Margin

Date: 2026-09-22. Chosen by the owner.

## The decision

The app is called Margin. The working name "Eink Launcher" is gone from the
app, the code, the tools and the current docs. It stays in `PROGRESS.md` and
in the older decisions, which are history.

The full name for the repository, and for places that need to say what the
app is, is "Margin Eink Launcher". The GitHub repository is
`FoxesRCool1/Margin-Eink-Launcher`.

## What changed with it

- Package: `io.github.foxesrcool1.margin`. The application class is
  `MarginApp`, the theme is `Theme.Margin`.
- The data folder is `Android/data/io.github.foxesrcool1.margin/files/Margin/`,
  and the log copies go to `Download/Margin/`.
- The release files are `margin-<tag>-<flavour>.apk` and
  `margin-<tag>-<flavour>-debug.apk`. The updater picks its file by the end
  of the name, so this needed no code change. The release title is
  "Margin v1.2.3".
- The updater reads the releases of `FoxesRCool1/Margin-Eink-Launcher`. The
  repository must have that name, or the update check says "not found".
  GitHub redirects the old name for a while, but a new repository could take
  it, so the name in `app/build.gradle.kts` is the new one.
- A new package is a new app to Android. The tablet needs a backup of the
  old app's data folder, an uninstall, and a fresh install of Margin, then a
  restore. The 0.3.0 build cannot update itself into 0.4.0: its check will
  refuse the file as "a different app", which is the right answer.

## How the name was chosen

Eighteen paper and plant words were checked against Google Play, F-Droid,
IzzyOnDroid, GitHub and the trademark listings on justia.com and
trademarkia.com. Quire, Daybook, Fallow, Endpaper, Quarto and Wove are
registered marks or big apps. Flyleaf, Deckle, Seedling, Commonplace,
Looseleaf, Paperbark and Ream have live apps of the same name. Foolscap and
Onionskin were free. The owner chose Margin: the space at the edge of a page,
where notes go. Not a legal opinion, and no USPTO or EUIPO search was made.
