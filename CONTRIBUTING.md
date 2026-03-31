# Contributing to GameNative

Thanks for your interest in contributing! We welcome pull requests, bug reports, and feedback.

## Getting Started

1. Join our [Discord server](https://discord.gg/2hKv4VfZfE) to discuss what you're working on
2. Fork the repo and create a branch for your changes
3. Pick or create a local ticket in `todo/INDEX.md` (details in `todo/README.md`)
4. See the **Building** section in the [README](README.md) for setup instructions
5. Submit a pull request with a clear description of what your change does and why

## Local Ticket Workflow

- Tickets are tracked in-repo under `todo/`.
- Use `todo/TICKET_TEMPLATE.md` for new tickets.
- Keep ticket status current (`Backlog`, `In Progress`, `Blocked`, `Done`).
- Link ticket ID in your PR description and update ticket links after merge.
- If a ticket is too large, split it into child tickets immediately and cross-link them.
- If new required work appears while implementing, create follow-up ticket(s) immediately.
- Do not leave an `In Progress` ticket orphaned; finish it or move it to `Blocked` with a concrete reason.

## Documentation Requirement

Every change must include documentation updates.

- Update the most relevant docs for the change (`docs/`, `README.md`, `AGENTS.md`, ticket notes, or code comments where appropriate).
- In your PR, include a short **Documentation Impact** note describing what was updated.
- If a change truly requires no doc edits, include **Documentation Impact: No doc changes required** with a clear reason.

## Completion and Review Requirement

- When implementation is complete, commit your changes before requesting review.
- A different agent or reviewer must check the work before finalizing.
- After review updates, ensure all changes are committed before marking the ticket `Done`.
- Record improvement opportunities found during implementation/review in `docs/process-improvement-log.md`.

## PR Checklist

- [ ] Ticket linked from `todo/INDEX.md` and ticket file updated.
- [ ] Documentation updated for this change.
- [ ] PR description includes a `Documentation Impact` section.
- [ ] Implementation committed before review.
- [ ] Independent review completed and outcome captured.
- [ ] All post-review changes committed.
- [ ] Improvement opportunities captured in `docs/process-improvement-log.md`.
- [ ] Validation steps/results are included.

## Reporting Issues

Please report bugs and request support through our [Discord server](https://discord.gg/2hKv4VfZfE). GitHub issues will be automatically closed.

## Contribution Licensing

By submitting a contribution to GameNative, you agree that your contribution shall be licensed under the same license(s) as the project at the time of contribution.

You also grant the project maintainers the right to relicense the project, including your contribution, under any future version of those license(s), or under any other license that is free and open source software (FOSS) and compatible with the current license(s).

You retain full copyright ownership of your contributions. You confirm that your contributions are your original work and that you have the right to submit them under these terms.
