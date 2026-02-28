package com.devil.phoenixproject.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ACE Theme Colors
private val PhoenixBlack = Color(0xFF080A0F)
private val Slate900 = Color(0xFF0F172A)
private val Slate800 = Color(0xFF1E293B)
private val FlameOrange = Color(0xFFFF6B00)
private val FlameYellow = Color(0xFFFFAB00)
private val ItalianGold = Color(0xFFD4AF37)
private val TextPrimary = Color(0xFFFFFFFF)
private val TextSecondary = Color(0xFF94A3B8)
private val GlassWhite = Color(0x1AFFFFFF)
private val GlassBorder = Color(0x33FFFFFF)

/**
 * ACE Onboarding Flow
 */
@Composable
fun OnboardingScreen(
    onComplete: (appName: String, goals: Set<String>, hasVitruvian: Boolean, hasDumbbells: Boolean, hasTRX: Boolean, hasPullUpBar: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentStep by remember { mutableIntStateOf(0) }
    
    // User preferences
    var selectedAppName by remember { mutableStateOf("AUREA") }
    var selectedGoals by remember { mutableStateOf(setOf<String>()) }
    var hasVitruvian by remember { mutableStateOf(true) }
    var hasDumbbells by remember { mutableStateOf(false) }
    var hasTRX by remember { mutableStateOf(false) }
    var hasPullUpBar by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(PhoenixBlack, Slate900)
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(24.dp)
        ) {
            // Progress indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf("Welcome", "Goals", "Equipment", "Connect").forEachIndexed { index, _ ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .size(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (index <= currentStep) FlameOrange 
                                else GlassWhite
                            )
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Current step content using when
            when (currentStep) {
                0 -> WelcomeStepContent(
                    selectedAppName = selectedAppName,
                    onAppNameSelect = { selectedAppName = it },
                    onNext = { currentStep = 1 }
                )
                1 -> GoalsStepContent(
                    selectedGoals = selectedGoals,
                    onGoalsChange = { selectedGoals = it },
                    onNext = { currentStep = 2 },
                    onBack = { currentStep = 0 }
                )
                2 -> EquipmentStepContent(
                    hasVitruvian = hasVitruvian,
                    hasDumbbells = hasDumbbells,
                    hasTRX = hasTRX,
                    hasPullUpBar = hasPullUpBar,
                    onVitruvianChange = { hasVitruvian = it },
                    onDumbbellsChange = { hasDumbbells = it },
                    onTRXChange = { hasTRX = it },
                    onPullUpBarChange = { hasPullUpBar = it },
                    onNext = { currentStep = 3 },
                    onBack = { currentStep = 1 }
                )
                3 -> ConnectStepContent(
                    appName = selectedAppName,
                    goals = selectedGoals,
                    hasVitruvian = hasVitruvian,
                    hasDumbbells = hasDumbbells,
                    hasTRX = hasTRX,
                    hasPullUpBar = hasPullUpBar,
                    onComplete = { appName, goals, vitruvian, dumbbells, trx, pullUpBar ->
                        onComplete(appName, goals, vitruvian, dumbbells, trx, pullUpBar)
                    },
                    onBack = { currentStep = 2 }
                )
            }
        }
    }
}

@Composable
private fun WelcomeStepContent(
    selectedAppName: String,
    onAppNameSelect: (String) -> Unit,
    onNext: () -> Unit
) {
    val appNames = listOf("AUREA", "ACE", "VITRUVIANO")

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))
        
        Text(
            text = "WELCOME TO",
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            appNames.forEach { name ->
                val isSelected = name == selectedAppName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (isSelected) FlameOrange else GlassWhite)
                        .border(
                            width = 1.dp,
                            color = if (isSelected) FlameOrange else GlassBorder,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .clickable { onAppNameSelect(name) }
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                ) {
                    Text(
                        text = name,
                        color = if (isSelected) Color.White else TextPrimary,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Your AI-Powered\nFitness Companion",
            fontSize = 28.sp,
            fontWeight = FontWeight.Light,
            color = TextPrimary,
            textAlign = TextAlign.Center,
            lineHeight = 36.sp
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Smart workouts. Real results.\nConnected to your Vitruvian.",
            fontSize = 14.sp,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.weight(1f))

        Button(
            onClick = onNext,
            colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("CONTINUE", fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun GoalsStepContent(
    selectedGoals: Set<String>,
    onGoalsChange: (Set<String>) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    val goals = listOf(
        "Build Muscle" to Icons.Filled.FitnessCenter,
        "Get Stronger" to Icons.Filled.TrendingUp,
        "Lose Weight" to Icons.Filled.MonitorWeight,
        "Endurance" to Icons.Filled.DirectionsRun,
        "General Health" to Icons.Filled.Favorite
    )

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "YOUR GOALS",
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What do you want to achieve?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Goals grid
        goals.forEach { (goal, icon) ->
            val isSelected = goal in selectedGoals
            GoalCard(
                text = goal,
                icon = icon,
                isSelected = isSelected,
                onClick = {
                    val newGoals = if (goal in selectedGoals) {
                        selectedGoals - goal
                    } else {
                        selectedGoals + goal
                    }
                    onGoalsChange(newGoals)
                }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("BACK")
            }
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("CONTINUE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun GoalCard(
    text: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) FlameOrange.copy(alpha = 0.2f) else Slate800)
            .border(
                width = 1.dp,
                color = if (isSelected) FlameOrange else GlassBorder,
                shape = RoundedCornerShape(16.dp)
            )
            .clickable(onClick = onClick)
            .padding(20.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isSelected) FlameOrange else TextSecondary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = text,
                    color = if (isSelected) TextPrimary else TextSecondary,
                    fontSize = 16.sp
                )
            }
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onClick() },
                colors = CheckboxDefaults.colors(
                    checkedColor = FlameOrange,
                    uncheckedColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun EquipmentStepContent(
    hasVitruvian: Boolean,
    hasDumbbells: Boolean,
    hasTRX: Boolean,
    hasPullUpBar: Boolean,
    onVitruvianChange: (Boolean) -> Unit,
    onDumbbellsChange: (Boolean) -> Unit,
    onTRXChange: (Boolean) -> Unit,
    onPullUpBarChange: (Boolean) -> Unit,
    onNext: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "YOUR EQUIPMENT",
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "What equipment do you have?",
            fontSize = 24.sp,
            fontWeight = FontWeight.Light,
            color = TextPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        EquipmentRow(text = "Vitruvian Trainer+", isSelected = hasVitruvian, onChange = onVitruvianChange)
        Spacer(modifier = Modifier.height(12.dp))
        EquipmentRow(text = "Dumbbells", isSelected = hasDumbbells, onChange = onDumbbellsChange)
        Spacer(modifier = Modifier.height(12.dp))
        EquipmentRow(text = "TRX", isSelected = hasTRX, onChange = onTRXChange)
        Spacer(modifier = Modifier.height(12.dp))
        EquipmentRow(text = "Pull-up Bar", isSelected = hasPullUpBar, onChange = onPullUpBarChange)

        Spacer(modifier = Modifier.weight(1f))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onBack,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("BACK")
            }
            Button(
                onClick = onNext,
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                modifier = Modifier.weight(1f).height(56.dp)
            ) {
                Text("CONTINUE", fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun EquipmentRow(
    text: String,
    isSelected: Boolean,
    onChange: (Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) FlameOrange.copy(alpha = 0.2f) else Slate800)
            .border(
                width = 1.dp,
                color = if (isSelected) FlameOrange else GlassBorder,
                shape = RoundedCornerShape(12.dp)
            )
            .clickable { onChange(!isSelected) }
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = text, color = TextPrimary, fontSize = 16.sp)
            Checkbox(
                checked = isSelected,
                onCheckedChange = { onChange(it) },
                colors = CheckboxDefaults.colors(
                    checkedColor = FlameOrange,
                    uncheckedColor = TextSecondary
                )
            )
        }
    }
}

@Composable
private fun ConnectStepContent(
    appName: String,
    goals: Set<String>,
    hasVitruvian: Boolean,
    hasDumbbells: Boolean,
    hasTRX: Boolean,
    hasPullUpBar: Boolean,
    onComplete: (appName: String, goals: Set<String>, hasVitruvian: Boolean, hasDumbbells: Boolean, hasTRX: Boolean, hasPullUpBar: Boolean) -> Unit,
    onBack: () -> Unit
) {
    var isConnecting by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "CONNECT",
            fontSize = 12.sp,
            letterSpacing = 4.sp,
            color = TextSecondary
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (hasVitruvian) {
            Text(
                text = "Connect to your\nVitruvian Trainer+",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(RoundedCornerShape(60.dp))
                    .background(
                        if (isConnected) FlameOrange.copy(alpha = 0.2f) else Slate800
                    )
                    .border(
                        width = 2.dp,
                        color = if (isConnected) FlameOrange else GlassBorder,
                        shape = RoundedCornerShape(60.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isConnected) Icons.Filled.Check else Icons.Filled.Bluetooth,
                    contentDescription = null,
                    tint = if (isConnected) FlameOrange else TextSecondary,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = if (isConnecting) "Searching..." 
                       else if (isConnected) "Connected!" 
                       else "Tap below to connect",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    isConnecting = true
                    // TODO: Actual BLE connection - simulate for now
                    // For now, just mark as connected
                    isConnected = true
                    isConnecting = false
                },
                enabled = !isConnecting && !isConnected,
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text(
                    text = if (isConnecting) "CONNECTING..." else "CONNECT",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Call onComplete when connected
            if (isConnected) {
                LaunchedEffect(isConnected) {
                    onComplete(appName, goals, hasVitruvian, hasDumbbells, hasTRX, hasPullUpBar)
                }
            }
        } else {
            Text(
                text = "No Vitruvian?\nNo problem.",
                fontSize = 24.sp,
                fontWeight = FontWeight.Light,
                color = TextPrimary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Use simulator mode to try the app",
                color = TextSecondary,
                fontSize = 14.sp
            )

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = { 
                    onComplete(appName, goals, hasVitruvian, hasDumbbells, hasTRX, hasPullUpBar)
                },
                colors = ButtonDefaults.buttonColors(containerColor = FlameOrange),
                modifier = Modifier.fillMaxWidth().height(56.dp)
            ) {
                Text("GET STARTED", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onBack,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            modifier = Modifier.fillMaxWidth().height(56.dp)
        ) {
            Text("BACK")
        }
    }
}
