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
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlink.ui.viewmodels.DashboardViewModel
@Composable
fun ReminderSetupScreen(
    onNext: () -> Unit,
    viewModel: DashboardViewModel,
    onScreenOpened: (Boolean) -> Unit
) {

    var hour by remember { mutableStateOf("18") }
    var minute by remember { mutableStateOf("00") }
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

        // Progress indicator (step 3)
        LinearProgressIndicator(
            progress = 0.75f,
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(20.dp)),
            color = PurpleGradientStart
        )

        Spacer(Modifier.height(36.dp))

        Text(
            text = "Workout Reminder",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Choose a time to remind you to workout every day.",
            fontSize = 14.sp,
            color = TextMedium
        )

        Spacer(Modifier.height(32.dp))

        // Time selection card
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = CardWhite),
            modifier = Modifier.fillMaxWidth()
        ) {

            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

                Text(
                    "Reminder Time",
                    fontWeight = FontWeight.SemiBold,
                    color = TextDark
                )

                Spacer(Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {


                    OutlinedTextField(
                        value = hour,
                        onValueChange = { hour = it.take(2) },
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
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
                    Text(
                        ":",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp),
                        color = TextDark
                    )
                    OutlinedTextField(
                        value = minute,
                        onValueChange = { minute = it.take(2) },
                        singleLine = true,
                        modifier = Modifier.width(90.dp),
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

        Spacer(Modifier.height(24.dp))

        Text(
            "Quick times",
            fontWeight = FontWeight.Medium,
            color = TextMedium
        )

        Spacer(Modifier.height(10.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {

            ReminderChip("Morning", "07", "00") {
                hour = "07"
                minute = "00"
            }

            ReminderChip("Evening", "18", "00") {
                hour = "18"
                minute = "00"
            }

            ReminderChip("Night", "21", "00") {
                hour = "21"
                minute = "00"
            }
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = {

                val h = hour.toIntOrNull()?.coerceIn(0, 23) ?: 18
                val m = minute.toIntOrNull()?.coerceIn(0, 59) ?: 0

                viewModel.updateReminderSettings(
                    h,
                    m,
                    true
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
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.height(20.dp))
    }
}@Composable
fun ReminderChip(
    label: String,
    hourValue: String,
    minuteValue: String,
    onClick: () -> Unit
) {

    Surface(
        shape = RoundedCornerShape(30.dp),
        color = LightPurpleBg,
        modifier = Modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) { onClick() }
    ) {

        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
            fontWeight = FontWeight.Medium,
            color = TextDark
        )
    }
}