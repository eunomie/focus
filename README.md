# focus

A focus-oriented Android launcher for a Pixel 6.

Hit a button on the normal home screen and the phone swaps to a smaller,
quieter launcher — a few apps, no notifications, nothing else. Hit it again and
the normal launcher comes back.

Built for one phone and one person. Not a product.

## Status

**v1 built and running** on a Pixel 6 (Android 17). Every platform mechanism it
relies on was proven on the device first.

- **[Design doc + mockups](https://eunomie.github.io/focus/design/focus-launcher.html)** — the shape of the thing, with
  phone mockups and diagrams.
- [Decisions](docs/decisions/) — ADRs.
- [Build tooling](docs/build-tooling.md) — Dagger, pinned to v1.0.0-beta.9.

## Development

```sh
dagger check                                       # run all checks
dagger call focus apk export --path=/tmp/focus.apk # build the app
```

Installing needs a one-time set of grants — see [AGENT.md](AGENT.md).

See [AGENT.md](AGENT.md) for the full orientation.

## Licence

MIT — see [LICENSE](LICENSE).
