# 0022. A release key, GitHub only, no Play Store

Date: 2026-09-22. Decided by the owner.

## The decision

- **1.0.0 is the first full release.** The owner said the testing phase is
  over.
- **Every release file is signed with the release key.** The owner holds
  the key. It is not in the repository. `docs/RELEASING.md` says where it
  lives and how the workflow gets it.
- **A release holds no debug file any more.** A debug file is signed with
  `keystore/debug.keystore`, which is public on purpose so that test builds
  from any machine install over one another. But a public key means anyone
  can sign a file that installs over the app. Nobody should install such a
  file. The workflow still makes debug files when there is no key, for a
  fork.
- **The workflow checks each file before it publishes it:** it must be
  signed, and with the key whose SHA-256 fingerprint is written in
  `.github/workflows/release.yml`. With a secret missing, Gradle makes an
  unsigned file and says nothing. This stops that file.
- **The test releases 0.1.0 to 0.4.0 were removed from GitHub.** They held
  debug files only. Their git tags stay, and so do their notes in
  `docs/RELEASE_NOTES.md`.
- **The app is on GitHub Releases only.** Not on Google Play. The e-ink
  community installs APK files, and the app updates itself. Play would
  refuse the `viwoods` flavour, which targets SDK 30 for the fast pen, and
  Play does not allow an app to update itself (decision 0012). F-Droid stays
  open for later.
- **The owner registers with Google's developer verification.** From 2027,
  a certified Android device refuses an app from a developer who has not
  registered, even as an APK file. The owner registers a personal account
  and the package name `io.github.foxesrcool1.margin` with the fingerprint
  of the release key.

## What it costs

- A tablet with a test build cannot update to 1.0.0. It needs back up,
  uninstall, install, restore, once. The README says so.
- A debug build no longer finds updates on GitHub. That is fine: debug
  builds are for development, and `tools/deploy.sh` still serves them.
- If the release key is lost, no update can reach anyone. The owner keeps a
  copy away from the dev machine.
- If F-Droid ever builds the app, it signs with its own key, which is a
  second key for the same package name. Google's pages say nothing about
  that case yet. Look again before F-Droid.
