package com.example.fitlinktrainer.ui.screens.dashboard

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.example.fitlinktrainer.data.model.Trainer
import com.example.fitlinktrainer.viewmodel.TrainerProfileViewModel

@Composable
fun TrainerProfileScreen(
    trainerId: String,
    viewModel: TrainerProfileViewModel = hiltViewModel()
) {

    val trainer by viewModel.trainer.collectAsState()
    val uploading by viewModel.uploading.collectAsState()

    val context = LocalContext.current

    LaunchedEffect(trainerId) {
        viewModel.loadTrainer(trainerId)
    }

    val imagePicker =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent()
        ) { uri: Uri? ->

            uri?.let {
                viewModel.uploadProfileImage(context, it)
            }
        }

    trainer?.let { profile ->

        ProfileContent(
            trainer = profile,
            uploading = uploading,
            onImageClick = {
                imagePicker.launch("image/*")
            },
            onSave = {
                viewModel.updateTrainer(it)
            }
        )
    }
}
@Composable
fun ProfileContent(
    trainer: Trainer,
    uploading: Boolean,
    onImageClick: () -> Unit,
    onSave: (Trainer) -> Unit
) {

    var name by remember { mutableStateOf(trainer.name) }
    var phone by remember { mutableStateOf(trainer.phone) }
    var bio by remember { mutableStateOf(trainer.bio) }
    var rate by remember { mutableStateOf(trainer.hourlyRate.toString()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp)
    ) {

        Text(
            text = "Trainer Profile",
            style = MaterialTheme.typography.headlineSmall
        )

        Spacer(Modifier.height(20.dp))

        Box(
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {

            AsyncImage(
                model = trainer.profileImageUrl,
                contentDescription = null,
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
            )

            if (uploading)
                CircularProgressIndicator()
        }

        Spacer(Modifier.height(10.dp))

        Button(
            onClick = onImageClick,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Text("Change Photo")
        }

        Spacer(Modifier.height(20.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Trainer Name") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = bio,
            onValueChange = { bio = it },
            label = { Text("Bio") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(10.dp))

        OutlinedTextField(
            value = rate,
            onValueChange = { rate = it },
            label = { Text("Hourly Rate") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(20.dp))

        Button(
            onClick = {

                val updated = trainer.copy(
                    name = name,
                    phone = phone,
                    bio = bio,
                    hourlyRate = rate.toDoubleOrNull() ?: 0.0
                )

                onSave(updated)

            },
            modifier = Modifier.fillMaxWidth()
        ) {

            Text("Save Changes")

        }
    }
}