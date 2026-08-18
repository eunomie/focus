# Documentation

## Layout

```
docs/
  build-tooling.md   the Dagger toolchain: what exists now, what's planned
  design/            design docs — self-contained HTML, published via Pages
  decisions/         ADRs — one short file per decision, with its why
```

## Why this layout

Plain `docs/` at the repo root, not a bespoke split. This is a small personal
repo with one feature; a deeper convention (separate `hack/`, `future/`,
`specs/`, `rfcs/` trees) would be structure for its own sake. `docs/` is the
convention every reader and every tool already expects, so it costs nothing to
learn.

The two subdirectories earn their place because they hold genuinely different
things:

- **`design/`** — the shape of a feature *before* it is built. Long, visual,
  revised in place, superseded when the feature ships.
- **`decisions/`** — ADRs. Short, append-only, never rewritten. A design doc
  says "here is the plan"; an ADR says "we chose X over Y because Z, on this
  date". Keeping them apart means the record of *why* survives the churn of
  the plan.

## Why design docs are HTML

The design work here is inherently visual — phone-screen mockups, state
diagrams, side-by-side comparisons. Markdown renders none of that without a
toolchain, and a doc that needs a build step to be legible tends not to get
read. A self-contained HTML file with inline SVG opens in any browser, on any
machine, forever, with no dependencies.

It also means they publish as-is. `design/` is served by GitHub Pages, so
[focus-launcher.html](https://eunomie.github.io/focus/design/focus-launcher.html)
is readable without cloning anything — which a Markdown link to an HTML file in
the repo is not, since GitHub renders it as source.

ADRs stay Markdown: they are prose, they are short, and they benefit from
being readable in a diff.
