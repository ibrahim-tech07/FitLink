package com.example.fitlink.ui.components



import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.fitlink.R
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.Column


@Composable
fun ProfileImagePicker(
    imageUrl: String?,
    onImageSelected: (Uri) -> Unit,
    onImageRemoved: () -> Unit,
    modifier: Modifier = Modifier,
    size: Int = 100,
    isUploading: Boolean = false,
    uploadProgress: Double = 0.0
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            onImageSelected(it)
        }
    }

    Box(
        modifier = modifier
            .size(size.dp)
            .clickable { showDialog = true }
    ) {
        // Profile Image
        if (imageUrl != null && !isUploading) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(imageUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Profile Image",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        } else {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxSize()
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (isUploading) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                progress = { uploadProgress.toFloat() / 100 },
                                modifier = Modifier.size(30.dp)
                            )
                            Text(
                                text = "${uploadProgress.toInt()}%",
                                fontSize = 10.sp
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_camera),
                            contentDescription = "Add Photo",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }

        // Edit icon overlay
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size((size / 3).dp)

        ) {
            Icon(
                painter = painterResource(R.drawable.ic_edit),
                contentDescription = "Edit",
                tint = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier
                    .padding(4.dp)
                    .size((size / 3).dp)

            )
        }
    }

    // Options Dialog
    if (showDialog) {
        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Profile Photo") },
            text = {
                Column {
                    TextButton(
                        onClick = {
                            imagePickerLauncher.launch("image/*")
                            showDialog = false
                        }
                    ) {
                        Text("Choose from Gallery")
                    }
                    if (imageUrl != null) {
                        TextButton(
                            onClick = {
                                onImageRemoved()
                                showDialog = false
                            }
                        ) {
                            Text("Remove Photo", color = MaterialTheme.colorScheme.error)
                        }
                    }
                    TextButton(
                        onClick = { showDialog = false }
                    ) {
                        Text("Cancel")
                    }
                }
            },
            confirmButton = {},
            dismissButton = {}
        )
    }
}