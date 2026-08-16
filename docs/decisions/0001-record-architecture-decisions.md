# 1. Record architecture decisions

Date: 2026-08-16
Status: Accepted

## Context

This repo is starting from nothing. Decisions taken now — language, build
tooling, how the launcher hands control back to the system — will be invisible
in six months, and the reasoning behind them will be the first thing lost.

## Decision

Record decisions as short ADRs in `docs/decisions/`, numbered, append-only.

Each records the context, the decision, and the consequences. Superseded ADRs
are marked `Superseded by NNNN`, not deleted or edited — the record of a wrong
turn is worth as much as the record of a right one.

`Status` is one of `Proposed`, `Accepted`, `Superseded by NNNN`. `Proposed`
means the decision is written down but still waiting on review.

## Consequences

Design docs in `docs/design/` can stay focused on the plan and be rewritten
freely, because the durable *why* lives here instead.
