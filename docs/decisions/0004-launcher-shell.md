# 0004. Launcher shell

Date: 2026-09-19
Status: accepted
Step: 3

## Navigation

No navigation library. `LauncherRoute` is an enum and the host is a `when`.

Reasons:

1. A launcher has seven screens. A back stack would fight the Home key, which
   is meant to land on Today whatever was open.
2. A `when` cannot play a transition, so e-ink rule 1 holds by construction
   rather than by remembering to pass `EnterTransition.None` everywhere.
3. Less to load at start-up. The launcher process has to start fast.

The route is held on the activity, not inside the composition, because
`onNewIntent` has to reset it and `onNewIntent` is an activity callback.

## Home key and Back key

`android:launchMode="singleTask"` means the Home key brings the existing task
forward and calls `onNewIntent`. That handler sets the route back to Today.

`BackHandler(enabled = true)` is always on:

- On any tab it goes to Today.
- On Today it does nothing. A home screen has nothing behind it.

`android:stateNotNeeded="true"` lets the system kill and restore the launcher
without saved state, which is what a home app should tolerate.

## Reader and editors in their own activities

From Step 6 on, the reader and the editors get their own activities. If they
lived inside the launcher task, the Home key would close the book the user was
reading. `DemoActivity` already follows that shape.

## Apps tab

`LauncherApps` is the API meant for home apps. Every call is wrapped, and there
is a `PackageManager.queryIntentActivities` fallback, because a launcher that
cannot list apps leaves the user with no way to reach anything.

The tab has two pages, chosen with two controls:

- **Pinned**: up to 8 pinned apps and the ways out.
- **All apps**: the whole list, A to Z, paginated at 8 rows a page.

Two pages rather than one, because 640 dp of height cannot hold 8 pinned rows,
the ways out and a full list at the 56 dp minimum target size.

Pins live in DataStore as one newline separated string, because a preference
set does not keep an order and the order is what the user chose.

## Never trap the user

Plan section 3.2. The Pinned page always shows an "Always available" block:

- every other home app that is installed, so the stock launcher is reachable,
- any app whose package name contains "viwoods", which is how this build finds
  the ViWoods settings app,
- the standard Android settings app, when this build has one.

The ViWoods settings package name is a guess by name, because it is not
documented. **The owner has to confirm it on the tablet.** Once the real name
is known, replace the name match with the exact package.

## Set as home app

Three ways, in order, exactly as the plan says:

1. `RoleManager.ROLE_HOME` through `createRequestRoleIntent`.
2. `Settings.ACTION_HOME_SETTINGS`, then `Settings.ACTION_SETTINGS`.
3. Written steps using DevCheck, shown inside the app.

Each step logs what it tried, so the log file shows which one this tablet took.
