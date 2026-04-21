package com.example.fitlinktrainer.ui.screens.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.unit.dp
import com.example.fitlinktrainer.ui.theme.PrimaryBlue
import com.example.fitlinktrainer.ui.theme.SecondaryCyan

@Composable
fun StatCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
    showGradientAccent: Boolean = false
) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium, // RoundedCornerShape(16.dp) by default
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        // Optional gradient accent line on top (like KpiCard)
        if (showGradientAccent) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(MaterialTheme.shapes.medium)
                    .background(
                        brush = Brush.horizontalGradient(
                            listOf(PrimaryBlue, SecondaryCyan)
                        )
                    )
            )
        }
    }
}