package com.example.fitlinktrainer.ui.components

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.navigation.NavController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.example.fitlinktrainer.navigation.BottomNavItem
import com.example.fitlinktrainer.ui.theme.*

@Composable
fun BottomNavigationBar(
    items: List<BottomNavItem>,
    navController: NavController
) {
    val navBackStackEntry = navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry.value?.destination?.route

    NavigationBar(
        modifier = Modifier.fillMaxWidth(),
        containerColor = Surface
    ) {
        items.forEach { item ->
            val selected = currentRoute == item.route

            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (currentRoute != item.route) {
                        // Check if route exists in graph
                        if (navController.graph.findNode(item.route) != null) {
                            navController.navigate(item.route) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        } else {
                            Log.e("BottomNavigation", "Route ${item.route} not found")
                        }
                    }
                },
                icon = {
                    Icon(
                        painter = painterResource(item.icon),
                        contentDescription = item.label,
                        tint = if (selected) PrimaryBlue else TextSecondary
                    )
                },
                label = {
                    Text(
                        text = item.label,
                        color = if (selected) PrimaryBlue else TextSecondary
                    )
                }
            )
        }
    }
}