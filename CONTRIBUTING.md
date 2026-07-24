# Contributing

Tickera is a portfolio project, but issues and pull requests are welcome — whether
that is a bug report, a question about an architectural decision, or a concrete
improvement.

## Opening an issue

- **Bug** — include the service name, the command you ran, and the output or
  error you saw.
- **Question** — if something in the code or the ADRs is unclear, opening an
  issue is a fine way to ask. The ADRs are meant to be living documents.
- **Feature idea** — describe the problem you are trying to solve, not just the
  solution. Check the "Limitations & next steps" section in the README first —
  several extensions are already on the list.

## Submitting a pull request

1. Fork the repo and create a branch from `main`.
2. Make your change. For code changes, run the relevant test group to confirm
   nothing is broken:

   ```bash
   make test-unit          # fast — no Docker needed
   make test-contract      # Pact consumer/provider round-trip
   make test-integration   # Testcontainers (needs Docker)
   ```

3. Keep commits focused. A commit should do one thing and have a message that
   explains *why*, not just *what*.
4. Open a PR with a short description of what changed and why. If it touches
   an architecture decision, reference or update the relevant ADR.

## What is in and out of scope

**In scope:** bug fixes, documentation improvements, test coverage gaps,
observability additions, performance notes, anything in the "Limitations &
next steps" section of the README.

**Out of scope:** changes that alter the core architecture without a written
rationale (if you think Axon should be replaced or the saga pattern changed,
open an issue and discuss it first — that is an ADR-level decision).

## Code style

Java code is checked by Checkstyle on every CI run (`./mvnw verify`). The
configuration is in `checkstyle.xml` at the repo root. There is no formatter
gate — standard IntelliJ or Eclipse formatting is fine.
