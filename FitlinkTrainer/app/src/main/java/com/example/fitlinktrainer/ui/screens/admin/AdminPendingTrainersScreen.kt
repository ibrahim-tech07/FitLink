package com.example.fitlinktrainer.ui.screens.admin

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlinktrainer.viewmodel.AdminDashboardViewModel

@Composable
fun AdminPendingTrainersScreen(
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {


    val pendingTrainers by viewModel.pendingTrainers.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text("Pending Trainer Approvals", style = MaterialTheme.typography.headlineMedium)
        }

        if (isLoading) {
            item { CircularProgressIndicator() }
        } else if (pendingTrainers.isEmpty()) {
            item { Text("No pending trainers") }
        } else {
            items(pendingTrainers) { trainer ->
                Card(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(trainer.name, style = MaterialTheme.typography.titleLarge)
                        Text(trainer.email)
                        Text("Specialties: ${trainer.specialties.joinToString()}")
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = { viewModel.approveTrainer(trainer.id) }
                            ) {
                                Text("Approve")
                            }
                            Button(
                                onClick = { viewModel.rejectTrainer(trainer.id) },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Reject")
                            }
                        }
                    }
                }
            }
        }
    }
}