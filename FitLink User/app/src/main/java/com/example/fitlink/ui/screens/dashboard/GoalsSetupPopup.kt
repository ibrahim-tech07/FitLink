package com.example.fitlink.ui.screens.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlink.ui.viewmodels.DashboardViewModel


@Composable
fun GoalsSetupScreen(
    onNext: () -> Unit,
    viewModel: DashboardViewModel,
    onScreenOpened: (Boolean) -> Unit
) {

    var calories by remember { mutableStateOf("2000") }
    var workouts by remember { mutableStateOf("5") }
    var selectedGoal by remember { mutableStateOf("general") }

    // hide bottom bar
    LaunchedEffect(Unit) {
        onScreenOpened(true)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundSoft)
            .padding(24.dp)
    ) {

        Spacer(Modifier.height(30.dp))

        LinearProgressIndicator(
            progress = { 0.5f },
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = PurpleGradientStart
        )

        Spacer(Modifier.height(32.dp))

        Text(
            "Set Your Goals",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(Modifier.height(8.dp))

        Text(
            "These goals will personalize your fitness journey.",
            fontSize = 14.sp,
            color = TextMedium
        )

        Spacer(Modifier.height(32.dp))

        // Calories Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(Modifier.padding(20.dp)) {

                Text(
                    "Daily Calories Goal",
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = calories,
                    onValueChange = { calories = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }

        Spacer(Modifier.height(16.dp))

        // Workout Goal Card
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(Modifier.padding(20.dp)) {

                Text(
                    "Weekly Workouts Goal",
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Spacer(Modifier.height(10.dp))

                OutlinedTextField(
                    value = workouts,
                    onValueChange = { workouts = it.filter { c -> c.isDigit() } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
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
            }
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Quick presets",
            fontWeight = FontWeight.Medium,
            color = TextMedium
        )

        Spacer(Modifier.height(10.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            item {
                GoalChip(
                    label = "Weight Loss",
                    selected = selectedGoal == "loss"
                ) {
                    calories = "1800"
                    workouts = "4"
                    selectedGoal = "loss"
                }
            }

            item {
                GoalChip(
                    label = "General Fitness",
                    selected = selectedGoal == "general"
                ) {
                    calories = "2200"
                    workouts = "5"
                    selectedGoal = "general"
                }
            }

            item {
                GoalChip(
                    label = "Muscle Gain",
                    selected = selectedGoal == "muscle"
                ) {
                    calories = "2600"
                    workouts = "6"
                    selectedGoal = "muscle"
                }
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {

                val caloriesGoal = calories.toIntOrNull() ?: 2000
                val workoutGoal = workouts.toIntOrNull() ?: 5

                viewModel.updateUserGoals(
                    caloriesGoal,
                    workoutGoal,
                    selectedGoal
                )

                onNext()
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
                "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}
@Composable
fun GoalChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = if (selected) PurpleGradientStart else LightPurpleBg,
        modifier = Modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null
        ) { onClick() }
    ) {

        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            fontWeight = FontWeight.Medium,
            color = if (selected) Color.White else TextDark
        )
    }
}