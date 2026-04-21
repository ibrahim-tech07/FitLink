package com.example.fitlinktrainer.ui.screens.components



import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.fitlinktrainer.ui.theme.Background
import com.example.fitlinktrainer.ui.theme.PrimaryBlue

@Composable
fun LoadingScreen() {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Background),
        contentAlignment = Alignment.Center
    ) {

        Column(horizontalAlignment = Alignment.CenterHorizontally) {

            CircularProgressIndicator(
                color = PrimaryBlue,
                strokeWidth = 3.dp
            )

            Spacer(modifier = Modifier.height(16.dp))

        }

    }
}