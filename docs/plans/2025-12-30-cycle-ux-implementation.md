# Cycle UX Redesign Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the current clunky cycle editor with a visual drag-and-drop scheduler featuring per-day modifiers, timeline review, and flexible day selection during active use.

**Architecture:** Two-phase approach: (1) Update data model and database schema with new CycleDay modifier fields and CycleProgress tracking fields, (2) Rebuild UI screens from the ground up - day count picker, two-panel drag-drop editor, config popup, timeline review, and active day strip.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, sh.calvin.reorderable (already in use), Koin DI

---

## Phase 1: Data Model & Database

### Task 1: Add EchoLevel to CycleDay modifiers

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/TrainingCycleModels.kt:58-97`

**Step 1: Update CycleDay data class**

Add new modifier fields to the existing CycleDay class:

```kotlin
data class CycleDay(
    val id: String,
    val cycleId: String,
    val dayNumber: Int,
    val name: String?,
    val routineId: String?,
    val isRestDay: Boolean,
    // NEW: Per-day modifiers
    val echoLevel: EchoLevel? = null,
    val eccentricLoadPercent: Int? = null,  // 0-150
    val weightProgressionPercent: Float? = null,  // e.g., +5.0 or -10.0
    val repModifier: Int? = null,  // -2 to +2
    val restTimeOverrideSeconds: Int? = null
)
```

**Step 2: Update companion object create() and restDay() functions**

Add default null values for new parameters.

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/TrainingCycleModels.kt
git commit -m "feat(cycle): add per-day modifier fields to CycleDay model"
```

---

### Task 2: Update CycleProgress with tracking fields

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/TrainingCycleModels.kt:102-139`

**Step 1: Update CycleProgress data class**

```kotlin
data class CycleProgress(
    val id: String,
    val cycleId: String,
    val currentDayNumber: Int,
    val lastCompletedDate: Long?,
    val cycleStartDate: Long,
    // NEW: Tracking fields
    val lastAdvancedAt: Long? = null,  // For 24h auto-advance
    val completedDays: Set<Int> = emptySet(),  // Days completed this rotation
    val missedDays: Set<Int> = emptySet(),  // Days missed this rotation
    val rotationCount: Int = 0  // Full cycles completed
)
```

**Step 2: Add helper methods**

```kotlin
fun shouldAutoAdvance(): Boolean {
    val advancedAt = lastAdvancedAt ?: return false
    val now = currentTimeMillis()
    val hoursSince = (now - advancedAt) / (60 * 60 * 1000L)
    return hoursSince >= 24
}

fun advanceToNextDay(totalDays: Int, markMissed: Boolean = false): CycleProgress {
    val newMissed = if (markMissed) missedDays + currentDayNumber else missedDays
    val nextDay = if (currentDayNumber >= totalDays) 1 else currentDayNumber + 1
    val newRotation = if (currentDayNumber >= totalDays) rotationCount + 1 else rotationCount
    val resetCompleted = if (currentDayNumber >= totalDays) emptySet() else completedDays
    val resetMissed = if (currentDayNumber >= totalDays) emptySet() else newMissed

    return copy(
        currentDayNumber = nextDay,
        lastAdvancedAt = currentTimeMillis(),
        completedDays = resetCompleted,
        missedDays = resetMissed,
        rotationCount = newRotation
    )
}

fun markDayCompleted(dayNumber: Int, totalDays: Int): CycleProgress {
    // Mark any skipped days as missed
    val skippedDays = (currentDayNumber until dayNumber)
        .filter { it !in completedDays && it !in missedDays }
        .toSet()

    val newMissed = missedDays + skippedDays
    val newCompleted = completedDays + dayNumber
    val nextDay = if (dayNumber >= totalDays) 1 else dayNumber + 1
    val newRotation = if (dayNumber >= totalDays) rotationCount + 1 else rotationCount
    val resetCompleted = if (dayNumber >= totalDays) emptySet() else newCompleted
    val resetMissed = if (dayNumber >= totalDays) emptySet() else newMissed

    return copy(
        currentDayNumber = nextDay,
        lastCompletedDate = currentTimeMillis(),
        lastAdvancedAt = currentTimeMillis(),
        completedDays = resetCompleted,
        missedDays = resetMissed,
        rotationCount = newRotation
    )
}
```

**Step 3: Update companion object create() function**

**Step 4: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/domain/model/TrainingCycleModels.kt
git commit -m "feat(cycle): add progress tracking fields to CycleProgress"
```

---

### Task 3: Update database schema

**Files:**
- Modify: `shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq:773-792`

**Step 1: Add columns to CycleDay table**

Find the CycleDay CREATE TABLE statement and add new columns:

```sql
CREATE TABLE CycleDay (
    id TEXT PRIMARY KEY NOT NULL,
    cycle_id TEXT NOT NULL,
    day_number INTEGER NOT NULL,
    name TEXT,
    routine_id TEXT,
    is_rest_day INTEGER NOT NULL DEFAULT 0,
    -- NEW: Per-day modifiers
    echo_level TEXT,  -- 'HARD','HARDER','HARDEST','EPIC'
    eccentric_load_percent INTEGER,  -- 0-150
    weight_progression_percent REAL,  -- e.g., 5.0 or -10.0
    rep_modifier INTEGER,  -- -2 to +2
    rest_time_override_seconds INTEGER,
    FOREIGN KEY (cycle_id) REFERENCES TrainingCycle(id) ON DELETE CASCADE,
    FOREIGN KEY (routine_id) REFERENCES Routine(id) ON DELETE SET NULL
);
```

**Step 2: Add columns to CycleProgress table**

```sql
CREATE TABLE CycleProgress (
    id TEXT PRIMARY KEY NOT NULL,
    cycle_id TEXT NOT NULL UNIQUE,
    current_day_number INTEGER NOT NULL DEFAULT 1,
    last_completed_date INTEGER,
    cycle_start_date INTEGER NOT NULL,
    -- NEW: Tracking fields
    last_advanced_at INTEGER,
    completed_days TEXT,  -- JSON array: "[1,2,3]"
    missed_days TEXT,  -- JSON array: "[4]"
    rotation_count INTEGER NOT NULL DEFAULT 0,
    FOREIGN KEY (cycle_id) REFERENCES TrainingCycle(id) ON DELETE CASCADE
);
```

**Step 3: Update insert/select queries**

Update `insertCycleDay` query:
```sql
insertCycleDay:
INSERT INTO CycleDay(id, cycle_id, day_number, name, routine_id, is_rest_day, echo_level, eccentric_load_percent, weight_progression_percent, rep_modifier, rest_time_override_seconds)
VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);
```

Update `selectCycleDaysByCycleId` query to include new columns.

Update `insertCycleProgress` and `updateCycleProgress` queries to include new columns.

**Step 4: Increment schema version**

Find the schema version and increment it. Add migration if needed.

**Step 5: Verify compilation**

Run: `./gradlew :shared:generateCommonMainVitruvianDatabaseInterface`
Expected: BUILD SUCCESSFUL

**Step 6: Commit**

```bash
git add shared/src/commonMain/sqldelight/com/devil/phoenixproject/database/VitruvianDatabase.sq
git commit -m "feat(db): add cycle modifier and progress tracking columns"
```

---

### Task 4: Update repository mappings

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightTrainingCycleRepository.kt`

**Step 1: Update CycleDay mapping**

Add mapping for new columns in the `toCycleDay()` extension function or wherever CycleDay is constructed from database rows.

**Step 2: Update CycleProgress mapping**

Add JSON parsing for `completed_days` and `missed_days` columns. Use kotlinx.serialization or simple string parsing:

```kotlin
private fun parseIntSet(json: String?): Set<Int> {
    if (json.isNullOrBlank()) return emptySet()
    return json.trim('[', ']')
        .split(',')
        .mapNotNull { it.trim().toIntOrNull() }
        .toSet()
}

private fun intSetToJson(set: Set<Int>): String {
    return set.sorted().joinToString(",", "[", "]")
}
```

**Step 3: Update insert/update methods**

Ensure `saveCycleDay()` and `updateCycleProgress()` methods include new fields.

**Step 4: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/SqlDelightTrainingCycleRepository.kt
git commit -m "feat(repo): update repository to handle new cycle fields"
```

---

## Phase 2: Day Count Picker Screen

### Task 5: Create DayCountPickerScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/DayCountPickerScreen.kt`

**Step 1: Create the screen composable**

```kotlin
@Composable
fun DayCountPickerScreen(
    onDayCountSelected: (Int) -> Unit,
    onBack: () -> Unit
) {
    val presets = listOf(7, 14, 21, 28)
    var customValue by remember { mutableStateOf("") }
    var showCustomInput by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Create Cycle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "How many days in your cycle?",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )

            Spacer(Modifier.height(8.dp))

            Text(
                "This is a rolling schedule — Day 1 follows the last day automatically",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.height(32.dp))

            // Preset chips
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                presets.forEach { days ->
                    FilterChip(
                        selected = false,
                        onClick = { onDayCountSelected(days) },
                        label = { Text("$days") },
                        modifier = Modifier.size(64.dp, 48.dp)
                    )
                }
            }

            Spacer(Modifier.height(16.dp))

            // Custom option
            OutlinedButton(
                onClick = { showCustomInput = true }
            ) {
                Text("Custom")
            }
        }
    }

    // Custom input dialog
    if (showCustomInput) {
        AlertDialog(
            onDismissRequest = { showCustomInput = false },
            title = { Text("Custom Day Count") },
            text = {
                OutlinedTextField(
                    value = customValue,
                    onValueChange = { customValue = it.filter { c -> c.isDigit() } },
                    label = { Text("Number of days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        customValue.toIntOrNull()?.let {
                            if (it in 1..365) onDayCountSelected(it)
                        }
                        showCustomInput = false
                    }
                ) { Text("OK") }
            },
            dismissButton = {
                TextButton(onClick = { showCustomInput = false }) { Text("Cancel") }
            }
        )
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/DayCountPickerScreen.kt
git commit -m "feat(ui): add DayCountPickerScreen for cycle creation"
```

---

### Task 6: Add navigation route for DayCountPicker

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/navigation/NavigationRoutes.kt`
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/navigation/NavGraph.kt`

**Step 1: Add route object**

```kotlin
object DayCountPicker : NavigationRoutes("dayCountPicker")
```

**Step 2: Add composable to NavGraph**

```kotlin
composable(NavigationRoutes.DayCountPicker.route) {
    DayCountPickerScreen(
        onDayCountSelected = { dayCount ->
            navController.navigate(NavigationRoutes.CycleEditor.createRoute("new", dayCount))
        },
        onBack = { navController.popBackStack() }
    )
}
```

**Step 3: Update CycleEditor route to accept dayCount parameter**

```kotlin
object CycleEditor : NavigationRoutes("cycleEditor/{cycleId}?dayCount={dayCount}") {
    fun createRoute(cycleId: String, dayCount: Int? = null): String {
        val base = "cycleEditor/$cycleId"
        return if (dayCount != null) "$base?dayCount=$dayCount" else base
    }
}
```

**Step 4: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/navigation/
git commit -m "feat(nav): add DayCountPicker route and update CycleEditor route"
```

---

## Phase 3: Cycle Editor Redesign

### Task 7: Create CycleDayConfigSheet component

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/CycleDayConfigSheet.kt`

**Step 1: Create the bottom sheet composable**

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDayConfigSheet(
    day: CycleDay,
    routine: Routine?,
    onDismiss: () -> Unit,
    onApply: (CycleDay) -> Unit
) {
    // Determine which modifiers are relevant based on routine exercises
    // Note: RoutineExercise.workoutType is WorkoutType (Echo or Program)
    val hasEchoMode = routine?.exercises?.any {
        it.workoutType is WorkoutType.Echo
    } ?: false
    val hasOldSchool = routine?.exercises?.any {
        it.workoutType is WorkoutType.Program
    } ?: true
    val allSetsAmrap = routine?.exercises?.all { exercise ->
        exercise.isAMRAP
    } ?: false

    var echoLevel by remember { mutableStateOf(day.echoLevel) }
    var eccentricLoad by remember { mutableStateOf(day.eccentricLoadPercent ?: 100) }
    var weightProgression by remember { mutableStateOf(day.weightProgressionPercent ?: 0f) }
    var repModifier by remember { mutableStateOf(day.repModifier ?: 0) }
    var restTime by remember { mutableStateOf(day.restTimeOverrideSeconds ?: 90) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Day ${day.dayNumber} — ${routine?.name ?: "Workout"}",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, "Close")
                }
            }

            Spacer(Modifier.height(24.dp))

            // Echo Level (if applicable)
            if (hasEchoMode) {
                Text("ECHO LEVEL", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    EchoLevel.entries.forEach { level ->
                        FilterChip(
                            selected = echoLevel == level,
                            onClick = { echoLevel = level },
                            label = { Text(level.displayName) }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))

                // Eccentric Load
                Text("ECCENTRIC LOAD", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = eccentricLoad.toFloat(),
                        onValueChange = { eccentricLoad = it.toInt() },
                        valueRange = 0f..150f,
                        steps = 5,  // 0, 50, 75, 100, 110, 120, 130, 140, 150
                        modifier = Modifier.weight(1f)
                    )
                    Text("$eccentricLoad%", modifier = Modifier.width(48.dp))
                }
                Spacer(Modifier.height(16.dp))
            }

            // Weight Progression (if Old School)
            if (hasOldSchool) {
                Text("WEIGHT PROGRESSION", style = MaterialTheme.typography.labelMedium)
                Text(
                    "Each occurrence adds this percentage",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Slider(
                        value = weightProgression,
                        onValueChange = { weightProgression = it },
                        valueRange = -20f..20f,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        "${if (weightProgression >= 0) "+" else ""}${weightProgression.toInt()}%",
                        modifier = Modifier.width(48.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
            }

            // Rep Modifier (if not all AMRAP)
            if (!allSetsAmrap) {
                Text("REP MODIFIER", style = MaterialTheme.typography.labelMedium)
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(-2, -1, 0, 1, 2).forEach { mod ->
                        FilterChip(
                            selected = repModifier == mod,
                            onClick = { repModifier = mod },
                            label = { Text(if (mod >= 0) "+$mod" else "$mod") }
                        )
                    }
                }
                Spacer(Modifier.height(16.dp))
            }

            // Rest Time (always shown)
            Text("REST TIME", style = MaterialTheme.typography.labelMedium)
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Slider(
                    value = restTime.toFloat(),
                    onValueChange = { restTime = it.toInt() },
                    valueRange = 0f..300f,
                    modifier = Modifier.weight(1f)
                )
                Text("${restTime}s", modifier = Modifier.width(48.dp))
            }

            Spacer(Modifier.height(24.dp))

            // Apply button
            Button(
                onClick = {
                    onApply(day.copy(
                        echoLevel = if (hasEchoMode) echoLevel else null,
                        eccentricLoadPercent = if (hasEchoMode) eccentricLoad else null,
                        weightProgressionPercent = if (hasOldSchool && weightProgression != 0f) weightProgression else null,
                        repModifier = if (!allSetsAmrap && repModifier != 0) repModifier else null,
                        restTimeOverrideSeconds = if (restTime != 90) restTime else null
                    ))
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Apply")
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/CycleDayConfigSheet.kt
git commit -m "feat(ui): add CycleDayConfigSheet for per-day modifiers"
```

---

### Task 8: Rewrite CycleEditorScreen with two-panel layout

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/CycleEditorScreen.kt`

**Step 1: Update state to include routine palette drag source**

```kotlin
data class CycleEditorState(
    val cycleName: String = "",
    val description: String = "",
    val days: List<CycleDay> = emptyList(),
    val configDayIndex: Int? = null  // For config sheet
)
```

**Step 2: Create two-panel layout**

Replace the single LazyColumn with a Row containing:
- Left panel (~30%): Routine palette as LazyColumn
- Right panel (~70%): Day slots as LazyColumn with reorderable

```kotlin
Row(modifier = Modifier.fillMaxSize()) {
    // Left: Routine Palette
    LazyColumn(
        modifier = Modifier
            .weight(0.3f)
            .fillMaxHeight()
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(8.dp)
    ) {
        item {
            Text(
                "ROUTINES",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        items(routines, key = { it.id }) { routine ->
            RoutineDragSource(
                routine = routine,
                onDragStarted = { /* handle drag */ }
            )
        }
    }

    // Right: Day Slots
    LazyColumn(
        state = lazyListState,
        modifier = Modifier
            .weight(0.7f)
            .fillMaxHeight()
            .padding(8.dp)
    ) {
        item {
            Text(
                "YOUR CYCLE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        itemsIndexed(state.days, key = { _, day -> day.id }) { index, day ->
            // Day slot with drop target + reorderable
        }
    }
}
```

**Step 3: Implement drag-and-drop from palette to day slots**

Use `Modifier.pointerInput` to detect drag gestures and `LocalDensity` for coordinate translation.

**Step 4: Update day cards to show modifier badges**

```kotlin
@Composable
fun CycleDaySlot(
    day: CycleDay,
    routineName: String?,
    onTap: () -> Unit,
    onClear: () -> Unit,
    onDrop: (Routine) -> Unit,
    dragModifier: Modifier
) {
    val hasModifiers = day.echoLevel != null ||
        day.weightProgressionPercent != null ||
        day.repModifier != null

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { if (routineName != null) onTap() }
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Drag handle
            Icon(Icons.Default.DragHandle, null, modifier = dragModifier)

            // Day number
            Text("Day ${day.dayNumber}", fontWeight = FontWeight.Bold)

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                if (day.routineId == null) {
                    // Rest day
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("💤", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(8.dp))
                        Text("Rest", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Text(routineName ?: "Select Routine", fontWeight = FontWeight.Medium)

                    // Modifier badges
                    if (hasModifiers) {
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            day.echoLevel?.let {
                                Badge { Text(it.displayName) }
                            }
                            day.weightProgressionPercent?.let {
                                Badge { Text("${if (it >= 0) "+" else ""}${it.toInt()}%") }
                            }
                            day.repModifier?.let {
                                Badge { Text("${if (it >= 0) "+" else ""}$it reps") }
                            }
                        }
                    }
                }
            }

            // Clear button (if assigned)
            if (day.routineId != null) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, "Clear")
                }
            }
        }
    }
}
```

**Step 5: Wire up config sheet**

```kotlin
// At bottom of CycleEditorScreen
state.configDayIndex?.let { index ->
    val day = state.days[index]
    val routine = day.routineId?.let { id -> routines.find { it.id == id } }

    CycleDayConfigSheet(
        day = day,
        routine = routine,
        onDismiss = { state = state.copy(configDayIndex = null) },
        onApply = { updatedDay ->
            val newDays = state.days.toMutableList().apply { set(index, updatedDay) }
            state = state.copy(days = newDays, configDayIndex = null)
        }
    )
}
```

**Step 6: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/CycleEditorScreen.kt
git commit -m "feat(ui): rewrite CycleEditorScreen with two-panel drag-drop layout"
```

---

## Phase 4: Timeline Review Screen

### Task 9: Create CycleReviewScreen

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/CycleReviewScreen.kt`

**Step 1: Create the review screen composable**

```kotlin
@Composable
fun CycleReviewScreen(
    cycle: TrainingCycle,
    routines: List<Routine>,
    onBack: () -> Unit,
    onSave: () -> Unit
) {
    var expandedDays by remember { mutableStateOf(setOf<Int>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review Your Cycle") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = onSave,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text("Save Cycle")
                }
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(cycle.days, key = { it.id }) { day ->
                val routine = day.routineId?.let { id -> routines.find { it.id == id } }
                val isExpanded = day.dayNumber in expandedDays

                CycleReviewDayCard(
                    day = day,
                    routine = routine,
                    isExpanded = isExpanded,
                    onToggleExpand = {
                        expandedDays = if (isExpanded) {
                            expandedDays - day.dayNumber
                        } else {
                            expandedDays + day.dayNumber
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun CycleReviewDayCard(
    day: CycleDay,
    routine: Routine?,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Header row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(enabled = routine != null) { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Day ${day.dayNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.width(16.dp))

                if (day.routineId == null) {
                    Text("💤 Rest", style = MaterialTheme.typography.titleMedium)
                } else {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            routine?.name ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        // Modifier badges
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            day.echoLevel?.let {
                                Badge { Text("Echo:${it.displayName}") }
                            }
                            day.weightProgressionPercent?.let {
                                Badge { Text("${if (it >= 0) "+" else ""}${it.toInt()}%") }
                            }
                            day.eccentricLoadPercent?.let {
                                Badge { Text("Ecc:$it%") }
                            }
                        }
                    }

                    Icon(
                        if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                        "Toggle details"
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(visible = isExpanded && routine != null) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surfaceContainerLow)
                        .padding(16.dp)
                ) {
                    routine?.exercises?.forEach { exercise ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp)
                        ) {
                            Text("•", modifier = Modifier.width(16.dp))
                            Text(
                                exercise.exerciseName,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                "${exercise.sets.size}×${exercise.sets.firstOrNull()?.targetReps ?: "?"}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Add navigation route**

Add to NavigationRoutes.kt:
```kotlin
object CycleReview : NavigationRoutes("cycleReview/{cycleId}")
```

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/CycleReviewScreen.kt
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/navigation/
git commit -m "feat(ui): add CycleReviewScreen with collapsible timeline"
```

---

## Phase 5: Active Cycle Day Strip

### Task 10: Create DayStripComponent

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/DayStrip.kt`

**Step 1: Create the day strip composable**

```kotlin
@Composable
fun DayStrip(
    days: List<CycleDay>,
    progress: CycleProgress,
    currentSelection: Int,
    onDaySelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp)
    ) {
        items(days, key = { it.id }) { day ->
            DayChip(
                dayNumber = day.dayNumber,
                isRestDay = day.isRestDay,
                state = when {
                    day.dayNumber in progress.completedDays -> DayState.COMPLETED
                    day.dayNumber in progress.missedDays -> DayState.MISSED
                    day.dayNumber == progress.currentDayNumber -> DayState.CURRENT
                    else -> DayState.UPCOMING
                },
                isSelected = day.dayNumber == currentSelection,
                onClick = { onDaySelected(day.dayNumber) }
            )
        }
    }
}

enum class DayState {
    COMPLETED,  // Green with checkmark
    MISSED,     // Red with X
    CURRENT,    // Blue/primary filled
    UPCOMING    // Gray outline
}

@Composable
fun DayChip(
    dayNumber: Int,
    isRestDay: Boolean,
    state: DayState,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = when (state) {
        DayState.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
        DayState.MISSED -> MaterialTheme.colorScheme.errorContainer
        DayState.CURRENT -> MaterialTheme.colorScheme.primary
        DayState.UPCOMING -> Color.Transparent
    }

    val contentColor = when (state) {
        DayState.COMPLETED -> MaterialTheme.colorScheme.onPrimaryContainer
        DayState.MISSED -> MaterialTheme.colorScheme.onErrorContainer
        DayState.CURRENT -> MaterialTheme.colorScheme.onPrimary
        DayState.UPCOMING -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val borderColor = if (isSelected && state != DayState.CURRENT) {
        MaterialTheme.colorScheme.primary
    } else if (state == DayState.UPCOMING) {
        MaterialTheme.colorScheme.outline
    } else {
        Color.Transparent
    }

    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = backgroundColor,
        border = BorderStroke(2.dp, borderColor),
        modifier = Modifier.size(48.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            when {
                isRestDay -> Text("💤", style = MaterialTheme.typography.titleMedium)
                state == DayState.COMPLETED -> Icon(
                    Icons.Default.Check,
                    null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                state == DayState.MISSED -> Icon(
                    Icons.Default.Close,
                    null,
                    tint = contentColor,
                    modifier = Modifier.size(20.dp)
                )
                else -> Text(
                    "$dayNumber",
                    color = contentColor,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
```

**Step 2: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/DayStrip.kt
git commit -m "feat(ui): add DayStrip component with state visualization"
```

---

### Task 11: Integrate DayStrip into TrainingCyclesScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt`

**Step 1: Add selected day state**

```kotlin
var selectedDayNumber by remember { mutableStateOf<Int?>(null) }

// When active cycle changes, reset selection to current day
LaunchedEffect(activeCycle, cycleProgress) {
    selectedDayNumber = cycleProgress[activeCycle?.id]?.currentDayNumber
}
```

**Step 2: Update ActiveCycleCard to include DayStrip**

Find the `ActiveCycleCard` composable and add the DayStrip below the current workout preview:

```kotlin
// After workout preview section
Spacer(Modifier.height(16.dp))

DayStrip(
    days = cycle.days,
    progress = progress ?: CycleProgress.create(cycleId = cycle.id),
    currentSelection = selectedDayNumber ?: progress?.currentDayNumber ?: 1,
    onDaySelected = { dayNumber ->
        selectedDayNumber = dayNumber
        // Update the displayed workout preview based on selection
    }
)
```

**Step 3: Make workout preview respond to selection**

Update the workout preview to show the selected day's routine, not just the current day.

**Step 4: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt
git commit -m "feat(ui): integrate DayStrip into active cycle display"
```

---

## Phase 6: Auto-Advance Logic

### Task 12: Add auto-advance check on app open

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/TrainingCycleRepository.kt`

**Step 1: Add repository method for auto-advance**

```kotlin
// In TrainingCycleRepository interface
suspend fun checkAndAutoAdvance(cycleId: String): CycleProgress?

// In SqlDelightTrainingCycleRepository
override suspend fun checkAndAutoAdvance(cycleId: String): CycleProgress? {
    val progress = getCycleProgress(cycleId) ?: return null
    val cycle = getCycleById(cycleId) ?: return null

    if (progress.shouldAutoAdvance()) {
        val updated = progress.advanceToNextDay(cycle.days.size, markMissed = true)
        updateCycleProgress(updated)
        return updated
    }
    return progress
}
```

**Step 2: Call auto-advance on screen launch**

```kotlin
// In TrainingCyclesScreen LaunchedEffect
LaunchedEffect(activeCycle) {
    activeCycle?.let { cycle ->
        cycleRepository.checkAndAutoAdvance(cycle.id)?.let { updated ->
            cycleProgress = cycleProgress + (cycle.id to updated)
        }
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/data/repository/
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt
git commit -m "feat(cycle): add 24h auto-advance logic"
```

---

### Task 13: Update workout completion to mark days

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt`

**Step 1: Find workout completion handling**

Locate where workout completion is handled (likely in `completeWorkout()` or similar).

**Step 2: Add cycle progress update**

```kotlin
// After workout completion logic
activeCycleId?.let { cycleId ->
    scope.launch {
        val cycle = cycleRepository.getCycleById(cycleId)
        val progress = cycleRepository.getCycleProgress(cycleId)
        if (cycle != null && progress != null) {
            // Find which day was just completed based on routineId
            val completedDay = cycle.days.find { it.routineId == completedRoutineId }
            completedDay?.let { day ->
                val updated = progress.markDayCompleted(day.dayNumber, cycle.days.size)
                cycleRepository.updateCycleProgress(updated)
            }
        }
    }
}
```

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/viewmodel/MainViewModel.kt
git commit -m "feat(cycle): mark days completed after workout finish"
```

---

## Phase 7: Integration & Polish

### Task 14: Update TrainingCyclesScreen FAB to use new flow

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt`

**Step 1: Change FAB action**

Update the FloatingActionButton to navigate to DayCountPicker instead of showing template dialog:

```kotlin
FloatingActionButton(
    onClick = { navController.navigate(NavigationRoutes.DayCountPicker.route) }
) {
    Icon(Icons.Default.Add, "Create Cycle")
}
```

**Step 2: Keep template dialog as secondary option**

Add a menu or long-press option to access templates for users who prefer pre-built cycles.

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/TrainingCyclesScreen.kt
git commit -m "feat(ui): update cycle creation to use new flow"
```

---

### Task 15: Wire up CycleEditor Save to Review screen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/CycleEditorScreen.kt`

**Step 1: Change Save button to navigate to Review**

Instead of saving directly, navigate to CycleReviewScreen:

```kotlin
TextButton(onClick = {
    // Store cycle in temp state or pass via navigation args
    navController.navigate(NavigationRoutes.CycleReview.createRoute(cycleId))
}) {
    Text("Review", fontWeight = FontWeight.Bold)
}
```

**Step 2: Handle cycle state passing**

Use a shared ViewModel or navigation argument serialization to pass the cycle data to the review screen.

**Step 3: Verify compilation**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/CycleEditorScreen.kt
git commit -m "feat(ui): wire cycle editor to review screen"
```

---

### Task 16: End-to-end testing

**Step 1: Build and run on Android**

```bash
./gradlew :androidApp:installDebug
```

**Step 2: Test cycle creation flow**

1. Navigate to Training Cycles
2. Tap FAB → Day Count Picker appears
3. Select 7 days → Editor opens with 7 empty slots
4. Drag routines to day slots
5. Tap assigned day → Config sheet opens
6. Set modifiers and apply
7. Tap Review → Timeline shows cycle
8. Tap Save → Cycle saved

**Step 3: Test active cycle experience**

1. Activate a cycle
2. Verify day strip shows with correct states
3. Tap different day chips → Preview updates
4. Complete a workout → Day marked completed
5. Wait 24h (or manually trigger) → Auto-advance works

**Step 4: Commit final fixes**

```bash
git add .
git commit -m "fix: polish cycle UX edge cases"
```

---

## Summary

**Total Tasks:** 16

**Estimated Implementation Order:**
1. Tasks 1-4: Data model & database (foundation)
2. Tasks 5-6: Day count picker (entry point)
3. Task 7: Config sheet component
4. Task 8: Cycle editor rewrite (biggest task)
5. Tasks 9-10: Review screen & day strip
6. Task 11: Day strip integration
7. Tasks 12-13: Auto-advance logic
8. Tasks 14-16: Integration & testing

**Key Dependencies:**
- Task 8 depends on Tasks 1-7
- Task 11 depends on Task 10
- Task 13 depends on Task 12
- Task 16 depends on all previous tasks
