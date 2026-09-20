# Making a release

A release is a set of APK files on the GitHub Releases page. A GitHub Actions
workflow makes them when a tag is pushed. The app on the tablet finds the
release by itself: Settings, Help, "Check for updates".

## Each release: one command

```
tools/release.sh 0.1.1 "What changed, in one line."
```

It does five things:

1. Sets `appVersionName` in `app/build.gradle.kts`. The version code is worked
   out from it, so there is no second number to raise.
2. Adds a `## 0.1.1` part to the top of `docs/RELEASE_NOTES.md`. For more than
   one line, write that part yourself first and leave the second argument out.
3. Commits both files.
4. Makes the tag `v0.1.1`.
5. Pushes the branch and the tag.

Then watch the "Release" workflow. It takes about 15 minutes. When it is
done, press "Check for updates" on the tablet.

The script refuses to run with changes that are not committed, with a version
lower than the current one, and with a tag that exists already. The workflow
refuses a tag that does not match `appVersionName`.

## What a release holds

| Release key in the GitHub secrets | Files | Kind |
| --- | --- | --- |
| No | `eink-launcher-v0.1.1-viwoods-debug.apk`, `...-generic-debug.apk` | Pre-release |
| Yes | The two above, plus `...-viwoods.apk` and `...-generic.apk` | Release |

Plus `SHA256SUMS.txt` every time.

A debug file is signed with the fixed key in `keystore/debug.keystore`. A
release file is signed with the release key. **One does not install over the
other.** The updater knows this: a debug build only ever takes a debug file,
and a release build only ever takes a release file and ignores pre-releases.

While the app is being tested, the tablet runs the debug build. The device
test, the ink baseline and the storage measurement are in the debug build
only, and no release key is needed.

Do not change the file names without changing `UpdateAssets` in
`core/update/ReleaseInfo.kt`. The app picks its file by the end of the name.

## A private repository

GitHub answers "not found" for the releases of a private repository. Two
ways out:

1. Make the repository public. Nothing else to do.
2. Keep it private and give the app an access token, once. On GitHub:
   Settings, Developer settings, Personal access tokens, Fine-grained tokens,
   "Generate new token". Repository access: only this repository. Permissions:
   Contents, read-only. On the tablet: Settings, Help, "Check for updates",
   "Access token". The token stays on the tablet, outside every backup, and
   is only ever sent to `api.github.com`.

## One time, before the first public release: the release key

The release key is not in this repository and must never be. Whoever holds it
can publish an update that installs over the app. If it is lost, every user
has to uninstall and install again. Keep a copy somewhere safe.

On the dev machine:

```
keytool -genkeypair -v \
  -keystore ~/eink-launcher-release.keystore \
  -alias einklauncher \
  -keyalg RSA -keysize 4096 -validity 10000
```

It asks for a password. Write it down.

In the repository on GitHub: Settings, then "Secrets and variables", then
Actions, then "New repository secret". Make these four.

| Secret | Value |
| --- | --- |
| `EINK_RELEASE_KEYSTORE_BASE64` | The output of `base64 -w0 ~/eink-launcher-release.keystore` |
| `EINK_RELEASE_STORE_PASSWORD` | The keystore password |
| `EINK_RELEASE_KEY_ALIAS` | `einklauncher` |
| `EINK_RELEASE_KEY_PASSWORD` | The key password. `keytool` uses the keystore password unless told otherwise. |

Do not make a public release before the device tests in `PROGRESS.md` have
passed on the tablet. A launcher that fails is a tablet that is hard to use.

## A signed build on the dev machine

```
export EINK_RELEASE_STORE_FILE=~/eink-launcher-release.keystore
export EINK_RELEASE_STORE_PASSWORD=...
export EINK_RELEASE_KEY_ALIAS=einklauncher
export EINK_RELEASE_KEY_PASSWORD=...
./gradlew assembleViwoodsRelease
```

Without those four values the same command makes an unsigned APK, which
Android will not install.

## Things to know

- Moving the tablet from the debug build to the release build means an
  uninstall, and an uninstall deletes the data folder. Back up first:
  Settings, Storage, "Back up". Restore after the new install.
- Right after an update Android may show the stock launcher, because for a
  moment this app was not there to be the home screen. Press the Home key.
- A build from the dev machine and a build from GitHub of the same version
  have the same version code, so either installs over the other. A lower
  version never installs over a higher one.
- The `viwoods` flavour targets SDK 30 and cannot go to the Play Store. The
  `generic` flavour can. See `docs/decisions/0007-viwoods-ink-and-refresh.md`.
- Code shrinking (R8) is off. See the comment in `app/build.gradle.kts`.
