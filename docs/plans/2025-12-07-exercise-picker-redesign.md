# Exercise Picker Redesign: The Unified Shelf

**Date:** 2025-12-07
**Status:** Approved
**Target File:** `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/ExercisePicker.kt`

## Problem Statement

The current ExercisePicker suffers from "Header Bloat." Before seeing any exercises, users scroll past:
1. Search bar
2. Two toggle switches (Favorites, My Exercises)
3. "Muscle Groups" label + chip row
4. "Equipment" label + chip row

This consumes ~200dp of vertical space (~40% of screen on mobile), leaving minimal room for actual content.

## Design Goals

- Compress filtering controls from 4 rows to 1 row
- Maximize exercise list visibility
- Enable fast navigation for 100+ exercise libraries
- Maintain discoverability while reducing visual clutter

---

## Architecture

### 1. Unified Filter Shelf

**Structure:**
```
[✕] [⭐ Favorites] [👤 Custom] │ [Chest] [Back] [Legs] [Shoulders] [Arms] [Core] │ [Long Bar] [Short Bar] [Handles] ...
```

**Components:**
| Element | Behavior |
|---------|----------|
| Clear button (✕) | Visible only when filters active. Clears all selections. |
| Favorites chip | Toggle. Icon + label. |
| Custom chip | Toggle. Icon + label. |
| Vertical divider | Visual separator between filter categories |
| Muscle chips | Multi-select. Tap multiple to combine (e.g., Chest + Shoulders) |
| Equipment chips | Multi-select. Same behavior as muscle chips |

**Specifications:**
- Container: `LazyRow` with horizontal scroll
- Height: 48dp fixed (consistent touch targets)
- Chip spacing: 8dp
- Content padding: 16dp horizontal
- Selected state: `primaryContainer` background color

**Filter Logic:**
```kotlin
val filteredExercises = allExercises.filter { exercise ->
    val matchesSearch = searchQuery.isBlank() ||
        exercise.name.contains(searchQuery, ignoreCase = true)
    val matchesMuscle = selectedMuscles.isEmpty() ||
        selectedMuscles.any { exercise.muscleGroups.contains(it, ignoreCase = true) }
    val matchesEquipment = selectedEquipment.isEmpty() ||
        selectedEquipment.any { equipmentMatches(exercise.equipment, it) }
    val matchesFavorites = !showFavoritesOnly || exercise.isFavorite
    val matchesCustom = !showCustomOnly || exercise.isCustom

    matchesSearch && matchesMuscle && matchesEquipment && matchesFavorites && matchesCustom
}
```

**Space Savings:** ~200dp → ~56dp = **144dp reclaimed** for the list.

---

### 2. Grouped List with Alphabet Navigation

**Sticky Headers:**
- Exercises grouped by first letter (A, B, C...)
- Header sticks to top while scrolling through section
- Style: `labelLarge`, `primary` color
- Background: `surface` at 95% opacity (semi-transparent)

**Alphabet Strip:**
- Vertical column positioned at right edge (`Alignment.CenterEnd`)
- Contains only letters that have exercises (skip empty)
- Font: ~12sp, minimal padding
- Background: semi-transparent overlay
- Interaction: Tap letter → `LazyListState.scrollToItem()` to section

**Implementation:**
```kotlin
@Composable
fun GroupedExerciseList(
    exercises: List<Exercise>,
    listState: LazyListState,
    onExerciseSelected: (Exercise) -> Unit,
    onToggleFavorite: (Exercise) -> Unit
) {
    val groupedExercises = remember(exercises) {
        exercises.groupBy { it.name.first().uppercaseChar() }
            .toSortedMap()
    }

    val sectionIndices = remember(groupedExercises) {
        var index = 0
        groupedExercises.mapValues { (_, list) ->
            val sectionIndex = index
            index += 1 + list.size // header + items
            sectionIndex
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(state = listState) {
            groupedExercises.forEach { (letter, exerciseList) ->
                stickyHeader(key = "header_$letter") {
                    LetterHeader(letter = letter.toString())
                }
                items(exerciseList, key = { it.id ?: it.name }) { exercise ->
                    SwipeableExerciseRow(
                        exercise = exercise,
                        onSelect = { onExerciseSelected(exercise) },
                        onToggleFavorite = { onToggleFavorite(exercise) }
                    )
                }
            }
        }

        AlphabetStrip(
            letters = groupedExercises.keys.toList(),
            onLetterTap = { letter ->
                sectionIndices[letter]?.let { index ->
                    coroutineScope.launch {
                        listState.animateScrollToItem(index)
                    }
                }
            },
            modifier = Modifier.align(Alignment.CenterEnd)
        )
    }
}
```

---

### 3. Enhanced List Item

**Layout:**
```
┌─────────────────────────────────────────────────────┐
│ ┌────────┐                                          │
│ │        │  Bench Press •                      3x   │
│ │  IMG   │  Chest • Long Bar                        │
│ │ 64x64  │                                          │
│ └────────┘                                          │
└─────────────────────────────────────────────────────┘
```

**Components:**

| Element | Specification |
|---------|---------------|
| Thumbnail | 64dp × 64dp, 12dp corner radius. Tap opens video dialog. |
| Title | `titleMedium`. Gold dot (6dp) inline if favorited. |
| Subtitle | `bodySmall`, `onSurfaceVariant`. Format: "Muscle • Equipment" |
| Count badge | Compact "3x" if `timesPerformed > 0` |
| Custom indicator | Accent-colored ring around thumbnail border |

**Swipe-to-Favorite:**
```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeableExerciseRow(
    exercise: Exercise,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onToggleFavorite()
            }
            false // Don't actually dismiss; reset position
        }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.CenterStart
            ) {
                Icon(
                    imageVector = if (exercise.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Toggle favorite",
                    modifier = Modifier.padding(start = 24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) {
        ExerciseRowContent(
            exercise = exercise,
            onClick = onSelect
        )
    }
}
```

---

### 4. Search Bar

**Style:**
- Floating appearance with rounded corners (28dp radius)
- `SearchBar` composable or styled `TextField` with filled variant
- Pinned below top bar, above filter shelf

**Behavior:**
- Filters persist and combine with search (AND logic)
- Trailing clear button (✕) appears when query is not empty
- Filter shelf remains visible during search

**Layout Order:**
```
┌─────────────────────────────────┐
│  ← Select Exercise          +   │  Top bar
├─────────────────────────────────┤
│  🔍 Search exercises...         │  Search bar
├─────────────────────────────────┤
│ [⭐] [👤] │ [Chest] [Back] ...   │  Filter shelf
├─────────────────────────────────┤
│ A ──────────────────────────    │
│ [img] Arnold Press         2x A │  List + alphabet strip
│ [img] Assisted Dip         3x B │
│ B ──────────────────────────  C │
└─────────────────────────────────┘
```

---

### 5. Empty States

| Condition | Display |
|-----------|---------|
| No matches (filters active) | "No exercises found" + "Clear filters" button |
| Custom tab empty | "No custom exercises yet" + "Create Exercise" button |
| Loading | Centered `CircularProgressIndicator` + "Loading exercises..." |

---

## State Management

```kotlin
// In ExercisePickerContent or ViewModel
var searchQuery by remember { mutableStateOf("") }
var showFavoritesOnly by remember { mutableStateOf(false) }
var showCustomOnly by remember { mutableStateOf(false) }
var selectedMuscles by remember { mutableStateOf(setOf<String>()) }
var selectedEquipment by remember { mutableStateOf(setOf<String>()) }

val hasActiveFilters = showFavoritesOnly || showCustomOnly ||
    selectedMuscles.isNotEmpty() || selectedEquipment.isNotEmpty()

fun clearAllFilters() {
    showFavoritesOnly = false
    showCustomOnly = false
    selectedMuscles = emptySet()
    selectedEquipment = emptySet()
}
```

---

## New Components Required

| Component | Purpose |
|-----------|---------|
| `ExerciseFilterShelf` | Unified horizontal filter row |
| `LetterHeader` | Sticky alphabetical section header |
| `AlphabetStrip` | Vertical tap-to-jump navigation |
| `SwipeableExerciseRow` | List item with swipe-to-favorite |
| `ExerciseRowContent` | The visual content of a list item |

---

## Migration Notes

- Current `ExercisePickerContent` signature can remain; internal implementation changes
- Filter state changes from single-select (`String`) to multi-select (`Set<String>`)
- `showFavoritesOnly` / `showCustomOnly` behavior unchanged
- Video dialog and create/edit flows remain as-is

---

## Visual Summary

**Before:** 4 rows of controls (~200dp) → flat unsorted list

**After:** 1 row of controls (~56dp) → alphabetically grouped list with fast navigation

**Net gain:** +144dp of list visibility, faster navigation, cleaner aesthetic
