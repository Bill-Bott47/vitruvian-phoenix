# Homescreen Buttons & Background Gradient Redesign

## Overview

Redesign the homescreen FAB buttons with unified color palette and animations, plus migrate all screen background gradients to a consistent theme-aware system.

## Goals

1. Unify homescreen button colors using a cool-toned palette
2. Add animations to make buttons feel alive and responsive
3. Create clear visual hierarchy (primary action stands out)
4. Migrate 12 screens from hardcoded gradients to theme-aware backgrounds

## Color Palette

Source: [Coolors Palette](https://coolors.co/7cea9c-55d6be-2e5eaa-5b4e77-593959)

| Name | Hex | Usage |
|------|-----|-------|
| Mint Green | `#7CEA9C` | Secondary button text (light mode) |
| Turquoise | `#55D6BE` | Secondary button fill/border |
| Royal Blue | `#2E5EAA` | Primary button fill (Just Lift) |
| Muted Purple | `#5B4E77` | Reserved for accents |
| Dark Plum | `#593959` | Background gradient accent |

## Button Design

### Primary Action (Just Lift / Start Session)

- **Fill**: Royal Blue `#2E5EAA` (solid)
- **Text/Icon**: White
- **Idle Animation**: Gentle pulse (1.0x → 1.02x scale) + glow pulse
- **Press Feedback**: Scale to 0.95x, spring bounce back

### Secondary Actions (Cycles, Routines, Single Exercise)

- **Fill**: Turquoise `#55D6BE` at 15% opacity (glassmorphic)
- **Border**: Turquoise `#55D6BE` at 100%, 1.5dp stroke
- **Text/Icon**: Mint `#7CEA9C` (light mode) / White (dark mode)
- **Idle Animation**: Shimmer sweep (diagonal light reflection)
- **Press Feedback**: Scale to 0.95x, spring bounce back

### Icon Animations

| Button | Icon | Animation |
|--------|------|-----------|
| Just Lift | Play Arrow | Pulses in sync with button glow |
| Cycles | Loop | Slow continuous rotation (8s/rev) |
| Routines | List | None (static) |
| Single Exercise | Dumbbell | Tilt oscillation (±5°) |

## Animation Specifications

### Press Feedback (All Buttons)

```kotlin
// Scale animation
val scale by animateFloatAsState(
    targetValue = if (pressed) 0.95f else 1f,
    animationSpec = spring(
        dampingRatio = Spring.DampingRatioMediumBouncy,
        stiffness = Spring.StiffnessLow
    )
)
```

### Primary Button Idle Pulse

```kotlin
val infiniteTransition = rememberInfiniteTransition()
val pulseScale by infiniteTransition.animateFloat(
    initialValue = 1f,
    targetValue = 1.02f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = EaseInOutSine),
        repeatMode = RepeatMode.Reverse
    )
)
val glowAlpha by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.5f,
    animationSpec = infiniteRepeatable(
        animation = tween(1000, easing = EaseInOutSine),
        repeatMode = RepeatMode.Reverse
    )
)
```

### Secondary Button Shimmer

```kotlin
val shimmerOffset by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 2f,
    animationSpec = infiniteRepeatable(
        animation = tween(3000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
)
// Apply as gradient offset in drawWithContent
```

### Cycles Icon Rotation

```kotlin
val rotation by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
        animation = tween(8000, easing = LinearEasing),
        repeatMode = RepeatMode.Restart
    )
)
```

## Background Gradient Migration

### Current State

12 screens have hardcoded purple/indigo gradients:
```kotlin
// Dark mode (repeated across files)
Color(0xFF0F172A), // slate-900
Color(0xFF1E1B4B), // indigo-950
Color(0xFF172554)  // blue-950

// Light mode
Color(0xFFE0E7FF), // indigo-200
Color(0xFFFCE7F3), // pink-100
Color(0xFFDDD6FE)  // violet-200
```

### New Theme-Aware Gradient

**Dark Mode:**
```kotlin
Brush.verticalGradient(
    0.0f to Slate900,           // #0F172A
    0.5f to DarkPlum.copy(alpha = 0.3f), // #593959 at 30%
    1.0f to Slate900            // #0F172A
)
```

**Light Mode:**
```kotlin
Brush.verticalGradient(
    0.0f to Slate50,            // #F8FAFC
    0.5f to MintGreen.copy(alpha = 0.1f), // #7CEA9C at 10%
    1.0f to Color.White
)
```

### Files to Migrate

1. `AnalyticsScreen.kt`
2. `DailyRoutinesScreen.kt`
3. `ExerciseDetailScreen.kt`
4. `ProgramBuilderScreen.kt`
5. `RoutineBuilderDialog.kt`
6. `RoutinesTab.kt`
7. `TrainingCyclesScreen.kt`
8. `WeeklyProgramsScreen.kt`
9. `WorkoutTab.kt`
10. `WorkoutTabAlt.kt`
11. `CreateExerciseDialog.kt`
12. `HomeScreen.kt`

## Implementation Architecture

### New Files

**`shared/.../ui/theme/HomeButtonColors.kt`**
```kotlin
object HomeButtonColors {
    val PrimaryBlue = Color(0xFF2E5EAA)
    val SecondaryTurquoise = Color(0xFF55D6BE)
    val SecondaryMint = Color(0xFF7CEA9C)
    val AccentPlum = Color(0xFF593959)
}
```

**`shared/.../ui/theme/ThemeHelpers.kt`**
```kotlin
@Composable
fun screenBackgroundBrush(): Brush {
    val isDark = isSystemInDarkTheme()
    return if (isDark) {
        Brush.verticalGradient(
            0.0f to Slate900,
            0.5f to HomeButtonColors.AccentPlum.copy(alpha = 0.3f),
            1.0f to Slate900
        )
    } else {
        Brush.verticalGradient(
            0.0f to Slate50,
            0.5f to HomeButtonColors.SecondaryMint.copy(alpha = 0.1f),
            1.0f to Color.White
        )
    }
}
```

**`shared/.../presentation/components/AnimatedActionButton.kt`**
- Reusable animated FAB component
- Parameters: `label`, `icon`, `onClick`, `isPrimary`, `iconAnimation`
- Encapsulates all animation logic

### Modified Files

- `HomeScreen.kt` - Replace 4 inline FABs with `AnimatedActionButton`
- 12 screen files - Replace hardcoded gradients with `screenBackgroundBrush()`

## Visual Hierarchy

```
┌─────────────────────────────────────┐
│          HomeScreen                 │
│                                     │
│  ┌─────────────┐ ┌─────────────┐   │
│  │   Cycles    │ │Single Exercise│  │  ← Secondary (Turquoise glass)
│  │   (rotate)  │ │   (tilt)     │   │
│  └─────────────┘ └─────────────┘   │
│  ┌─────────────┐ ┌─────────────┐   │
│  │  Routines   │ │  JUST LIFT  │   │  ← Primary (Royal Blue solid)
│  │   (static)  │ │   (pulse)   │   │     with pulse glow
│  └─────────────┘ └─────────────┘   │
└─────────────────────────────────────┘
```

## Design Rationale

1. **Royal Blue for Primary**: High contrast, action-oriented, draws the eye
2. **Glassmorphic Secondary**: Modern, unified, doesn't compete with primary
3. **Idle Animations**: Adds life without being distracting
4. **Press Feedback**: Tactile feel, confirms interaction
5. **Staggered Shimmer**: Prevents visual chaos (not all buttons shimmer in sync)
6. **Plum in Backgrounds**: Ties the cool button palette to the screen backgrounds
