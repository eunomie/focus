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
dagger check                                            # ✔ focus:check
dagger call android versions                            # JDK / Gradle / SDK in the toolchain
dagger call focus spike-apk export --path=/tmp/spike.apk   # build the platform spike
```

The `android` module builds a JDK 21 container with pinned Android
cmdline-tools, platform 36, build-tools 36.0.0 and Gradle 8.14.3, accepts the
SDK licences in the same cached layer, and exposes a `gradle(source, task)`
runner with the Gradle home mounted as a cache volume. `focus` composes it into
`spikeApk`, which produces an installable debug APK.

`focus:check` still only asserts that the workspace mounts and a container runs.
It stays a placeholder until there is app code with real tests.

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

## Planned shape, once the real app exists

Deliberately not built yet. Recorded here so the intent survives.

```
focus  (dang)
  check     unit tests + lint + detekt/ktlint
  build     debug + release APK artifacts
  sign      release signing from a secret keystore
  release   versioned APK for GitHub Releases
```

Three things that need care:

- **Android SDK licences.** Accepted non-interactively inside the `android`
  module's `toolchain`, so it is done once and cached rather than per build.
- **No Gradle wrapper.** The spike project has none; Gradle is installed in the
  toolchain container at a pinned version instead, which keeps a binary jar out
  of the repo. Revisit if the app ever needs to be built outside Dagger.
- **Signing keys.** Release signing needs a keystore as a Dagger secret, never
  a file in the repo. Debug builds use the standard debug keystore and need no
  secret at all — which covers everything until there is something to
  distribute.
