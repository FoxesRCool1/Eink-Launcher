# Making a release

A release is two APK files on the GitHub Releases page: `viwoods` for ViWoods
tablets and `generic` for any other Android device. A GitHub Actions workflow
makes them when a tag is pushed.

Do not tag a release before the device tests in `PROGRESS.md` have passed on
the tablet. A launcher that fails is a tablet that is hard to use.

## One time: make the release key

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

## One time: give the key to GitHub

In the repository on GitHub: Settings, then "Secrets and variables", then
Actions, then "New repository secret". Make these four.

| Secret | Value |
| --- | --- |
| `EINK_RELEASE_KEYSTORE_BASE64` | The output of `base64 -w0 ~/eink-launcher-release.keystore` |
| `EINK_RELEASE_STORE_PASSWORD` | The keystore password |
| `EINK_RELEASE_KEY_ALIAS` | `einklauncher` |
| `EINK_RELEASE_KEY_PASSWORD` | The key password. `keytool` uses the keystore password unless told otherwise. |

## Each release

1. Raise `versionCode` by one and set `versionName` in `app/build.gradle.kts`.
   Android refuses an update whose `versionCode` did not go up.
2. Write what changed at the top of `docs/RELEASE_NOTES.md`.
3. Merge to the default branch. Wait for CI to go green.
4. Tag and push:

   ```
   git tag v0.1.0
   git push origin v0.1.0
   ```

5. Watch the "Release" workflow. When it is done, the release is on the
   Releases page with both APK files and a `SHA256SUMS.txt`.

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

- A debug build and a release build have different keys. One does not install
  over the other. Uninstall first, and back up first: Settings, Storage,
  "Back up".
- The `viwoods` flavour targets SDK 30 and cannot go to the Play Store. The
  `generic` flavour can. See `docs/decisions/0007-viwoods-ink-and-refresh.md`.
- Code shrinking (R8) is off. See the comment in `app/build.gradle.kts`.
