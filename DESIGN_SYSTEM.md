# Phoenix Design System
*Premium fitness app for Vitruvian Trainer+ owners*

---

## Design Principles

1. **Dark First** — OLED-optimized black base (#080A0F). Light mode exists but dark is the identity.
2. **Data Forward** — Numbers are heroes. Force, weight, reps, time — displayed large and bold.
3. **Premium Physical** — Feels like the machine it controls. Heavy, warm, precise.
4. **No Clutter** — If it doesn't help the user train, it doesn't exist on screen.
5. **Italian Soul** — *Forza.* Inspired by the Vitruvian Man — ideal proportion, mathematical precision, human potential.

---

## Color Palette

### Brand Core
| Token | Hex | Usage |
|---|---|---|
| `PhoenixBlack` | `#080A0F` | Hero backgrounds, OLED base |
| `FlameOrange` | `#FF6B00` | Primary actions, CTAs |
| `FlameYellow` | `#FFAB00` | Gradient highlight end |
| `FlameRed` | `#E64A19` | Gradient start |
| `PhoenixOrangeDark` | `#FF9149` | Primary color (dark scheme) |
| `PhoenixAmber` | `#FFA500` | PRO badge, premium accents |
| `ItalianGold` | `#D4AF37` | Da Vinci gold — aspirational moments (PRs, achievements) |

### Glass / Overlay
| Token | Hex | Usage |
|---|---|---|
| `GlassWhite` | `#1AFFFFFF` | Card surfaces (10% white) |
| `GlassBorder` | `#33FFFFFF` | Card borders (20% white) |
| `PhoenixGlow` | `#40FF9149` | Orange ambient glow (25% orange) |

### Slate Neutrals
| Token | Approx Hex | Usage |
|---|---|---|
| `Slate950` | `#020617` | Deepest surfaces |
| `Slate900` | `#0F172A` | Card backgrounds |
| `Slate800` | `#1E293B` | Secondary cards |
| `Slate700` | `#334155` | Borders, dividers |
| `Slate400` | `#94A3B8` | Subtext, icons |
| `Slate200` | `#E2E8F0` | Light mode surfaces |

### Signal
| Token | Hex | Usage |
|---|---|---|
| `SignalSuccess` | `#22C55E` | Completed sets, PRs |
| `SignalError` | `#EF4444` | Failures, end workout |
| `SignalWarning` | `#F59E0B` | Deload warnings, caution |

---

## Gradients

```kotlin
// CTA button — the signature flame
Brush.horizontalGradient(listOf(FlameRed, FlameOrange, FlameYellow))

// PRO badge
Brush.horizontalGradient(listOf(FlameOrange, PhoenixAmber))

// Timer glow ring
Brush.radialGradient(listOf(PhoenixGlow, PhoenixBlack))

// Background hero sections
Brush.verticalGradient(listOf(Slate900, PhoenixBlack))
```

---

## Typography

Built on Material 3 Expressive with larger, bolder defaults.

| Role | Size | Weight | Use Case |
|---|---|---|---|
| `displayLarge` | 64sp | Bold | Force readout, hero numbers |
| `displayMedium` | 50sp | Bold | Large stats (total volume, 1RM) |
| `headlineLarge` | 36sp | Bold | Screen titles |
| `headlineMedium` | 32sp | Bold | Section headers |
| `headlineSmall` | 28sp | Bold | Card headers |
| `titleLarge` | 24sp | Bold | Card titles |
| `titleMedium` | 16sp | Medium | Labels with info |
| `labelMedium` | 12sp | Medium, +0.5sp tracking | Tags, status badges |
| `labelSmall` | 11sp | Medium, +0.5sp tracking | ALL CAPS metric headers |
| `bodyLarge` | 18sp | Normal | Primary body copy |
| `bodySmall` | 12sp | Normal | Secondary descriptions |

**Convention:** Metric labels (`FORCE`, `REPS`, `SETS`) use `labelSmall` with `letterSpacing=1.5.sp` and ALL CAPS.

---

## Core Components

### FlameButton
The primary CTA. Full-width, 56dp height, flame gradient background.
```kotlin
FlameButton(text = "START WORKOUT", onClick = { ... })
```
- Uses: `FlameRed → FlameOrange → FlameYellow` horizontal gradient
- Shadow: `PhoenixGlow` ambient glow (12dp elevation)
- Corner radius: 16dp
- Text: White, Bold, 16sp, 1.5sp letter spacing

### SecondaryFlameButton
Outlined variant. Same shape, gradient border, transparent fill.
```kotlin
SecondaryFlameButton(text = "VIEW SCHEDULE", onClick = { ... })
```

### PremiumBadge
Small inline PRO indicator.
```kotlin
PremiumBadge()           // Small (for inline use)
LargePremiumBadge()      // Full "PHOENIX PRO" (for paywalls)
```

### CircularForceGauge
The Vitruvian differentiator — real-time force visualization.
- Stroke: `PhoenixOrangeDark` on `Slate700` track
- Number: `displayLarge` typography

### RestTimerCard
- Countdown: `displayLarge` (64sp), `PhoenixOrangeDark` color
- Ring: Pulsing radial gradient — `PhoenixGlow` outer, `PhoenixBlack` inner
- Skip button: Primary (orange) at 56dp height

---

## Spacing (Spacing.kt)
Follow the existing Spacing scale. Key values:
- `extraSmall`: 4dp
- `small`: 8dp  
- `medium`: 16dp
- `large`: 24dp
- Standard padding: `20.dp` horizontal, `16.dp` vertical

---

## Shape Language
Material 3 Expressive rounded corners:
- Cards: `20dp` corner radius
- Buttons: `16dp`
- Badges/pills: `4dp` (small), `8dp` (medium)
- Circular elements: `200dp` (effectively circular)

---

## Screen Structure

### HomeScreen — "The Command Deck"
```
┌─────────────────────────────────┐
│  Weekly compliance strip (M-S)  │  ← 7 day dots, streak count
├─────────────────────────────────┤
│  Active Cycle Hero Card         │  ← Today's workout, flame CTA
│  [START WORKOUT ──────────────] │
├─────────────────────────────────┤
│  Recent Activity                │  ← Last 3 sessions
├─────────────────────────────────┤
│  [Cycles] [Single Ex]           │  ← Bottom FAB grid (always visible)
│  [Routines] [JUST LIFT 🔥]      │
└─────────────────────────────────┘
```

### RestTimerCard
```
┌─────────────────────────────────┐
│       REST TIME / QUICK REST    │
│                                 │
│      ╭─── (glow ring) ───╮      │
│      │    1:30           │      │  ← displayLarge, PhoenixOrange
│      ╰───────────────────╯      │
│                                 │
│           UP NEXT               │
│      Cable Chest Press          │
│          Set 2 of 4             │
│                                 │
│  [  NEXT SET CONFIGURATION  ]   │
│                                 │
│  [▶ SKIP REST ──────────────]   │  ← Full width flame button
│  [✕ End Workout            ]   │
└─────────────────────────────────┘
```

---

## Naming / Branding Notes
- Internal codename: **Phoenix**
- Target rebrand: **Italian-inspired** (TBD — *Forza*, *Forma*, *Aurea*, *Codice*)
- App bundle ID will need updating before public launch
- Color scheme name "Phoenix Rising" stays regardless of final app name

---

## V2 Design Considerations
- **Travel Mode UI:** Workout card shows substituted exercises (dumbbell icons vs cable icons)
- **Weight Translation:** Visual indicator when showing converted weights (cable → dumbbell)
- **Force Curve Screen:** Full-screen force visualization — signature premium feature
- **Onboarding:** Machine pairing flow + subscription upsell
