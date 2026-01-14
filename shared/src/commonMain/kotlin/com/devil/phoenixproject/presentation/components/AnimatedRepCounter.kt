package com.devil.phoenixproject.presentation.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devil.phoenixproject.domain.model.RepPhase

/**
 * Issue #163: Animated Rep Counter
 *
 * Displays the current rep being performed with animated visual feedback:
 * - CONCENTRIC (lifting): Number outline reveals from bottom to top
 * - ECCENTRIC (lowering): Number fills with color from top to bottom
 * - IDLE: Shows confirmed count with solid color
 *
 * The animation provides visual feedback during each phase of the rep
 * without the confusing number flicker of the previous implementation.
 *
 * @param nextRepNumber The rep number being performed (workingReps + 1)
 * @param phase Current movement phase (IDLE, CONCENTRIC, ECCENTRIC)
 * @param phaseProgress Progress through current phase (0.0 to 1.0)
 * @param confirmedReps The confirmed completed rep count (for idle display)
 * @param targetReps Target rep count (for stable X/Y display)
 * @param showStableCounter Whether to show the stable "X / Y" counter below
 * @param size Size of the component
 */
@Composable
fun AnimatedRepCounter(
    nextRepNumber: Int,
    phase: RepPhase,
    phaseProgress: Float,
    confirmedReps: Int,
    targetReps: Int,
    showStableCounter: Boolean = true,
    size: Dp = 120.dp,
    modifier: Modifier = Modifier
) {
    // Animate the progress for smoother transitions
    val animatedProgress by animateFloatAsState(
        targetValue = phaseProgress,
        animationSpec = tween(durationMillis = 50),
        label = "phase_progress"
    )

    // Colors
    val outlineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
    val fillColor = MaterialTheme.colorScheme.primary
    val glowColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(size)
    ) {
        when (phase) {
            RepPhase.IDLE -> {
                // Show confirmed count with solid primary color
                Text(
                    text = confirmedReps.toString(),
                    style = MaterialTheme.typography.displayLarge.copy(
                        fontSize = (size.value * 0.7f).sp,
                        fontWeight = FontWeight.Black
                    ),
                    color = fillColor,
                    textAlign = TextAlign.Center
                )
            }

            RepPhase.CONCENTRIC -> {
                // Outline reveals from bottom to top
                // Progress 0.0 = nothing visible, 1.0 = full outline visible
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Base outline (revealed bottom-to-top based on progress)
                    Text(
                        text = nextRepNumber.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = (size.value * 0.7f).sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = outlineColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                // Mask: hide from top down based on inverse progress
                                // At progress 0: hide everything (clip at bottom)
                                // At progress 1: show everything (no clip)
                                val clipHeight = this.size.height * (1f - animatedProgress)
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(0f, 0f),
                                    size = this.size.copy(height = clipHeight),
                                    blendMode = BlendMode.Clear
                                )
                            }
                    )
                }
            }

            RepPhase.ECCENTRIC -> {
                // Fill reveals from top to bottom over the outline
                // Progress 0.0 = outline only, 1.0 = fully filled
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    // Full outline as background
                    Text(
                        text = nextRepNumber.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = (size.value * 0.7f).sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = outlineColor,
                        textAlign = TextAlign.Center
                    )

                    // Filled text with glow (revealed top-to-bottom based on progress)
                    Text(
                        text = nextRepNumber.toString(),
                        style = MaterialTheme.typography.displayLarge.copy(
                            fontSize = (size.value * 0.7f).sp,
                            fontWeight = FontWeight.Black
                        ),
                        color = fillColor,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                            .drawWithContent {
                                drawContent()
                                // Mask: hide from bottom up based on inverse progress
                                // At progress 0: hide everything (clip entire area)
                                // At progress 1: show everything (no clip)
                                val visibleHeight = this.size.height * animatedProgress
                                val clipTop = visibleHeight
                                drawRect(
                                    color = Color.Transparent,
                                    topLeft = Offset(0f, clipTop),
                                    size = this.size.copy(height = this.size.height - clipTop),
                                    blendMode = BlendMode.Clear
                                )
                            }
                    )
                }
            }
        }
    }
}

/**
 * Stable rep progress display showing confirmed reps vs target.
 * Example: "3 / 10"
 */
@Composable
fun StableRepProgress(
    confirmedReps: Int,
    targetReps: Int,
    modifier: Modifier = Modifier
) {
    Text(
        text = "$confirmedReps / $targetReps",
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Medium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = modifier
    )
}
