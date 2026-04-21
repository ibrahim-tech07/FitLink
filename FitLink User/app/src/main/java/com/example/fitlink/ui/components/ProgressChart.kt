// File: com/example/fitlink/ui/components/ProgressChart.kt
package com.example.fitlink.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlink.ui.screens.dashboard.*

val MainGreen = Color(0xFF10B981)
val DarkGreen = Color(0xFF059669)
val SoftGray = Color(0xFFF3F4F6)
val MediumGray = Color(0xFF9CA3AF)
val DarkGray = Color(0xFF4B5563)
@Composable
fun WeeklyProgressChart(
    data: List<Int>,
    labels: List<String> = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"),
    modifier: Modifier = Modifier
) {
    val maxValue = data.maxOrNull()?.coerceAtLeast(1) ?: 1
    val animatedValues = remember(data) {
        data.map { Animatable(0f) }.toMutableStateList()
    }

    LaunchedEffect(data) {
        data.forEachIndexed { index, value ->
            animatedValues[index].animateTo(
                value.toFloat() / maxValue,
                animationSpec = tween(1000, delayMillis = index * 100)
            )
        }
    }

    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        data.indices.forEach { index ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .padding(horizontal = 4.dp),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val barWidth = size.width * 0.6f
                        val barHeight = size.height * animatedValues[index].value

                        drawRoundRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(MainGreen, DarkGreen)
                            ),
                            topLeft = Offset(
                                (size.width - barWidth) / 2,
                                size.height - barHeight
                            ),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(8f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = data[index].toString(),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MainGreen
                )

                Text(
                    text = labels[index],
                    fontSize = 10.sp,
                    color = DarkGray
                )
            }
        }
    }
}

@Composable
fun CircularProgressCard(
    progress: Float,
    value: String,
    label: String,
    sublabel: String,
    color: Color = MainGreen,
    modifier: Modifier = Modifier
) {
    val animatedProgress = remember { Animatable(0f) }

    LaunchedEffect(progress) {
        animatedProgress.animateTo(
            progress,
            animationSpec = tween(1500, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Circular Progress
            Box(
                modifier = Modifier.size(80.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // Background circle
                    drawCircle(
                        color = SoftGray,
                        style = Stroke(width = 8f)
                    )

                    // Progress arc
                    val sweepAngle = 360 * animatedProgress.value
                    drawArc(
                        brush = Brush.sweepGradient(
                            colors = listOf(color, color.copy(alpha = 0.5f))
                        ),
                        startAngle = -90f,
                        sweepAngle = sweepAngle,
                        useCenter = false,
                        style = Stroke(width = 8f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                    )
                }

                Text(
                    text = "${(progress * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Content
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = value,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = color
                )
                Text(
                    text = label,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkGray
                )
                Text(
                    text = sublabel,
                    fontSize = 12.sp,
                    color = MediumGray
                )
            }
        }
    }
}