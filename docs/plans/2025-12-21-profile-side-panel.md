# Profile Side Panel Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace the ProfileSpeedDial FAB with a slide-in panel from the right edge, triggered by a chevron handle or edge swipe.

**Architecture:** ModalDrawerSheet-based side panel with gesture detection. Reuses existing ProfileAvatar and color system. Adds edit/delete functionality via long-press context menu.

**Tech Stack:** Compose Multiplatform, Material3 ModalDrawerSheet, Compose gestures

---

## Task 1: Create EditProfileDialog Component

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/EditProfileDialog.kt`

**Step 1: Create the EditProfileDialog file**

```kotlin
package com.devil.phoenixproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devil.phoenixproject.data.repository.UserProfile
import com.devil.phoenixproject.data.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Dialog for editing an existing user profile.
 * Allows changing name and color.
 */
@Composable
fun EditProfileDialog(
    profile: UserProfile,
    profileRepository: UserProfileRepository,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(profile.name) }
    var selectedColorIndex by remember { mutableStateOf(profile.colorIndex) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Profile") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text(
                    "Color",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileColors.forEachIndexed { index, color ->
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(color)
                                .then(
                                    if (index == selectedColorIndex) {
                                        Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                    } else {
                                        Modifier
                                    }
                                )
                                .clickable { selectedColorIndex = index },
                            contentAlignment = Alignment.Center
                        ) {
                            if (index == selectedColorIndex) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .background(Color.White, CircleShape)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (name.isNotBlank()) {
                        scope.launch {
                            profileRepository.updateProfile(profile.id, name.trim(), selectedColorIndex)
                        }
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank()
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

**Step 2: Verify file compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/EditProfileDialog.kt
git commit -m "feat: add EditProfileDialog component for profile editing"
```

---

## Task 2: Create DeleteProfileDialog Component

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/DeleteProfileDialog.kt`

**Step 1: Create the DeleteProfileDialog file**

```kotlin
package com.devil.phoenixproject.presentation.components

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import com.devil.phoenixproject.data.repository.UserProfile
import com.devil.phoenixproject.data.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Confirmation dialog for deleting a user profile.
 */
@Composable
fun DeleteProfileDialog(
    profile: UserProfile,
    profileRepository: UserProfileRepository,
    scope: CoroutineScope,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Profile") },
        text = {
            Text("Are you sure you want to delete \"${profile.name}\"? This cannot be undone.")
        },
        confirmButton = {
            TextButton(
                onClick = {
                    scope.launch {
                        profileRepository.deleteProfile(profile.id)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error
                )
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
```

**Step 2: Verify file compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/DeleteProfileDialog.kt
git commit -m "feat: add DeleteProfileDialog confirmation component"
```

---

## Task 3: Create ProfileListItem Component

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/ProfileListItem.kt`

**Step 1: Create the ProfileListItem file**

```kotlin
package com.devil.phoenixproject.presentation.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devil.phoenixproject.data.repository.UserProfile

/**
 * Individual profile row for the side panel.
 * Shows avatar, name, and active indicator.
 * Supports tap to select and long-press for context menu.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileListItem(
    profile: UserProfile,
    isActive: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val profileColor = ProfileColors.getOrElse(profile.colorIndex) { ProfileColors[0] }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        color = if (isActive) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Avatar
            Surface(
                modifier = Modifier.size(40.dp),
                shape = CircleShape,
                color = profileColor
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }

            // Name
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )

            // Active indicator
            if (isActive) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = "Active",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
```

**Step 2: Verify file compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/ProfileListItem.kt
git commit -m "feat: add ProfileListItem component with long-press support"
```

---

## Task 4: Create ProfileSidePanel Component

**Files:**
- Create: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/ProfileSidePanel.kt`

**Step 1: Create the ProfileSidePanel file**

```kotlin
package com.devil.phoenixproject.presentation.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.devil.phoenixproject.data.repository.UserProfile
import com.devil.phoenixproject.data.repository.UserProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private val PANEL_WIDTH = 200.dp
private val HANDLE_WIDTH = 24.dp
private val HANDLE_HEIGHT = 48.dp
private val EDGE_SWIPE_THRESHOLD = 20.dp

/**
 * Slide-in profile panel from right edge.
 * Replaces ProfileSpeedDial FAB with a less intrusive side panel.
 */
@Composable
fun ProfileSidePanel(
    profiles: List<UserProfile>,
    activeProfile: UserProfile?,
    profileRepository: UserProfileRepository,
    scope: CoroutineScope,
    onAddProfile: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isOpen by remember { mutableStateOf(false) }
    var showContextMenu by remember { mutableStateOf<UserProfile?>(null) }
    var showEditDialog by remember { mutableStateOf<UserProfile?>(null) }
    var showDeleteDialog by remember { mutableStateOf<UserProfile?>(null) }

    val density = LocalDensity.current
    val panelWidthPx = with(density) { PANEL_WIDTH.toPx() }

    // Panel offset animation
    val panelOffset by animateDpAsState(
        targetValue = if (isOpen) 0.dp else PANEL_WIDTH,
        animationSpec = tween(durationMillis = 300),
        label = "panelOffset"
    )

    Box(modifier = modifier.fillMaxSize()) {
        // Scrim (when open)
        if (isOpen) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .clickable { isOpen = false }
                    .zIndex(1f)
            )
        }

        // Edge swipe detection zone
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(EDGE_SWIPE_THRESHOLD)
                .fillMaxHeight()
                .pointerInput(Unit) {
                    detectHorizontalDragGestures { _, dragAmount ->
                        if (dragAmount < -10) {
                            isOpen = true
                        }
                    }
                }
        )

        // Panel + Handle
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .offset { IntOffset(panelOffset.roundToPx(), 0) }
                .zIndex(2f)
        ) {
            // Chevron handle (visible when closed)
            Surface(
                modifier = Modifier
                    .offset(x = -HANDLE_WIDTH)
                    .align(Alignment.CenterStart)
                    .width(HANDLE_WIDTH)
                    .height(HANDLE_HEIGHT)
                    .clickable { isOpen = !isOpen },
                shape = RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp),
                color = ProfileColors.getOrElse(activeProfile?.colorIndex ?: 0) { ProfileColors[0] }.copy(alpha = 0.9f),
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                        contentDescription = if (isOpen) "Close profiles" else "Open profiles",
                        tint = Color.White
                    )
                }
            }

            // Main panel
            Surface(
                modifier = Modifier
                    .width(PANEL_WIDTH)
                    .fillMaxHeight()
                    .pointerInput(Unit) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (dragAmount > 20) {
                                isOpen = false
                            }
                        }
                    },
                shape = RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                shadowElevation = 8.dp
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    // Header
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp)
                    )

                    HorizontalDivider()

                    // Profile list
                    LazyColumn(
                        modifier = Modifier.weight(1f)
                    ) {
                        items(profiles, key = { it.id }) { profile ->
                            val isActive = profile.id == activeProfile?.id

                            Box {
                                ProfileListItem(
                                    profile = profile,
                                    isActive = isActive,
                                    onClick = {
                                        scope.launch { profileRepository.setActiveProfile(profile.id) }
                                        isOpen = false
                                    },
                                    onLongClick = {
                                        showContextMenu = profile
                                    }
                                )

                                // Context menu dropdown
                                DropdownMenu(
                                    expanded = showContextMenu?.id == profile.id,
                                    onDismissRequest = { showContextMenu = null }
                                ) {
                                    DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            showEditDialog = profile
                                            showContextMenu = null
                                        },
                                        leadingIcon = {
                                            Icon(Icons.Default.Edit, contentDescription = null)
                                        }
                                    )
                                    // Don't show delete for default profile
                                    if (profile.id != "default") {
                                        DropdownMenuItem(
                                            text = { Text("Delete") },
                                            onClick = {
                                                showDeleteDialog = profile
                                                showContextMenu = null
                                            },
                                            leadingIcon = {
                                                Icon(
                                                    Icons.Default.Delete,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.error
                                                )
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider()

                    // Add profile button
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                isOpen = false
                                onAddProfile()
                            },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Surface(
                                modifier = Modifier.size(40.dp),
                                shape = androidx.compose.foundation.shape.CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                            Text(
                                text = "Add Profile",
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }

    // Edit dialog
    showEditDialog?.let { profile ->
        EditProfileDialog(
            profile = profile,
            profileRepository = profileRepository,
            scope = scope,
            onDismiss = { showEditDialog = null }
        )
    }

    // Delete dialog
    showDeleteDialog?.let { profile ->
        DeleteProfileDialog(
            profile = profile,
            profileRepository = profileRepository,
            scope = scope,
            onDismiss = { showDeleteDialog = null }
        )
    }
}
```

**Step 2: Verify file compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/components/ProfileSidePanel.kt
git commit -m "feat: add ProfileSidePanel slide-in component"
```

---

## Task 5: Update JustLiftScreen to Use ProfileSidePanel

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/JustLiftScreen.kt`

**Step 1: Update imports**

Replace the ProfileSpeedDial import with ProfileSidePanel:

Find:
```kotlin
import com.devil.phoenixproject.presentation.components.ProfileSpeedDial
```

Replace with:
```kotlin
import com.devil.phoenixproject.presentation.components.ProfileSidePanel
```

**Step 2: Remove floatingActionButton from Scaffold**

Find:
```kotlin
    Scaffold(
        floatingActionButton = {
            ProfileSpeedDial(
                profiles = profiles,
                activeProfile = activeProfile,
                onProfileSelected = { profile ->
                    scope.launch { profileRepository.setActiveProfile(profile.id) }
                },
                onAddProfile = { showAddProfileDialog = true }
            )
        }
    ) { padding ->
```

Replace with:
```kotlin
    Scaffold { padding ->
```

**Step 3: Add ProfileSidePanel as overlay inside Scaffold**

Find the closing of the main Box (before `// Add Profile Dialog`):
```kotlin
            // Connection error dialog (ConnectingOverlay removed - status shown in top bar button)
            connectionError?.let { error ->
                com.devil.phoenixproject.presentation.components.ConnectionErrorDialog(
                    message = error,
                    onDismiss = { viewModel.clearConnectionError() }
                )
            }
```

After this block, add the ProfileSidePanel:
```kotlin
            // Connection error dialog (ConnectingOverlay removed - status shown in top bar button)
            connectionError?.let { error ->
                com.devil.phoenixproject.presentation.components.ConnectionErrorDialog(
                    message = error,
                    onDismiss = { viewModel.clearConnectionError() }
                )
            }

            // Profile side panel
            ProfileSidePanel(
                profiles = profiles,
                activeProfile = activeProfile,
                profileRepository = profileRepository,
                scope = scope,
                onAddProfile = { showAddProfileDialog = true }
            )
```

**Step 4: Verify file compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/JustLiftScreen.kt
git commit -m "refactor: replace ProfileSpeedDial with ProfileSidePanel in JustLiftScreen"
```

---

## Task 6: Update EnhancedMainScreen to Use ProfileSidePanel

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/EnhancedMainScreen.kt`

**Step 1: Update imports**

Find:
```kotlin
import com.devil.phoenixproject.presentation.components.ProfileSpeedDial
```

Replace with:
```kotlin
import com.devil.phoenixproject.presentation.components.ProfileSidePanel
```

**Step 2: Remove showProfileSpeedDial logic**

Find and remove:
```kotlin
    // Only show ProfileSpeedDial on Home screen (JustLift has its own)
    val showProfileSpeedDial = remember(currentRoute) {
        currentRoute == NavigationRoutes.Home.route
    }
```

**Step 3: Remove floatingActionButton from Scaffold**

Find:
```kotlin
        floatingActionButton = {
            if (showProfileSpeedDial) {
                ProfileSpeedDial(
                    profiles = profiles,
                    activeProfile = activeProfile,
                    onProfileSelected = { profile ->
                        scope.launch { profileRepository.setActiveProfile(profile.id) }
                    },
                    onAddProfile = { showAddProfileDialog = true }
                )
            }
        },
```

Replace with nothing (remove the entire floatingActionButton block and its trailing comma). The Scaffold should now start with:
```kotlin
    Scaffold(
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
```

**Step 4: Add ProfileSidePanel after NavGraph**

Find:
```kotlin
    ) { padding ->
        // Use proper padding to account for TopAppBar and system bars
        NavGraph(
            navController = navController,
            viewModel = viewModel,
            exerciseRepository = exerciseRepository,
            themeMode = themeMode,
            onThemeModeChange = onThemeModeChange,
            modifier = Modifier.padding(padding)
        )
    }
```

Replace with:
```kotlin
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize()) {
            // Use proper padding to account for TopAppBar and system bars
            NavGraph(
                navController = navController,
                viewModel = viewModel,
                exerciseRepository = exerciseRepository,
                themeMode = themeMode,
                onThemeModeChange = onThemeModeChange,
                modifier = Modifier.padding(padding)
            )

            // Profile side panel (only on Home screen)
            if (currentRoute == NavigationRoutes.Home.route) {
                ProfileSidePanel(
                    profiles = profiles,
                    activeProfile = activeProfile,
                    profileRepository = profileRepository,
                    scope = scope,
                    onAddProfile = { showAddProfileDialog = true }
                )
            }
        }
    }
```

**Step 5: Add missing import for fillMaxSize**

Ensure this import exists (should already be there via `foundation.layout.*`):
```kotlin
import androidx.compose.foundation.layout.*
```

**Step 6: Verify file compiles**

Run: `./gradlew :shared:compileKotlinMetadata`
Expected: BUILD SUCCESSFUL

**Step 7: Commit**

```bash
git add shared/src/commonMain/kotlin/com/devil/phoenixproject/presentation/screen/EnhancedMainScreen.kt
git commit -m "refactor: replace ProfileSpeedDial with ProfileSidePanel in EnhancedMainScreen"
```

---

## Task 7: Build and Test

**Step 1: Full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 2: Run Android app**

Run: `./gradlew :androidApp:installDebug`

**Step 3: Manual verification checklist**

- [ ] Chevron handle visible on right edge of Home screen
- [ ] Chevron handle visible on right edge of Just Lift screen
- [ ] Tapping chevron opens panel
- [ ] Swiping left from right edge opens panel
- [ ] Scrim appears when panel is open
- [ ] Tapping scrim closes panel
- [ ] Swiping right on panel closes panel
- [ ] Profile list shows all profiles
- [ ] Active profile has checkmark
- [ ] Tapping profile selects it and closes panel
- [ ] Long-press shows Edit/Delete menu
- [ ] Edit dialog pre-fills name and color
- [ ] Color picker works
- [ ] Delete confirmation works
- [ ] Default profile cannot be deleted
- [ ] Add Profile opens dialog

**Step 4: Final commit**

```bash
git add -A
git commit -m "feat: complete ProfileSidePanel implementation

Replaces FAB with slide-in panel:
- Chevron handle on right edge
- Edge swipe gesture support
- Long-press for edit/delete
- Color picker in edit dialog
- Delete confirmation"
```

---

## Summary

| Task | Description | Files |
|------|-------------|-------|
| 1 | EditProfileDialog | Create new |
| 2 | DeleteProfileDialog | Create new |
| 3 | ProfileListItem | Create new |
| 4 | ProfileSidePanel | Create new |
| 5 | JustLiftScreen | Replace FAB |
| 6 | EnhancedMainScreen | Replace FAB |
| 7 | Build and Test | Verify all |
