package com.devil.phoenixproject.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.devil.phoenixproject.ui.theme.FlameOrange
import com.devil.phoenixproject.ui.theme.PhoenixAmber

/**
 * PremiumBadge — shown next to features that require a Pro subscription.
 *
 * Usage:
 *   Row { Text("AI Workouts"); Spacer(Modifier.width(6.dp)); PremiumBadge() }
 */
@Composable
fun PremiumBadge(modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(FlameOrange, PhoenixAmber)
    )

    Box(
        modifier = modifier
            .background(brush = gradient, shape = RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(8.dp)
            )
            Spacer(modifier = Modifier.width(2.dp))
            Text(
                text = "PRO",
                style = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 9.sp,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}

/**
 * LargePremiumBadge — used on paywalls and feature highlight screens.
 */
@Composable
fun LargePremiumBadge(modifier: Modifier = Modifier) {
    val gradient = Brush.horizontalGradient(
        colors = listOf(FlameOrange, PhoenixAmber)
    )

    Box(
        modifier = modifier
            .background(brush = gradient, shape = RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "PHOENIX PRO",
                style = TextStyle(
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.5.sp
                )
            )
        }
    }
}
