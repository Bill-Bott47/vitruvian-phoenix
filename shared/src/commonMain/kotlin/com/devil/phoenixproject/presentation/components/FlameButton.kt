package com.devil.phoenixproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devil.phoenixproject.ui.theme.FlameOrange
import com.devil.phoenixproject.ui.theme.FlameRed
import com.devil.phoenixproject.ui.theme.FlameYellow
import com.devil.phoenixproject.ui.theme.PhoenixGlow

/**
 * FlameButton — the signature full-width CTA for Phoenix.
 *
 * Uses the Phoenix flame gradient (red-orange-yellow).
 * Used for: Start Workout, Just Lift, Begin Session, etc.
 *
 * Usage:
 *   FlameButton(text = "START WORKOUT", onClick = { ... })
 */
@Composable
fun FlameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 56.dp,
    enabled: Boolean = true,
    cornerRadius: Dp = 16.dp
) {
    val gradient = if (enabled) {
        Brush.horizontalGradient(
            colors = listOf(FlameRed, FlameOrange, FlameYellow)
        )
    } else {
        Brush.horizontalGradient(
            colors = listOf(Color(0xFF666666), Color(0xFF888888))
        )
    }

    val shadowColor = if (enabled) PhoenixGlow else Color.Transparent
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .shadow(
                elevation = if (enabled) 12.dp else 0.dp,
                shape = RoundedCornerShape(cornerRadius),
                ambientColor = shadowColor,
                spotColor = shadowColor
            )
            .clip(RoundedCornerShape(cornerRadius))
            .background(brush = gradient)
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = Color.White.copy(alpha = 0.3f)),
                enabled = enabled,
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                letterSpacing = 1.5.sp
            )
        )
    }
}

/**
 * SecondaryFlameButton — outlined variant for secondary CTAs.
 * Same shape language, but uses a border instead of fill.
 */
@Composable
fun SecondaryFlameButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 48.dp,
    cornerRadius: Dp = 16.dp
) {
    val borderGradient = Brush.horizontalGradient(
        colors = listOf(FlameRed, FlameOrange, FlameYellow)
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(height)
            .background(
                brush = borderGradient,
                shape = RoundedCornerShape(cornerRadius)
            )
            .padding(1.5.dp) // border thickness
            .clip(RoundedCornerShape(cornerRadius - 1.dp))
            .background(Color(0xFF0F172A)) // Slate900
            .clickable(
                interactionSource = interactionSource,
                indication = rememberRipple(color = FlameOrange.copy(alpha = 0.2f)),
                onClick = onClick
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = TextStyle(
                color = FlameOrange,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                letterSpacing = 1.sp
            )
        )
    }
}
