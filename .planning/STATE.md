# Project State

## Project Reference

See: .planning/PROJECT.md (updated 2026-02-12)

**Core value:** Users can connect to their Vitruvian trainer and execute workouts reliably on both platforms.
**Current focus:** Phase 1 — Characterization Tests

## Current Position

Phase: 1 of 4 (Characterization Tests)
Plan: 0 of 2 in current phase
Status: Ready to plan
Last activity: 2026-02-12 — Roadmap created for v0.4.1 Architectural Cleanup

Progress: [░░░░░░░░░░] 0%

## Performance Metrics

**Velocity:**
- Total plans completed: 0
- Average duration: -
- Total execution time: 0 hours

**By Phase:**

| Phase | Plans | Total | Avg/Plan |
|-------|-------|-------|----------|
| - | - | - | - |

**Recent Trend:**
- Last 5 plans: -
- Trend: N/A

*Updated after each plan completion*

## Accumulated Context

### Decisions

Decisions are logged in PROJECT.md Key Decisions table.
Recent decisions affecting current work:

- [Roadmap]: Managers stay OUT of Koin (manual construction in MainViewModel) — lifecycle mismatch with viewModelScope
- [Roadmap]: Concrete classes for sub-managers, not interfaces — test through DWSM public API with fake repos
- [Roadmap]: WorkoutCoordinator is a dumb state bus with zero methods — prevents it from becoming new monolith
- [Roadmap]: BLE commands stay co-located with state transitions in ActiveSessionEngine — not a separate concern

### Pending Todos

None yet.

### Blockers/Concerns

- [Phase 2]: handleMonitorMetric() hot path (10-20Hz) performance must not regress — may need profiling
- [Phase 2]: Init block collector ordering must be documented before splitting across sub-managers
- [Phase 2]: SharedFlow event loss risk — inventory all shared flows before extraction

## Session Continuity

Last session: 2026-02-12
Stopped at: Roadmap and state initialized for v0.4.1
Resume file: None
