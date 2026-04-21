package com.example.fitlinktrainer.ui.screens.components

import androidx.compose.animation.core.*
import androidx.compose.animation.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.ui.theme.*
import kotlin.math.abs
import kotlin.math.roundToInt

data class ChartSeries(
    val label: String,
    val data: List<Float>,
    val color: Color,
    var isVisible: Boolean = true
)

@Composable
fun MiniLineChart(
    series: List<ChartSeries>,
    modifier: Modifier = Modifier,
    animationDuration: Int = 800,
    showPoints: Boolean = false,
    showArea: Boolean = true,
    showLegend: Boolean = true,
    gradient: Brush? = null
) {
    val animatedProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(durationMillis = animationDuration, easing = FastOutSlowInEasing)
    )

    // State for tooltip
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    val density = LocalDensity.current

    val maxY = remember(series) {
        series.filter { it.isVisible }.flatMap { it.data }.maxOrNull() ?: 1f
    }.coerceAtLeast(1f)

    // Determine the number of steps (use the first visible series data size, assume all series have same length)
    val steps = series.firstOrNull()?.data?.size ?: 0

    Column {
        if (showLegend && series.size > 1) {
            LegendRow(series)
        }

        Box(modifier = modifier) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .onGloballyPositioned { coordinates ->
                        canvasSize = coordinates.size
                    }
                    .pointerInput(series) {
                        detectTapGestures { offset ->
                            if (steps > 1 && canvasSize.width > 0) {
                                // Calculate which index was tapped
                                val xStep = canvasSize.width.toFloat() / (steps - 1)
                                val index = (offset.x / xStep).roundToInt()
                                    .coerceIn(0, steps - 1)
                                selectedIndex = if (selectedIndex == index) null else index
                            }
                        }
                    }
            ) {
                val width = size.width
                val height = size.height
                if (steps < 2) return@Canvas

                val xStep = width / (steps - 1)
                val scaleY = height / maxY

                // Draw visible series
                series.filter { it.isVisible }.forEach { s ->
                    val points = s.data.mapIndexed { index, value ->
                        Offset(
                            x = index * xStep,
                            y = height - (value * scaleY * animatedProgress).coerceIn(0f, height)
                        )
                    }

                    // Draw area
                    if (showArea && gradient != null) {
                        val path = Path().apply {
                            val first = points.first()
                            moveTo(first.x, height)
                            points.forEach { lineTo(it.x, it.y) }
                            lineTo(points.last().x, height)
                            close()
                        }
                        drawPath(
                            path = path,
                            brush = gradient,
                            alpha = 0.3f * animatedProgress,
                            style = Fill
                        )
                    }

                    // Draw line
                    for (i in 0 until points.size - 1) {
                        drawLine(
                            color = s.color,
                            start = points[i],
                            end = points[i + 1],
                            strokeWidth = 3f,
                            cap = StrokeCap.Round
                        )
                    }

                    // Draw points if needed
                    if (showPoints) {
                        points.forEach { point ->
                            drawCircle(
                                color = s.color,
                                radius = 4f,
                                center = point,
                                style = Fill
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 2f,
                                center = point,
                                style = Fill
                            )
                        }
                    }
                }

                // Draw vertical line and highlight points for selected index
                selectedIndex?.let { idx ->
                    val x = idx * xStep
                    // Vertical line
                    drawLine(
                        color = TextSecondary.copy(alpha = 0.5f),
                        start = Offset(x, 0f),
                        end = Offset(x, height),
                        strokeWidth = 2f,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                    // Highlight points on each visible series
                    series.filter { it.isVisible }.forEach { s ->
                        if (idx < s.data.size) {
                            val y = height - (s.data[idx] * scaleY * animatedProgress).coerceIn(0f, height)
                            drawCircle(
                                color = s.color,
                                radius = 8f,
                                center = Offset(x, y),
                                style = Fill
                            )
                            drawCircle(
                                color = Color.White,
                                radius = 4f,
                                center = Offset(x, y),
                                style = Fill
                            )
                        }
                    }
                }
            }

            // Tooltip popup
            androidx.compose.animation.AnimatedVisibility(
                visible = selectedIndex != null && canvasSize.width > 0 && steps > 1,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut()
            ) {
                selectedIndex?.let { idx ->
                    val xStep = canvasSize.width.toFloat() / (steps - 1)
                    val tooltipX = idx * xStep
                    // Determine if tooltip would go off screen horizontally
                    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
                    val tooltipWidth = 180.dp // approximate
                    val tooltipOffsetX = with(density) {
                        val halfTooltip = tooltipWidth.toPx() / 2
                        when {
                            tooltipX - halfTooltip < 0 -> -tooltipX + 8.dp.toPx()
                            tooltipX + halfTooltip > canvasSize.width -> canvasSize.width - tooltipX - halfTooltip - 8.dp.toPx()
                            else -> -halfTooltip
                        }
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .absoluteOffset(
                                x = (tooltipX + tooltipOffsetX).roundToInt().dp,
                                y = 0.dp
                            )
                    ) {
                        TooltipCard(
                            series = series,
                            index = idx,
                            onDismiss = { selectedIndex = null }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TooltipCard(
    series: List<ChartSeries>,
    index: Int,
    onDismiss: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier
            .wrapContentSize()
            .padding(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            // Header with close button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_close),
                        contentDescription = "Close",
                        tint = TextSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            // Values for each visible series
            series.filter { it.isVisible }.forEach { s ->
                if (index < s.data.size) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(12.dp)
                                .clip(CircleShape)
                                .background(s.color)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = s.label,
                            fontSize = 12.sp,
                            color = TextPrimary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = String.format("%.1f", s.data[index]),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = TextPrimary
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendRow(series: List<ChartSeries>) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp),
        horizontalArrangement = Arrangement.Center
    ) {
        series.forEach { s ->
            LegendItem(
                label = s.label,
                color = s.color,
                isVisible = s.isVisible,
                onToggle = { s.isVisible = !s.isVisible }
            )
            Spacer(modifier = Modifier.width(16.dp))
        }
    }
}

@Composable
private fun LegendItem(
    label: String,
    color: Color,
    isVisible: Boolean,
    onToggle: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onToggle() }
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(color = if (isVisible) color else TextSecondary.copy(alpha = 0.3f), shape = CircleShape)
        )
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            fontSize = 12.sp,
            color = if (isVisible) TextPrimary else TextSecondary
        )
    }
}