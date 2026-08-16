# focus — agent guide

Read this first. It is the canonical orientation file for the repo; `CLAUDE.md`
points here.

## What this project is

`focus` is a **focus-oriented Android launcher for a Pixel 6**, for a single
user (Yves). It is not a general-audience product.

The one locked-in requirement:

> From the phone's normal launcher, hit a button and the phone swaps to a
> smaller, lighter, focus-oriented launcher — minimal apps, minimal
> notifications — with a clear way back to the classic launcher.

Everything else is being designed. The current design lives in
[`docs/design/focus-launcher.html`](docs/design/focus-launcher.html) — open it
in a browser, it is a self-contained HTML doc with mockups and diagrams.

## Status

**Scope settled, no app code yet.** The design has been reviewed and all eight
open questions answered — see [ADR 5](docs/decisions/0005-v1-scope.md) for the
answers, which are the authoritative statement of what v1 is.

The platform spike is **done and deleted** — every mechanism the design depends
on was verified on the real Pixel 6 running Android 17. The results are in
[ADR 4](docs/decisions/0004-focus-holds-the-home-role-while-active.md) and
[ADR 6](docs/decisions/0006-device-state-effects.md), and those ADRs say where
in git history to find the working probe code.

**Nothing in the design now rests on an unverified assumption.** What remains is
building v1 as scoped in [ADR 5](docs/decisions/0005-v1-scope.md) and ADR 6.

What exists today:

- The design doc + mockups (`docs/`), and the settled scope in ADR 5 + ADR 6.
- Six ADRs, all Accepted, all backed by on-device measurement where they make a
  platform claim.
- A working Dagger toolchain: `.dagger/modules/android/` (Android SDK + Gradle
  in a container) and `.dagger/modules/focus/`. See
  [`docs/build-tooling.md`](docs/build-tooling.md).

## Repo layout

```
AGENT.md                   this file
CLAUDE.md                  pointer to this file + the non-negotiables
dagger.toml                Dagger workspace: dang SDK + local modules
.dagger/modules/android/   Android SDK + Gradle toolchain (dang)
.dagger/modules/focus/     the repo's own Dagger module (dang)
docs/
  README.md                doc conventions
  build-tooling.md         Dagger toolchain: what exists, what's planned
  design/                  design docs (HTML, self-contained, with mockups)
  decisions/               ADRs — short records of decisions and their why
```

Why this layout, and why not `docs/` for design docs elsewhere: see
[`docs/README.md`](docs/README.md).

## Build tooling

[Dagger](https://dagger.io) v1.0.0-beta.9, pinned in
`.dagger/modules/focus/dagger-module.toml` (`engineVersion`). Modules are
written in **dang**; the **Java SDK** (`dagger/java-sdk`) is the fallback for
modules that need more logic than dang expresses comfortably.

```sh
dagger check                  # run all checks
dagger call android versions  # what's in the toolchain
```

The `android` module builds a JDK 21 + Android SDK 36 + Gradle container and
exposes `gradle(source, task)`. It has no consumer right now — the spike that
exercised it is gone and the app has not landed — so the first job when app code
appears is wiring `focus.apk` back onto it.

## Working with the phone

Installing on the Pixel needs no cable. On the phone: Settings → System →
Developer options → **Wireless debugging** → on → **Pair device with pairing
code**.

```sh
adb pair <ip>:<pairing-port>    # port from inside the pairing dialog
adb connect <ip>:<connect-port> # port from the main Wireless debugging screen
```

The two ports differ, and the connect port **rotates** whenever the phone sleeps
or that screen closes — a dropped connection usually just needs a new
`adb connect`, not a re-pair. Don't use `adb -d`; that means USB.

The permissions the app needs beyond the normal ones are granted once over adb,
never at runtime:

```sh
adb shell pm grant <pkg> android.permission.WRITE_SECURE_SETTINGS
adb shell pm grant <pkg> android.permission.READ_MEDIA_IMAGES
adb shell cmd notification allow_listener <pkg>/<pkg>.NotificationListener
adb shell cmd notification allow_dnd <pkg>
```

## Git identity — important

This is Yves's **personal** repo. Commits here must use his **personal**
address, which is deliberately different from the `yves@dagger.io` identity used
on his work repos:

```sh
git config user.name  "Yves Brissaud"
git config user.email "yves.brissaud@gmail.com"
```

Set locally in this repo only — never `--global`. `Signed-off-by:` trailers must
read `Signed-off-by: Yves Brissaud <yves.brissaud@gmail.com>`.

## Conventions

- Small, deliberate commits. Stage specific paths; no blind `git add -A`.
- Never `git push` without explicit approval.
- No AI attribution anywhere — not in commit messages, patch descriptions, PR
  titles or bodies. No `Co-Authored-By` lines for agents.
- Comments explain *why*, never *what*. Default to no comment.
- Prefer the smallest change that solves the problem.
