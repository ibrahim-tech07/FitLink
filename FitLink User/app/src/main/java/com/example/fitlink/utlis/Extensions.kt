package com.example.fitlink.utlis


import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Modifier.topBorder(strokeWidth: Dp, color: Color) = drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    drawLine(
        color = color,
        start = Offset(0f, 0f),
        end = Offset(size.width, 0f),
        strokeWidth = strokeWidthPx
    )
}

fun Modifier.bottomBorder(strokeWidth: Dp, color: Color) = drawBehind {
    val strokeWidthPx = strokeWidth.toPx()
    drawLine(
        color = color,
        start = Offset(0f, size.height),
        end = Offset(size.width, size.height),
        strokeWidth = strokeWidthPx
    )
}

fun Modifier.verticalGradientBackground(
    colors: List<Color>
) = drawBehind {
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.verticalGradient(colors)
    )
}

fun Modifier.horizontalGradientBackground(
    colors: List<Color>
) = drawBehind {
    drawRect(
        brush = androidx.compose.ui.graphics.Brush.horizontalGradient(colors)
    )
}