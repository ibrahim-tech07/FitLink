package com.example.fitlink.ui.screens.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlink.ui.viewmodels.DashboardViewModel
import com.example.fitlink.R
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box


import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer

@Composable
fun WaterSetupScreen(
    onFinish: () -> Unit,
    viewModel: DashboardViewModel,
    onScreenOpened: (Boolean) -> Unit
) {

    var goal by remember { mutableStateOf("8") }
    LaunchedEffect(Unit) {
        onScreenOpened(true)
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .padding(horizontal = 24.dp)
    ) {

        Spacer(Modifier.height(30.dp))

        // Progress indicator (final step)
        LinearProgressIndicator(
            progress = 1f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = PurpleGradientStart
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = "Stay Hydrated 💧",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Set your daily water goal to keep your body energized and performing at its best.",
            fontSize = 14.sp,
            color = TextMedium
        )

        Spacer(Modifier.height(32.dp))

        // Hydration visual card
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Icon(
                    painter = painterResource(R.drawable.ic_water),
                    contentDescription = null,
                    tint = Color(0xFF3B82F6),
                    modifier = Modifier.size(42.dp)
                )

                Spacer(Modifier.height(10.dp))

                Text(
                    "Daily Water Goal",
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Spacer(Modifier.height(14.dp))


                OutlinedTextField(
                    value = goal,
                    onValueChange = { goal = it.take(2) },
                    singleLine = true,
                    modifier = Modifier.width(120.dp),
                    shape = RoundedCornerShape(14.dp),

                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PurpleGradientStart,
                        unfocusedBorderColor = LightPurpleBg,
                        focusedContainerColor = CardWhite,
                        unfocusedContainerColor = CardWhite,
                        cursorColor = PurpleGradientStart
                    ),

                    textStyle = TextStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = TextDark
                    )
                )
                Spacer(Modifier.height(8.dp))

                Text(
                    "glasses per day",
                    fontSize = 12.sp,
                    color = TextMedium
                )
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Recommended goals",
            fontWeight = FontWeight.Medium,
            color = TextMedium
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            var waterGoal by remember { mutableStateOf(goal.toIntOrNull() ?: 8) }

            PremiumWaterGoalSelector(
                selectedGoal = waterGoal,
                onGoalSelected = {
                    waterGoal = it
                    goal = it.toString()
                }
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {

                val waterGoal = goal.toIntOrNull()?.coerceIn(4, 20) ?: 8

                viewModel.updateWaterGoal(waterGoal)

                onFinish()
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = PurpleGradientStart,
                contentColor = Color.White
            )
        ) {

            Text(
                "Finish Setup",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}
@Composable
fun PremiumWaterGoalSelector(
    selectedGoal: Int,
    onGoalSelected: (Int) -> Unit
) {

    val maxGlasses = 12

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {

        for (i in 1..maxGlasses) {

            val filled = i <= selectedGoal

            val scale by animateFloatAsState(
                targetValue = if (filled) 1.1f else 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy
                ),
                label = ""
            )

            val waterLevel by animateFloatAsState(
                targetValue = if (filled) 1f else 0f,
                animationSpec = tween(600),
                label = ""
            )

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(70.dp)
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                    }
                    .clip(RoundedCornerShape(12.dp))
                    .background(LightPurpleBg)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        onGoalSelected(i)
                    }
            ) {

                Canvas(
                    modifier = Modifier.fillMaxSize()
                ) {

                    val height = size.height * waterLevel

                    val waveAmplitude = 6f

                    val path = Path()

                    path.moveTo(0f, size.height - height)

                    for (x in 0..size.width.toInt()) {

                        val y = size.height - height +
                                waveAmplitude *
                                kotlin.math.sin(
                                    (x / size.width) * 2 * Math.PI
                                ).toFloat()

                        path.lineTo(x.toFloat(), y)
                    }

                    path.lineTo(size.width, size.height)
                    path.lineTo(0f, size.height)
                    path.close()

                    drawPath(
                        path = path,
                        brush = Brush.verticalGradient(
                            listOf(
                                Color(0xFF60A5FA),
                                Color(0xFF2563EB)
                            )
                        )
                    )
                }
            }
        }
    }
}