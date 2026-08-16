# Build tooling

[Dagger](https://dagger.io) runs the build. Engine pinned to **v1.0.0-beta.9**
in `.dagger/modules/focus/dagger-module.toml`.

## What exists today

A deliberately minimal skeleton, sized to the fact that **there is no app code
yet**. Its only job is to prove the mechanism works against the pinned engine
so that the real pipeline has somewhere to land.

```
dagger.toml                          workspace: dang SDK + the focus module
.dagger/modules/focus/
  dagger-module.toml                 name + engineVersion pin + dang runtime
  main.dang                          `check` (smoke) and `tree` (introspection)
```

```sh
dagger check              # ✔ focus:check
dagger call focus tree    # the repo as the module sees it
```

`focus:check` currently asserts only that the workspace mounts and a container
runs. That is the point: it is a placeholder holding the shape of the real
check, not a pretend pipeline for code that does not exist.

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

## Planned shape, once the app exists

Deliberately not built yet. Recorded here so the intent survives.

```
.dagger/modules/
  android-sdk/     (dang)  base container: JDK + cmdline-tools + platform +
                           build-tools, licences accepted, pinned versions
  gradle/          (dang)  generic Gradle runner on top of android-sdk:
                           wrapper validation, Gradle + AGP cache volumes,
                           `assemble*` / `test` / `lint` as functions
  focus/           (dang)  composes the above into the repo's real surface:
                             check    unit tests + lint + detekt/ktlint
                             build    debug + release APK artifacts
                             sign     release signing from a secret keystore
                             release  versioned APK for GitHub Releases
```

Splitting `android-sdk` and `gradle` out is worth it because the Android SDK
container is slow to build and highly cacheable, while the Gradle runner is
generic enough to be reused unchanged. `focus` stays thin: it wires them
together and exposes the verbs.

Two things that will need care when they land:

- **Android SDK licences.** `sdkmanager` needs them accepted
  non-interactively; that belongs in the `android-sdk` module so it is done
  once and cached, not per build.
- **Signing keys.** Release signing needs a keystore as a Dagger secret, never
  a file in the repo. Debug builds use the standard debug keystore and need no
  secret at all — which covers everything until there is something to
  distribute.
