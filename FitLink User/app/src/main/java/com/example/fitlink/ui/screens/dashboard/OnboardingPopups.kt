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
fun OnboardingBodyScreen(
    onNext: () -> Unit,
    viewModel: DashboardViewModel,
    onScreenOpened: (Boolean) -> Unit
) {

    var age by remember { mutableStateOf("") }
    var height by remember { mutableStateOf("") }
    var weight by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf("Male") }
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

        // Progress indicator (step 1)
        LinearProgressIndicator(
            progress = 0.25f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = PurpleGradientStart
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = "Tell us about you",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(Modifier.height(6.dp))

        Text(
            text = "This helps us personalize your workouts and calorie tracking.",
            fontSize = 14.sp,
            color = TextMedium
        )

        Spacer(Modifier.height(32.dp))

        // Gender selection
        Text(
            text = "Gender",
            fontWeight = FontWeight.SemiBold,
            color = TextDark
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            GenderChip(
                label = "Male",
                selected = gender == "Male",
                onClick = { gender = "Male" }
            )

            GenderChip(
                label = "Female",
                selected = gender == "Female",
                onClick = { gender = "Female" }
            )

            GenderChip(
                label = "Other",
                selected = gender == "Other",
                onClick = { gender = "Other" }
            )
        }

        Spacer(Modifier.height(24.dp))

        // Age Card
        InputCard(
            label = "Age",
            value = age,
            onChange = { age = it }
        )

        Spacer(Modifier.height(16.dp))

        // Height Card
        InputCard(
            label = "Height (cm)",
            value = height,
            onChange = { height = it }
        )

        Spacer(Modifier.height(16.dp))

        // Weight Card
        InputCard(
            label = "Weight (kg)",
            value = weight,
            onChange = { weight = it }
        )

        Spacer(Modifier.weight(1f))

        // Continue button
        Button(
            onClick = {

                viewModel.updateUserBodyDetails(
                    age.toIntOrNull() ?: 0,
                    gender,
                    height.toDoubleOrNull() ?: 0.0,
                    weight.toDoubleOrNull() ?: 0.0
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
                text = "Continue",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}
@Composable
fun GenderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = if (selected) PurpleGradientStart else LightPurpleBg,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {

        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
            color = if (selected) Color.White else TextDark,
            fontWeight = FontWeight.Medium
        )
    }
}@Composable
fun InputCard(
    label: String,
    value: String,
    onChange: (String) -> Unit
) {

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        modifier = Modifier.fillMaxWidth()
    ) {

        Column(
            modifier = Modifier.padding(18.dp)
        ) {

            Text(
                text = label,
                fontWeight = FontWeight.SemiBold,
                color = TextDark
            )

            Spacer(Modifier.height(8.dp))

            OutlinedTextField(
                value = value,
                onValueChange = onChange,
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
}