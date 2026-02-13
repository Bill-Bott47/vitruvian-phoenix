# Project Phoenix MP

## What This Is

Kotlin Multiplatform app for controlling Vitruvian Trainer workout machines (V-Form, Trainer+) via BLE. Community rescue project keeping machines functional after company bankruptcy. Supports Android (Compose) and iOS (SwiftUI) from a shared KMP codebase.

## Core Value

Users can connect to their Vitruvian trainer and execute workouts with accurate rep counting, weight control, and progress tracking — reliably, on both platforms.

## Requirements

### Validated

<!-- Shipped and confirmed valuable. -->

- ✓ BLE connection to V-Form (`Vee_*`) and Trainer+ (`VIT*`) devices — v0.1
- ✓ 6 workout modes (Old School, Eccentric, etc.) with real-time metric display — v0.1
- ✓ Rep counting via machine + position-based phase detection — v0.2
- ✓ Exercise library with muscle groups, equipment, video support — v0.2
- ✓ Routines with supersets, set/rep/weight tracking — v0.2
- ✓ Personal records with 1RM calculation (Brzycki, Epley) — v0.3
- ✓ Gamification: XP, badges, workout streaks — v0.3
- ✓ Training cycles with day rotation — v0.3
- ✓ Cloud sync infrastructure — v0.4
- ✓ MainViewModel decomposition into 5 managers (History, Settings, BLE, Gamification, WorkoutSession) — v0.4

### Active

<!-- Current scope. Building toward these. -->

- [ ] DefaultWorkoutSessionManager decomposed into focused sub-managers
- [ ] Circular dependency between BleConnectionManager and WorkoutSessionManager eliminated
- [ ] Koin DI wiring updated — managers injected directly, MainViewModel thinned or removed
- [ ] Large UI composables (WorkoutTab, HistoryAndSettingsTabs) decomposed into focused screens
- [ ] Characterization test suite covering workout lifecycle, state transitions, rep counting
- [ ] Testing foundation enabling safe future refactoring

### Out of Scope

<!-- Explicit boundaries. Includes reasoning to prevent re-adding. -->

- KableBleRepository decomposition — works reliably, refactoring risk outweighs benefit
- Premium features (data foundation, biomechanics, intelligence) — deferred to post-cleanup milestone
- New user-facing features — this milestone is purely architectural
- iOS-specific UI work — focus is shared module and Android Compose layer
- BLE protocol changes — no hardware interaction changes

## Context

- App is at v0.4.0, actively used by community
- MainViewModel went from 4,771 lines to 420-line facade via 5 manager extractions
- DefaultWorkoutSessionManager (4,024 lines) absorbed the biggest cluster without further decomposition
- UI monoliths remain: WorkoutTab (2,840L), HistoryAndSettingsTabs (2,750L), InsightCards (1,492L)
- Test fakes exist for all major dependencies (BleRepository, WorkoutRepository, etc.)
- Characterization test plan documented but tests may not be written yet
- OpenSpec specs (00-05) drafted for future premium features — this cleanup prepares the foundation
- Refactoring analysis docs exist in `.planning/refactoring/` (archaeologist, architect, surgeon, UI decoupler, safety inspector)

## Constraints

- **Platform**: KMP shared module — all business logic must remain in commonMain
- **Compatibility**: No breaking changes to existing workout behavior — characterization tests first
- **BLE stability**: Do not touch KableBleRepository or BLE protocol code
- **Incremental**: Each refactoring phase must leave the app in a buildable, working state

## Current Milestone: v0.4.1 Architectural Cleanup

**Goal:** Complete the architectural decomposition started in v0.4.0 — break up remaining monoliths (DefaultWorkoutSessionManager, UI screens), fix DI wiring, and establish a testing foundation for safe future development.

**Target features:**
- DefaultWorkoutSessionManager decomposition
- DI/circular dependency resolution
- UI monolith decomposition
- Characterization test suite

## Key Decisions

<!-- Decisions that constrain future work. Add throughout project lifecycle. -->

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Manager extraction pattern (scope injection, interface-based) | Enables testability, preserves ViewModel lifecycle | ✓ Good |
| MainViewModel as thin facade during transition | Preserves UI API while extracting logic incrementally | — Pending |
| Leave KableBleRepository alone | Works reliably, high risk/low reward to refactor | — Pending |
| Characterize before refactoring | Tests lock in behavior, catch regressions during extraction | — Pending |

---
*Last updated: 2026-02-12 after milestone v0.4.1 initialization*
