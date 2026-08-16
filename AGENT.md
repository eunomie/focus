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

**Design phase. There is no app code yet, on purpose.** The feature plan is
awaiting review. Do not start writing the Android app until that review has
happened.

What exists today:

- The design doc + mockups (`docs/`).
- A minimal, working Dagger toolchain skeleton (`dagger.toml`,
  `.dagger/modules/focus/`) that proves the build mechanism runs. It is
  deliberately a skeleton — see [`docs/build-tooling.md`](docs/build-tooling.md)
  for the shape it is expected to grow into.

## Repo layout

```
AGENT.md                   this file
CLAUDE.md                  pointer to this file + the non-negotiables
dagger.toml                Dagger workspace: dang SDK + local modules
.dagger/modules/focus/     the repo's Dagger module (dang)
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
dagger check              # run all checks
dagger call focus tree    # smoke test: the repo as the module sees it
```

There is no Gradle/Android build yet because there is no app code yet.

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
