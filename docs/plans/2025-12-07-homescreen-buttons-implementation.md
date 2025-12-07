# Homescreen Buttons & Background Gradient Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add animated FAB buttons with unified color palette to homescreen, plus migrate 12 screens to theme-aware background gradients.

**Architecture:** Create reusable `AnimatedActionButton` component with press/idle animations, `HomeButtonColors` object for button-specific colors, and `ThemeHelpers.kt` for background gradient function. Update HomeScreen to use new buttons, then migrate all screens to use the gradient helper.

**Tech Stack:** Compose Multiplatform, Compose Animation APIs (infiniteTransition, animateFloatAsState, spring)

---

## Task 1: Create HomeButtonColors Object

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/ui/theme/HomeButtonColors.kt`

**Step 1: Create the color definitions file**

```kotlin
package com.devil.phoenixproject.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Colors specific to homescreen action buttons.
 * Based on Coolors palette: https://coolors.co/7cea9c-55d6be-2e5eaa-5b4e77-593959
 */
object HomeButtonColors {
    /** Primary action button (Just Lift) - Royal Blue */
    val PrimaryBlue = Color(0xFF2E5EAA)

    /** Secondary button border/accent - Turquoise */
    val SecondaryTurquoise = Color(0xFF55D6BE)

    /** Secondary button text (light mode) - Mint Green */
    val SecondaryMint = Color(0xFF7CEA9C)

    /** Background gradient accent - Dark Plum */
    val AccentPlum = Color(0xFF593959)

    /** Muted Purple - reserved for future accents */
    val AccentPurple = Color(0xFF5B4E77)
}
```

**Step 2: Verify it compiles**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :shared:compileKotlinMetadata --quiet`
Expected: No errors

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/ui/theme/HomeButtonColors.kt
git commit -m "feat(theme): add HomeButtonColors for homescreen button palette"
```

---

## Task 2: Create ThemeHelpers with Background Gradient

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/ui/theme/ThemeHelpers.kt`

**Step 1: Create the theme helpers file**

```kotlin
package com.devil.phoenixproject.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/**
 * Theme helper functions for consistent styling across screens.
 */

/**
 * Returns a vertical gradient brush for screen backgrounds.
 * Dark mode: Slate with subtle plum accent in center
 * Light mode: Light with subtle mint wash
 */
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

**Step 2: Verify it compiles**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :shared:compileKotlinMetadata --quiet`
Expected: No errors

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/ui/theme/ThemeHelpers.kt
git commit -m "feat(theme): add screenBackgroundBrush helper for consistent gradients"
```

---

## Task 3: Create AnimatedActionButton Component

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/AnimatedActionButton.kt`

**Step 1: Create the animated button component**

```kotlin
package com.devil.phoenixproject.presentation.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devil.phoenixproject.ui.theme.HomeButtonColors

/**
 * Icon animation types for AnimatedActionButton
 */
enum class IconAnimation {
    NONE,
    PULSE,      // Scale pulse (for Play icon)
    ROTATE,     // Continuous rotation (for Loop icon)
    TILT        // Oscillating tilt (for Dumbbell icon)
}

/**
 * Animated FAB with press feedback and idle animations.
 *
 * @param label Button text
 * @param icon Button icon
 * @param onClick Click handler
 * @param isPrimary If true, uses solid Royal Blue. If false, uses glassmorphic turquoise.
 * @param iconAnimation Type of icon animation to apply
 * @param modifier Modifier for the button
 */
@Composable
fun AnimatedActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    isPrimary: Boolean,
    iconAnimation: IconAnimation = IconAnimation.NONE,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    // Press feedback animation
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "pressScale"
    )

    // Idle animations
    val infiniteTransition = rememberInfiniteTransition(label = "idleTransition")

    // Primary button pulse
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = if (isPrimary) 1.02f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = if (isPrimary) 0.6f else 0.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowAlpha"
    )

    // Secondary button shimmer
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -1f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmerOffset"
    )

    // Icon animations
    val iconRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = if (iconAnimation == IconAnimation.ROTATE) 360f else 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "iconRotation"
    )

    val iconTilt by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconTilt"
    )

    val iconPulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    // Determine icon transform
    val iconModifier = when (iconAnimation) {
        IconAnimation.NONE -> Modifier
        IconAnimation.PULSE -> Modifier.scale(iconPulse)
        IconAnimation.ROTATE -> Modifier.graphicsLayer { rotationZ = iconRotation }
        IconAnimation.TILT -> Modifier.graphicsLayer { rotationZ = iconTilt }
    }

    // Colors based on primary/secondary
    val containerColor = if (isPrimary) {
        HomeButtonColors.PrimaryBlue
    } else {
        HomeButtonColors.SecondaryTurquoise.copy(alpha = 0.15f)
    }

    val contentColor = if (isPrimary) {
        Color.White
    } else {
        HomeButtonColors.SecondaryMint
    }

    val border = if (isPrimary) {
        null
    } else {
        BorderStroke(1.5.dp, HomeButtonColors.SecondaryTurquoise)
    }

    ExtendedFloatingActionButton(
        onClick = onClick,
        containerColor = containerColor,
        contentColor = contentColor,
        interactionSource = interactionSource,
        modifier = modifier
            .fillMaxWidth()
            .height(52.dp)
            .scale(scale * pulseScale)
            .then(
                if (!isPrimary) {
                    Modifier.drawWithContent {
                        drawContent()
                        // Shimmer overlay
                        drawRect(
                            brush = Brush.linearGradient(
                                colors = listOf(
                                    Color.Transparent,
                                    Color.White.copy(alpha = 0.1f),
                                    Color.Transparent
                                ),
                                start = Offset(size.width * shimmerOffset, 0f),
                                end = Offset(size.width * (shimmerOffset + 0.5f), size.height)
                            )
                        )
                    }
                } else {
                    Modifier
                }
            ),
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = iconModifier
            )
        },
        text = { Text(label) }
    )
}
```

**Step 2: Verify it compiles**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :shared:compileKotlinMetadata --quiet`
Expected: No errors

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/AnimatedActionButton.kt
git commit -m "feat(components): add AnimatedActionButton with press/idle/icon animations"
```

---

## Task 4: Update HomeScreen to Use AnimatedActionButton

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/HomeScreen.kt`

**Step 1: Add imports at top of file (after existing imports around line 35)**

Add these imports:
```kotlin
import com.devil.phoenixproject.presentation.components.AnimatedActionButton
import com.devil.phoenixproject.presentation.components.IconAnimation
```

**Step 2: Replace the bottom FAB grid (lines 140-211)**

Replace the entire `Row` containing the FABs with:

```kotlin
            // Bottom FAB Grid: 2 columns, equal width
            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Left column: Cycles & Routines
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedActionButton(
                        label = "Cycles",
                        icon = Icons.Default.Loop,
                        onClick = { navController.navigate(NavigationRoutes.TrainingCycles.route) },
                        isPrimary = false,
                        iconAnimation = IconAnimation.ROTATE
                    )
                    AnimatedActionButton(
                        label = "Routines",
                        icon = Icons.Default.FormatListBulleted,
                        onClick = { navController.navigate(NavigationRoutes.DailyRoutines.route) },
                        isPrimary = false,
                        iconAnimation = IconAnimation.NONE
                    )
                }

                // Right column: Single Exercise & Just Lift
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    AnimatedActionButton(
                        label = "Single Exercise",
                        icon = Icons.Outlined.FitnessCenter,
                        onClick = { navController.navigate(NavigationRoutes.SingleExercise.route) },
                        isPrimary = false,
                        iconAnimation = IconAnimation.TILT
                    )
                    AnimatedActionButton(
                        label = if (activeCycle != null) "Start Session" else "Just Lift",
                        icon = Icons.Default.PlayArrow,
                        onClick = {
                            if (nextRoutineId != null) {
                                viewModel.ensureConnection(
                                    onConnected = {
                                        viewModel.loadRoutineById(nextRoutineId)
                                        viewModel.startWorkout()
                                        navController.navigate(NavigationRoutes.ActiveWorkout.route)
                                    },
                                    onFailed = { /* Error handled by state */ }
                                )
                            } else {
                                navController.navigate(NavigationRoutes.JustLift.route)
                            }
                        },
                        isPrimary = true,
                        iconAnimation = IconAnimation.PULSE
                    )
                }
            }
```

**Step 3: Verify it compiles**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :shared:compileKotlinMetadata --quiet`
Expected: No errors

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/HomeScreen.kt
git commit -m "feat(homescreen): replace FABs with AnimatedActionButton components"
```

---

## Task 5: Migrate HomeScreen Background to Theme Gradient

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/HomeScreen.kt`

**Step 1: Add import for screenBackgroundBrush**

Add this import at top of file:
```kotlin
import com.devil.phoenixproject.ui.theme.screenBackgroundBrush
```

**Step 2: Update Scaffold containerColor**

Find line ~91-93:
```kotlin
    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
```

Change to:
```kotlin
    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
```

**Step 3: Add background brush to the Box**

Find line ~94:
```kotlin
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
```

Change to:
```kotlin
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(screenBackgroundBrush())
                .padding(paddingValues)
        ) {
```

**Step 4: Add background import if not present**

Ensure this import exists:
```kotlin
import androidx.compose.foundation.background
```

**Step 5: Verify it compiles**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :shared:compileKotlinMetadata --quiet`
Expected: No errors

**Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/HomeScreen.kt
git commit -m "feat(homescreen): apply theme-aware background gradient"
```

---

## Task 6: Migrate Remaining Screens to Theme Gradient

**Files to modify (11 screens):**
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/AnalyticsScreen.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/DailyRoutinesScreen.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/ExerciseDetailScreen.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/ProgramBuilderScreen.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/RoutineBuilderDialog.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/RoutinesTab.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/WeeklyProgramsScreen.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/WorkoutTab.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/WorkoutTabAlt.kt`
- `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/CreateExerciseDialog.kt`

**Step 1: For each file, apply this pattern**

Add import:
```kotlin
import com.devil.phoenixproject.ui.theme.screenBackgroundBrush
```

Find hardcoded gradient like:
```kotlin
Brush.verticalGradient(
    colors = if (isDark) listOf(
        Color(0xFF0F172A),
        Color(0xFF1E1B4B),
        Color(0xFF172554)
    ) else listOf(
        Color(0xFFE0E7FF),
        Color(0xFFFCE7F3),
        Color(0xFFDDD6FE)
    )
)
```

Replace with:
```kotlin
screenBackgroundBrush()
```

Remove the `isDark` variable if it's only used for the gradient.

**Step 2: Verify all screens compile**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :shared:compileKotlinMetadata --quiet`
Expected: No errors

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/*.kt
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/CreateExerciseDialog.kt
git commit -m "refactor(screens): migrate 11 screens to screenBackgroundBrush"
```

---

## Task 7: Build and Visual Verification

**Step 1: Full Android build**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL

**Step 2: Run tests**

Run: `cd .worktrees/homescreen-buttons && ./gradlew :androidApp:testDebugUnitTest`
Expected: All tests pass

**Step 3: Commit verification**

```bash
git commit --allow-empty -m "chore: verify homescreen buttons and gradients build successfully"
```

---

## Summary

After completing all tasks:
- Homescreen has 4 animated FAB buttons with unified color palette
- Primary button (Just Lift) is Royal Blue with pulse glow
- Secondary buttons are glassmorphic turquoise with shimmer
- Icon animations: Loop rotates, Dumbbell tilts, Play pulses
- 12 screens use consistent theme-aware background gradients
- No external libraries needed - pure Compose animation APIs
