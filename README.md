# focus

A focus-oriented Android launcher for a Pixel 6.

Hit a button on the normal home screen and the phone swaps to a smaller,
quieter launcher — a few apps, no notifications, nothing else. Hit it again and
the normal launcher comes back.

Built for one phone and one person. Not a product.

## Status

**Scope settled.** No app code yet — the platform spike is built and waiting to
be run on the phone. See the [runbook](docs/spike.md).

- **[Design doc + mockups](docs/design/focus-launcher.html)** — open in a
  browser; self-contained HTML.
- [Decisions](docs/decisions/) — ADRs.
- [Build tooling](docs/build-tooling.md) — Dagger, pinned to v1.0.0-beta.9.
- [Spike runbook](docs/spike.md) — the on-device questions still open.

## Development

```sh
dagger check                                             # run all checks
dagger call focus spike-apk export --path=/tmp/spike.apk # build the platform spike
```

See [AGENT.md](AGENT.md) for the full orientation.

## Licence

MIT — see [LICENSE](LICENSE).
