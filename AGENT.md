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

The **platform spike** is written and builds to an APK, but has not been run on
the phone yet — that needs a physical Pixel 6, so it is Yves's step, not an
agent's. It answers the only questions that could still change the architecture:
whether `ROLE_HOME` can be taken and handed back, how much the
gesture-navigation tax hurts, and whether greyscale and the notification
listener behave. **Don't start the real app before those answers exist**, and
record them in ADR 4 when they do. Runbook: [`docs/spike.md`](docs/spike.md).

What exists today:

- The design doc + mockups (`docs/`), and the settled scope in ADR 5.
- A working Dagger toolchain: `.dagger/modules/android/` (Android SDK + Gradle
  in a container) and `.dagger/modules/focus/`. See
  [`docs/build-tooling.md`](docs/build-tooling.md).
- `spike/` — the throwaway platform probe. Builds to an installable APK.
  Runbook: [`docs/spike.md`](docs/spike.md).

## Repo layout

```
AGENT.md                   this file
CLAUDE.md                  pointer to this file + the non-negotiables
dagger.toml                Dagger workspace: dang SDK + local modules
.dagger/modules/android/   Android SDK + Gradle toolchain (dang)
.dagger/modules/focus/     the repo's own Dagger module (dang)
spike/                     throwaway platform probe (delete after the spike)
docs/
  README.md                doc conventions
  build-tooling.md         Dagger toolchain: what exists, what's planned
  spike.md                 platform spike runbook
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
dagger check                                             # run all checks
dagger call android versions                             # what's in the toolchain
dagger call focus spike-apk export --path=/tmp/spike.apk # build the platform spike
```

There is no build for the *real* app yet, because there is no real app code
yet — only the spike.

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
