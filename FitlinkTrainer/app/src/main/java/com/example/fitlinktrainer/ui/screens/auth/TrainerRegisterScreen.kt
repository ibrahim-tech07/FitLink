// File: com/example/fitlinktrainer/ui/screens/auth/TrainerRegisterScreen.kt
package com.example.fitlinktrainer.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.fitlinktrainer.R
import com.example.fitlinktrainer.ui.theme.*
import com.example.fitlinktrainer.viewmodel.TrainerAuthState
import com.example.fitlinktrainer.viewmodel.TrainerAuthViewModel
import kotlinx.coroutines.launch

@Composable
fun TrainerRegisterScreen(
    onBack: () -> Unit,
    onRegistrationSuccess: () -> Unit,
    viewModel: TrainerAuthViewModel = hiltViewModel()
) {
    // --- form state (unchanged logic) ---
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var specialties by remember { mutableStateOf(setOf<String>()) }
    var certifications by remember { mutableStateOf(setOf<String>()) }
    var termsAccepted by remember { mutableStateOf(false) }

    // --- UI state ---
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(false) }
    var serverErrorMessage by remember { mutableStateOf<String?>(null) }

    var attemptedSubmit by remember { mutableStateOf(false) } // show inline errors after submit attempt

    // --- ViewModel state (unchanged) ---
    val isLoading by viewModel.isLoading.collectAsState()
    val authState by viewModel.authState.collectAsState()

    // --- snackbar / coroutine scope ---
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // --- local selectable lists (do NOT change these arrays unless you want different options) ---
    val TrainerSpecialties = listOf(
        "Weight Loss",
        "Muscle Gain",
        "Bodybuilding",
        "Yoga",
        "HIIT Training",
        "Cardio Fitness",
        "Strength Training",
        "Athletic Performance",
        "Post Injury Rehab"
    )

    val TrainerCertifications = listOf(
        "NASM Certified Trainer",
        "ACE Personal Trainer",
        "ISSA Certified",
        "CrossFit L1",
        "Yoga Alliance",
        "Nutrition Coach",
        "Sports Conditioning"
    )

    // --- basic validations ---
    val isNameValid = name.isNotBlank()
    val isEmailValid = remember(email) { Patterns.EMAIL_ADDRESS.matcher(email).matches() }
    val isPasswordValid = password.length >= 6
    val doPasswordsMatch = password == confirmPassword && password.isNotEmpty()

    // --- react to authState from ViewModel ---
    LaunchedEffect(authState) {
        when (authState) {
            is TrainerAuthState.Error -> {
                serverErrorMessage = (authState as TrainerAuthState.Error).message
                showErrorDialog = true
                serverErrorMessage?.let {
                    scope.launch { snackbarHostState.showSnackbar(it) }
                }
            }

            is TrainerAuthState.Unverified -> {
                scope.launch {
                    snackbarHostState.showSnackbar(
                        "Registration successful. Wait for admin approval."
                    )
                }
                onRegistrationSuccess() // navigate to login
            }
            else -> {}
        }
    }

    // --- helper reusable chip to avoid duplication ---
    @Composable
    fun SelectableChip(
        text: String,
        selected: Boolean,
        onToggle: () -> Unit
    ) {
        FilterChip(
            selected = selected,
            onClick = onToggle,
            label = {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (selected) {
                        Icon(
                            painter = painterResource(R.drawable.ic_check),
                            contentDescription = "selected",
                            tint = Color.White,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(end = 4.dp)
                        )
                    }
                    Text(text)
                }
            },
            colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = PrimaryBlue,
                selectedLabelColor = Color.White,
                containerColor = Surface,
                labelColor = TextPrimary
            )
        )
    }

    // --- UI ---
    Scaffold(
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(listOf(PrimaryBlue, SecondaryCyan))
                )
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                item { Spacer(modifier = Modifier.height(28.dp)) }

                item {
                    Image(
                        painter = painterResource(R.drawable.fitlink),
                        contentDescription = "FitLink Logo",
                        modifier = Modifier
                            .size(72.dp)
                            .shadow(10.dp, RoundedCornerShape(18.dp))
                            .clip(RoundedCornerShape(18.dp))
                            .background(Background),
                        contentScale = ContentScale.Fit
                    )
                }

                item { Spacer(modifier = Modifier.height(12.dp)) }

                item {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Trainer Registration",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Create your trainer profile and get clients",
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                item { Spacer(modifier = Modifier.height(18.dp)) }

                item {
                    // Card container; widthIn ensures it stays readable on large screens
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 560.dp),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = Background),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Name
                            OutlinedTextField(
                                value = name,
                                onValueChange = { name = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Full name", color = TextSecondary) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_user),
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Border,
                                    cursorColor = PrimaryBlue,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface
                                ),
                                isError = attemptedSubmit && !isNameValid
                            )
                            if (attemptedSubmit && !isNameValid) {
                                Text(
                                    "Please enter your full name",
                                    color = Error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Email
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Email address", color = TextSecondary) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_mail),
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Border,
                                    cursorColor = PrimaryBlue,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface
                                ),
                                isError = attemptedSubmit && !isEmailValid
                            )
                            if (attemptedSubmit && !isEmailValid) {
                                Text(
                                    "Please enter a valid email address",
                                    color = Error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Password
                            OutlinedTextField(
                                value = password,
                                onValueChange = { password = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Password (min 6 chars)", color = TextSecondary) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_lock),
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        painterResource(id = if (passwordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { passwordVisible = !passwordVisible }
                                    )
                                },
                                visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Border,
                                    cursorColor = PrimaryBlue,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface
                                ),
                                isError = attemptedSubmit && !isPasswordValid
                            )
                            if (attemptedSubmit && !isPasswordValid) {
                                Text(
                                    "Password must be at least 6 characters",
                                    color = Error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Confirm password
                            OutlinedTextField(
                                value = confirmPassword,
                                onValueChange = { confirmPassword = it },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                placeholder = { Text("Confirm password", color = TextSecondary) },
                                leadingIcon = {
                                    Icon(
                                        painterResource(R.drawable.ic_lock),
                                        contentDescription = null,
                                        tint = TextSecondary
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        painterResource(id = if (confirmPasswordVisible) R.drawable.ic_eye_off else R.drawable.ic_eye),
                                        contentDescription = null,
                                        tint = TextSecondary,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { confirmPasswordVisible = !confirmPasswordVisible }
                                    )
                                },
                                visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = PrimaryBlue,
                                    unfocusedBorderColor = Border,
                                    cursorColor = PrimaryBlue,
                                    focusedTextColor = TextPrimary,
                                    unfocusedTextColor = TextPrimary,
                                    focusedContainerColor = Surface,
                                    unfocusedContainerColor = Surface
                                ),
                                isError = attemptedSubmit && !doPasswordsMatch
                            )
                            if (attemptedSubmit && !doPasswordsMatch) {
                                Text(
                                    "Passwords do not match",
                                    color = Error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // --- Specialties (selectable chips) ---
                            var specialtiesExpanded by remember { mutableStateOf(false) }

                            Text(
                                text = "Specialties",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val visibleItems =
                                    if (specialtiesExpanded) TrainerSpecialties
                                    else TrainerSpecialties.take(4)

                                visibleItems.forEach { item ->
                                    val selected = specialties.contains(item)
                                    SelectableChip(
                                        text = item,
                                        selected = selected,
                                        onToggle = {
                                            specialties =
                                                if (selected) specialties - item
                                                else specialties + item
                                        }
                                    )
                                }
                            }

                            TextButton(
                                onClick = { specialtiesExpanded = !specialtiesExpanded }
                            ) {
                                Text(
                                    if (specialtiesExpanded) "Show Less"
                                    else "Show More",
                                    color = PrimaryBlue
                                )
                            }

                            // --- Certifications (selectable chips) ---
                            var certificationsExpanded by remember { mutableStateOf(false) }

                            Text(
                                text = "Certifications",
                                fontWeight = FontWeight.SemiBold,
                                color = TextPrimary
                            )

                            FlowRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                val visibleItems =
                                    if (certificationsExpanded) TrainerCertifications
                                    else TrainerCertifications.take(3)

                                visibleItems.forEach { item ->
                                    val selected = certifications.contains(item)
                                    SelectableChip(
                                        text = item,
                                        selected = selected,
                                        onToggle = {
                                            certifications =
                                                if (selected) certifications - item
                                                else certifications + item
                                        }
                                    )
                                }
                            }

                            TextButton(
                                onClick = { certificationsExpanded = !certificationsExpanded }
                            ) {
                                Text(
                                    if (certificationsExpanded) "Show Less"
                                    else "Show More",
                                    color = PrimaryBlue
                                )
                            }

                            // Terms checkbox row
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Checkbox(
                                    checked = termsAccepted,
                                    onCheckedChange = { termsAccepted = it },
                                    colors = CheckboxDefaults.colors(
                                        checkedColor = PrimaryBlue,
                                        uncheckedColor = TextSecondary
                                    )
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "I agree to the Terms and Privacy",
                                    modifier = Modifier.clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { termsAccepted = !termsAccepted },
                                    color = TextSecondary
                                )
                            }
                            if (attemptedSubmit && !termsAccepted) {
                                Text(
                                    "You must accept the Terms to continue",
                                    color = Error,
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }

                            // Register button
                            Button(
                                onClick = {
                                    attemptedSubmit = true
                                    if (isNameValid && isEmailValid && isPasswordValid && doPasswordsMatch && termsAccepted) {
                                        viewModel.register(
                                            email = email,
                                            password = password,
                                            name = name,
                                            specialties = specialties.toList(),
                                            certifications = certifications.toList()
                                        )
                                    } else {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                "Please fix the errors and try again"
                                            )
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = PrimaryBlue
                                ),
                                enabled = !isLoading
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(
                                        color = Color.White,
                                        strokeWidth = 2.dp,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Creating account...", color = Color.White)
                                } else {
                                    Text(
                                        "Register",
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                }
                            }

                            // Already have account
                            TextButton(
                                onClick = onBack,
                                modifier = Modifier
                                    .align(Alignment.CenterHorizontally)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null
                                    ) { onBack() }
                            ) {
                                Text("Already have an account? Sign In", color = TextPrimary)
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(36.dp)) }
            }
        }
    }
}