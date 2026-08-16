# CLAUDE.md

Read [`AGENT.md`](AGENT.md) — it is the canonical guide for this repo (what the
project is, layout, build tooling, conventions). This file only repeats the
things that are expensive to get wrong.

## Non-negotiables

**Git identity.** This is a *personal* repo. Commits must use Yves's personal
address, not the `yves@dagger.io` one used on his work repos:

```sh
git config user.name  "Yves Brissaud"
git config user.email "yves.brissaud@gmail.com"
```

Local to this repo only, never `--global`. `Signed-off-by:` trailers use the
same personal address.

**No app code yet, but the design is finished.** Scope is settled in
[ADR 5](docs/decisions/0005-v1-scope.md) and [ADR 6](docs/decisions/0006-device-state-effects.md),
and every platform mechanism it depends on was verified on the real Pixel 6 —
the throwaway spike that proved them has been deleted. Next step is building v1.

**No AI attribution.** Not in commits, patches, PR titles or bodies. No
`Co-Authored-By` lines for agents.

**No pushing** without explicit approval from Yves.

## Working here

- Design docs are self-contained HTML with embedded mockups — serve them
  locally and open in a browser rather than pasting prose summaries.
- `dagger check` is the entrypoint for verification.
- Comments explain *why*, never *what*. Default to no comment.
