# Build tooling

[Dagger](https://dagger.io) runs the build. Engine pinned to **v1.0.0-beta.9**
in `.dagger/modules/focus/dagger-module.toml`.

## What exists today

```
dagger.toml                          workspace: dang SDK + local modules
.dagger/modules/android/             Android SDK + Gradle toolchain
.dagger/modules/focus/               the repo's own surface
```

```sh
dagger check                                        # unit tests + Android lint
dagger call android versions                        # JDK / Gradle / SDK in the toolchain
dagger call focus apk export --path=/tmp/focus.apk  # debug APK
dagger call focus lint-report export --path=/tmp/lint.txt
```

The `android` module builds a JDK 21 container with pinned Android
cmdline-tools, platform 36, build-tools 36.0.0 and Gradle 8.14.3, accepts the
SDK licences in the same cached layer, and exposes a `gradle(source, task)`
runner with the Gradle home mounted as a cache volume. The `focus` module composes
it into `check`, `apk` and `release-apk`.

`focus:check` runs `:app:testDebugUnitTest` and `:app:lintDebug`. Lint is not
decoration: on its first real run it caught `AutomaticZenRule.Builder` being an
API 35 call under a `minSdk` of 34, which would have been a `NoSuchMethodError`
on any device older than the one it was developed against.

The tests cover the allowed-app list rules — the cap and the reordering
arithmetic — which are the only part of the app pure enough to test without a
device. `AllowedApps` exists as plain functions for exactly that reason.

## Language choice: dang first, Java SDK as fallback

Modules are written in **dang**. It is declarative, needs no codegen or compile
step, and the module *is* its source — for container plumbing (pick a base
image, mount caches, run a command, return an artifact) that is the whole job,
and dang says it in a few lines.

The **Java SDK** (`dagger/java-sdk`) is the fallback for modules that outgrow
that: parsing tool output, non-trivial control flow, anything wanting real data
structures. It is also the natural neighbour of a Kotlin/Gradle Android build —
same JVM toolchain, same mental model. The rule is *dang until it hurts, then
Java*, decided per module rather than repo-wide.

## One module, not two

The earlier plan here called for separate `android-sdk` and `gradle` modules.
That was written before there was anything to build; with one consumer the split
would have been an abstraction with a single caller, so `android` does both jobs
and will be split when a second consumer actually shows up.

The caching argument that motivated the split still holds and is met anyway: the
slow SDK layer is a distinct `toolchain` function, so it caches independently of
the Gradle invocations layered on top of it.

## Releases

```sh
export FOCUS_STORE_PASSWORD=… FOCUS_KEY_PASSWORD=…
dagger call focus release-apk \
  --keystore=file:/path/to/focus.jks \
  --store-password=env:FOCUS_STORE_PASSWORD \
  --key-password=env:FOCUS_KEY_PASSWORD \
  export --path=/tmp/focus-release.apk
```

The keystore is mounted as a Dagger secret and the passwords are secret
environment variables, so none of them land in the repository, the build cache
or an image layer. Gradle reads them through `FOCUS_*` env vars; when they are
absent the release variant is simply left unsigned rather than failing, which
keeps ordinary `assembleRelease` runs working.

Still open, and deliberately not built yet:

- **Code shrinking.** `isMinifyEnabled` is off. R8 with Compose usually works but
  can fail in ways only visible at runtime, and nothing yet depends on a smaller
  APK.
- **A lint baseline.** Eleven warnings remain, mostly dependency-version notices.
  Warnings do not fail the build; errors do.
- **detekt or ktlint.** Both are new Gradle plugins, so worth agreeing before
  adding rather than slipping one in.

Two things that need care:

- **Android SDK licences.** Accepted non-interactively inside the `android`
  module's `toolchain`, so it is done once and cached rather than per build.
- **No Gradle wrapper.** Gradle is pinned in the toolchain container instead,
  which keeps a binary jar out of the repo. Revisit if the app ever needs
  building outside Dagger.
- **The debug keystore lives in `~/.focus/debug.jks`, never in the repo.** A
  published debug key would let anyone build an APK with this application id and
  the same signature, and Android lets a matching signature update an installed
  app while inheriting its adb-granted permissions — which here includes
  `WRITE_SECURE_SETTINGS`. It reaches the build as a Dagger secret:

  ```sh
  dagger settings focus debugKeystore 'cmd:cat $HOME/.focus/debug.jks'
  ```

  The `cmd:` form keeps a machine-specific path out of the committed config.
  Without the setting Gradle falls back to its own generated debug key, which
  builds fine but is regenerated per container, so `adb install -r` over a
  previous build fails.
