# 3. Dagger for build tooling, modules in dang

Date: 2026-08-16
Status: Accepted

## Context

An Android build needs Gradle, a pinned Android SDK, accepted SDK licences and
eventually release signing. Doing that on a laptop means "works on my machine";
doing it in CI only means a slow feedback loop.

## Decision

**Dagger, pinned to v1.0.0-beta.9**, as the build entrypoint. Modules written
in **dang**, with the **Java SDK** (`dagger/java-sdk`) as the per-module
fallback when dang stops being comfortable.

Only a skeleton is built now: one module, one smoke check. The real pipeline
waits for real app code.

## Consequences

The same command runs locally and in CI, and the Android SDK container is
built once and cached rather than installed by hand.

dang first because container plumbing is exactly what it is for — no codegen,
no compile step, the module is its source. The Java SDK is the escape hatch for
logic-heavy modules, and sits naturally next to a Kotlin/Gradle build on the
same JVM toolchain.

Building only a skeleton now is deliberate: a full pipeline for an app that
does not exist would be untested guesswork that has to be rewritten the moment
real code lands. The intended end shape is recorded in
[`docs/build-tooling.md`](../build-tooling.md) so the plan survives without
being prematurely committed to code.

Gradle stays the thing that actually compiles the app. Dagger orchestrates it;
it does not replace it.
