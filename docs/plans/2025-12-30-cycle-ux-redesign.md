# Cycle UX Redesign

**Date:** 2025-12-30
**Status:** Design Complete

## Problem

The current Cycle implementation feels clunky across all phases:
- **Creating:** Multi-step template dialogs, buried routine assignment
- **Modifying:** No visual overview, awkward reordering, no drag-and-drop
- **Living with:** Rigid day progression, no flexibility to pick different days

The mental model is correct (rolling Day 1, Day 2, Day 3 rotation), but the UI makes customization tedious.

## Solution Overview

Redesign the Cycle experience with:
- Visual drag-and-drop scheduler
- Per-day modifier configuration
- Timeline review before saving
- Flexible day selection during active use
- Completion/missed tracking with 24h auto-advance

---

## Creation Flow

### Step 1: Day Count Picker

Entry point after tapping "Create Cycle."

```
┌─────────────────────────────────────┐
│ How many days in your cycle?        │
│                                     │
│ [7]  [14]  [21]  [28]  [Custom]     │
│                                     │
│ This is a rolling schedule —        │
│ Day 1 follows the last day          │
│ automatically.                      │
└─────────────────────────────────────┘
```

- Quick-select chips for common lengths
- Custom opens numeric input
- Tapping proceeds to Cycle Editor

### Step 2: Cycle Editor

Two-panel layout with ~30/70 split.

```
┌──────────────────────────────────────────────┐
│ New Cycle                             [Save] │
│ ──────────────────────────────────────────── │
│                                              │
│ ROUTINES          YOUR CYCLE                 │
│ ┌────────┐        ┌────────────────────────┐ │
│ │Push    │        │ Day 1   Push Day    ⋮  │ │
│ │Pull    │  ───►  │         [+5%] [Echo]   │ │
│ │Legs    │        │ Day 2   Pull Day    ⋮  │ │
│ │Upper   │        │         [Eccentric]    │ │
│ │Lower   │        │ Day 3   💤 Rest        │ │
│ │Full    │        │ Day 4   Leg Day     ⋮  │ │
│ └────────┘        │ Day 5   💤 Rest        │ │
│  ~30%             │ Day 6   Push Day    ⋮  │ │
│                   │ Day 7   Pull Day    ⋮  │ │
│                   └────────────────────────┘ │
│                         ~70%                 │
└──────────────────────────────────────────────┘
```

**Left Panel — Routine Palette:**
- Scrollable list of all saved routines
- Compact display (routine name only)
- Never depletes; dragging creates copies

**Right Panel — Day Slots:**
- Scrollable list of all days in cycle
- Shows day number + assigned routine + modifier badges
- Empty slots show 💤 Rest with Zzz icon

**Drag Interactions:**
| Action | Result |
|--------|--------|
| Drag routine → empty slot | Assigns routine to that day |
| Drag routine → assigned slot | Replaces existing routine |
| Drag day slot (via handle) | Reorders days in cycle |

### Day Slot States

**Assigned Day:**
```
┌────────────────────────────────┐
│ Day 3   Push Day          ⋮    │
│         [+5%] [Echo:Hard]      │
└────────────────────────────────┘
```
- Tap anywhere → Opens config popup
- Overflow menu (⋮) → Clear assignment, Duplicate to next empty
- Left edge drag handle → Reorder

**Empty Day (Rest):**
```
┌────────────────────────────────┐
│ Day 5   💤 Rest                │
└────────────────────────────────┘
```
- Tapping does nothing
- Drag a routine onto it to assign

---

## Per-Day Config Popup

Opens as bottom sheet when tapping an assigned day. **Context-aware** — only shows modifiers relevant to the routine's exercise modes.

```
┌─────────────────────────────────────┐
│ Day 3 — Push Day               [×]  │
├─────────────────────────────────────┤
│                                     │
│ ECHO LEVEL                          │
│ [Hard] [Harder] [Hardest] [Epic]    │
│                                     │
│ ECCENTRIC LOAD                      │
│ ──────────●────────── 75%           │
│                                     │
│ WEIGHT PROGRESSION                  │
│ ──────────●────────── +5%           │
│ "Each occurrence adds 5%"           │
│                                     │
│ REP MODIFIER                        │
│ [ -2 ] [ -1 ] [ 0 ] [ +1 ] [ +2 ]   │
│                                     │
│ REST TIME                           │
│ ──────────●────────── 90s           │
│                                     │
│              [Apply]                │
└─────────────────────────────────────┘
```

### Dynamic Visibility Rules

| Modifier | Shown When | Range/Options |
|----------|------------|---------------|
| Echo Level | Any exercise uses Echo mode | Hard, Harder, Hardest, Epic |
| Eccentric Load | Any exercise uses Echo mode | 0% - 150% slider |
| Weight Progression | Any exercise uses Old School mode | -50% to +50% slider |
| Rep Modifier | NOT all sets are AMRAP | -2, -1, 0, +1, +2 chips |
| Rest Time | Always | 0s - 300s slider |

**Examples:**
- Echo-only routine → Echo Level, Eccentric Load, Rep Modifier (unless all AMRAP), Rest Time
- Old School-only routine → Weight Progression, Rep Modifier, Rest Time
- Mixed routine → All options visible

### Weight Progression Behavior

Per-occurrence progression: each time this day comes around in the cycle, weights adjust by the set percentage from baseline.

---

## Timeline Review Screen

After tapping [Save], user sees a full preview before final confirmation.

```
┌─────────────────────────────────────────┐
│ Review Your Cycle              [Back]   │
│ ─────────────────────────────────────── │
│                                         │
│ ┌─────────────────────────────────────┐ │
│ │ Day 1                               │ │
│ │ Push Day  [+5%] [Echo:Hard]      ▾  │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ Day 2                               │ │
│ │ Pull Day  [Eccentric 80%]        ▾  │ │
│ ├─────────────────────────────────────┤ │
│ │  • Barbell Row      3×10            │ │
│ │  • Lat Pulldown     3×12            │ │
│ │  • Bicep Curl       4×10            │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ Day 3                               │ │
│ │ 💤 Rest                             │ │
│ └─────────────────────────────────────┘ │
│ ┌─────────────────────────────────────┐ │
│ │ Day 4                               │ │
│ │ Leg Day                          ▾  │ │
│ └─────────────────────────────────────┘ │
│                 ...                     │
│                                         │
│           [Save Cycle]                  │
└─────────────────────────────────────────┘
```

**Behavior:**
- Collapsed by default: Day number + routine name + modifier badges
- Tap row or ▾ → Expands to show exercises with sets/reps
- Rest days show 💤 Rest, no expand option
- [Back] → Returns to editor
- [Save Cycle] → Commits and navigates home

---

## Active Cycle Experience

### Main Workout Screen

```
┌─────────────────────────────────────────┐
│ UP NEXT                                 │
│ ─────────────────────────────────────── │
│                                         │
│  Day 3 — Push Day                       │
│  [+5%] [Echo:Hard]                      │
│                                         │
│  • Bench Press      3×10                │
│  • Shoulder Press   3×8                 │
│  • Tricep Dips      3×12                │
│                                         │
│ ─────────────────────────────────────── │
│                                         │
│  [✓1] [✓2] [●3] [✗4] [5] [💤] [7]      │
│   grn  grn  blu  red  gry gry  gry     │
│                                         │
│         [Start Workout]                 │
│                                         │
└─────────────────────────────────────────┘
```

### Day Strip States

| State | Visual | Color |
|-------|--------|-------|
| Completed | ✓ + number | Green |
| Missed | ✗ + number | Red |
| Current | ● + number (filled) | Blue/Primary |
| Upcoming | Number only (outline) | Gray |
| Rest | 💤 icon | Gray (or green if completed) |

**Interactions:**
- Tap any chip → Switches "UP NEXT" preview to that day
- Scrollable if cycle > 7 days
- Completing a workout advances marker to next day

### Flexibility Rules

- User can start any day's workout at any time
- No restrictions or warnings for picking non-current days
- Progress marker moves to completed day + 1

### Missed Day Rules

1. **Skip ahead:** Completing Day 5 while Day 4 is current → Day 4 marked missed
2. **Auto-advance:** If 24 hours pass with no workout → Current day marked missed, advances to next

### Rest Day Handling

- Rest days display for full 24 hours before auto-advancing
- Home screen shows "Today is a rest day" with 💤 visual
- After 24h, auto-advances and marks rest day as completed (not missed)

---

## Data Model

### CycleDay — Updated Fields

```kotlin
data class CycleDay(
    val id: String,
    val cycleId: String,
    val dayNumber: Int,
    val routineId: String?,              // null = rest day
    val name: String?,                   // optional custom name

    // Per-day modifiers (NEW)
    val echoLevel: EchoLevel?,           // Hard, Harder, Hardest, Epic
    val eccentricLoadPercent: Int?,      // 0-150%
    val weightProgressionPercent: Float?, // e.g., +5.0% or -10.0%
    val repModifier: Int?,               // -2 to +2
    val restTimeOverrideSeconds: Int?,   // overrides routine/global default
)

enum class EchoLevel { HARD, HARDER, HARDEST, EPIC }
```

### CycleProgress — Updated Fields

```kotlin
data class CycleProgress(
    val id: String,
    val cycleId: String,
    val currentDayNumber: Int,
    val lastAdvancedAt: Instant,         // NEW: for 24h auto-advance check
    val completedDays: Set<Int>,         // NEW: ✓ days this rotation
    val missedDays: Set<Int>,            // NEW: ✗ days this rotation
    val rotationCount: Int,              // NEW: full cycles completed
    val cycleStartDate: Instant,
)
```

### Rotation Reset Behavior

When cycle loops (last day → Day 1):
- `completedDays` and `missedDays` reset to empty
- `rotationCount` increments by 1
- Historical completion data persists in WorkoutSession records for analytics

### Database Schema Updates

```sql
-- Add columns to CycleDay
ALTER TABLE CycleDay ADD COLUMN echo_level TEXT;           -- 'HARD','HARDER','HARDEST','EPIC'
ALTER TABLE CycleDay ADD COLUMN eccentric_load_percent INTEGER;
ALTER TABLE CycleDay ADD COLUMN weight_progression_percent REAL;
ALTER TABLE CycleDay ADD COLUMN rep_modifier INTEGER;
ALTER TABLE CycleDay ADD COLUMN rest_time_override_seconds INTEGER;

-- Add columns to CycleProgress
ALTER TABLE CycleProgress ADD COLUMN last_advanced_at INTEGER;
ALTER TABLE CycleProgress ADD COLUMN completed_days TEXT;   -- JSON array: "[1,2,3]"
ALTER TABLE CycleProgress ADD COLUMN missed_days TEXT;      -- JSON array: "[4]"
ALTER TABLE CycleProgress ADD COLUMN rotation_count INTEGER DEFAULT 0;
```

---

## Key Files to Modify

| Component | File | Changes |
|-----------|------|---------|
| Domain Models | `domain/model/TrainingCycleModels.kt` | Add modifier fields to CycleDay, tracking fields to CycleProgress |
| Database Schema | `VitruvianDatabase.sq` | Add columns, update queries |
| Cycle Editor UI | `presentation/screen/CycleEditorScreen.kt` | Complete rewrite with two-panel drag-drop |
| Day Config Popup | NEW: `presentation/components/CycleDayConfigSheet.kt` | Context-aware modifier popup |
| Timeline Review | NEW: `presentation/screen/CycleReviewScreen.kt` | Collapsible preview screen |
| Active Cycle UI | `presentation/screen/TrainingCyclesScreen.kt` | Add day strip, update progress display |
| Progress Tracking | `data/repository/TrainingCycleRepository.kt` | Add auto-advance logic, completion tracking |
| Navigation | `presentation/navigation/NavGraph.kt` | Add review screen route |

---

## Implementation Notes

### Drag-and-Drop Library

Use same approach as superset redesign (likely `org.burnoutcrew:reorderable` or custom implementation with `Modifier.pointerInput`).

### Auto-Advance Background Check

Options:
1. **WorkManager** — Periodic task checks if 24h elapsed since `lastAdvancedAt`
2. **On app open** — Check elapsed time when user opens app, advance if needed
3. **Foreground timer** — If app is open past midnight, advance in real-time

Recommend option 2 for simplicity; option 1 for guaranteed accuracy.

### Weight Progression Application

When starting a workout for a day with `weightProgressionPercent`:
1. Calculate baseline from last completed instance of this day (or routine default)
2. Apply percentage: `newWeight = baseline * (1 + progressionPercent/100)`
3. Round to nearest valid weight step (0.5kg increments)
4. Pre-populate workout with calculated weights

---

## Migration Path

1. Add new columns to database with nullable/default values
2. Existing cycles continue working (modifiers all null = no changes)
3. New cycles get full modifier support
4. No data migration required for existing users
