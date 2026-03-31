# UI Ownership Matrix

This matrix defines accountability for UI placement cleanup.

## Ownership Model

- **Area Owner**: accountable for triage quality and technical direction for a UI area.
- **Review Owner**: validates architectural boundaries and migration safety.
- **Decision Owner**: resolves exceptions/blockers quickly.

## Matrix Template

| UI Area | Area Owner | Review Owner | Decision Owner | Backup | Notes |
|---|---|---|---|---|---|
| `ui/screen/library/**` | TBD | TBD | TBD | TBD | Includes app screen action migration |
| `ui/screen/settings/**` | TBD | TBD | TBD | TBD | Includes auth/settings event flow |
| `ui/GameGrubMain.kt` | TBD | TBD | TBD | TBD | Entry orchestration decomposition |
| `ui/screen/xserver/**` | TBD | TBD | TBD | TBD | Legacy integration exception area |
| `ui/component/**` | TBD | TBD | TBD | TBD | Reusable component boundary enforcement |
| `ui/utils/**` | TBD | TBD | TBD | TBD | Helper classification cleanup |

## SLA Guidance

- Triage to owner assignment: within 2 working days.
- In-progress item update cadence: at least once per week.
- Blocked item escalation: within 1 working day of block detection.

## Exception Governance

For any intentional boundary exception:

- Record the exception in `ui-element-triage-register.md` with a rationale.
- Add an expiration condition (date, milestone, or dependency).
- Assign a named owner for removal or reevaluation.

