package com.example.fitlink.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlink.R
import com.example.fitlink.ui.screens.dashboard.CardWhite
import com.example.fitlink.ui.screens.dashboard.PurpleGradientStart
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.runtime.getValue


// ───────────────────── REDESIGNED BOTTOM NAVIGATION (with labels) ─────────────────────
@Composable
fun BottomNavigationBar(
    currentPage: String,
    onTabClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .height(80.dp),
        tonalElevation = 8.dp,
        color = CardWhite
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {

            val items = listOf(
                "dashboard" to Pair(R.drawable.ic_home, "Dashboard"),
                "workouts" to Pair(R.drawable.ic_dumbbell, "Workouts"),
                "trainers" to Pair(R.drawable.ic_users, "Trainers"),
                "calendar" to Pair(R.drawable.ic_calendar, "Calendar"),
                "chat" to Pair(R.drawable.ic_message, "Chat")
            )

            items.forEach { (page, pair) ->

                val (iconRes, label) = pair
                val isActive = currentPage == page

                val iconSize by animateDpAsState(
                    targetValue = if (isActive) 28.dp else 22.dp,
                    label = ""
                )

                val iconColor by animateColorAsState(
                    targetValue = if (isActive) PurpleGradientStart else Color.Gray,
                    label = ""
                )

                val textColor by animateColorAsState(
                    targetValue = if (isActive) PurpleGradientStart else Color.Gray,
                    label = ""
                )

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier
                        .clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null
                        ) {
                            onTabClick(page)
                        }
                        .padding(horizontal = 4.dp)
                ) {

                    Icon(
                        painter = painterResource(iconRes),
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(iconSize)
                    )

                    Text(
                        text = label,
                        fontSize = 11.sp,
                        fontWeight = if (isActive) FontWeight.Medium else FontWeight.Normal,
                        color = textColor,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }
}