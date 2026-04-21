package com.example.fitlink.ui.screens.auth

import android.util.Patterns
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fitlink.R

@Composable
fun RegisterScreen(
    onNavigate: (String) -> Unit,
    onRegisterClick: (String, String, String, String) -> Unit, // Removed accountType
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var termsAccepted by remember { mutableStateOf(false) }
    var showErrorDialog by remember { mutableStateOf(errorMessage != null) }

    // Password visibility states
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }

    // Validation states
    val isEmailValid = remember(email) {
        Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
    val isPasswordValid = remember(password) {
        password.length >= 6
    }
    val doPasswordsMatch = remember(password, confirmPassword) {
        password == confirmPassword && password.isNotEmpty()
    }
    val isPhoneValid = remember(phone) {
        phone.length >= 10
    }

    // Update dialog visibility when errorMessage changes
    LaunchedEffect(errorMessage) {
        showErrorDialog = errorMessage != null
    }

    // Error Dialog
    if (showErrorDialog && errorMessage != null) {
        AlertDialog(
            onDismissRequest = { showErrorDialog = false },
            title = {
                Text(
                    text = "Registration Failed",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF111827)
                )
            },
            text = {
                Text(
                    text = errorMessage,
                    fontSize = 16.sp,
                    color = Color(0xFF6B7280),
                    lineHeight = 24.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = { showErrorDialog = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF059669)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Try Again",
                        color = Color.White,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = Color.White
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.linearGradient(
                    colors = listOf(Color(0xFF10B981), Color(0xFF0D9488), Color(0xFF06B6D4))
                )
            )
            .verticalScroll(rememberScrollState()),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            // Logo
            Image(
                painter = painterResource(id = R.drawable.fitlink),
                contentDescription = "FitLink Logo",
                modifier = Modifier
                    .size(100.dp)
                    .shadow(
                        elevation = 16.dp,
                        shape = RoundedCornerShape(24.dp),
                        clip = true,
                        ambientColor = Color(0x33000000),
                        spotColor = Color(0x1A000000)
                    )
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White),
                contentScale = ContentScale.Fit
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Title
            Text(
                text = "Create Account",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 0.5.sp
            )

            Text(
                text = "Join FitLink today",
                color = Color(0xFFCCFBF1),
                fontSize = 16.sp,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Form Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(
                        elevation = 20.dp,
                        shape = RoundedCornerShape(32.dp),
                        ambientColor = Color(0x33000000),
                        spotColor = Color(0x1A000000)
                    ),
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.98f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    // Name Input
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Full name",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_user),
                                contentDescription = "Name",
                                tint = if (name.isNotEmpty()) Color(0xFF059669) else Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF059669),
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Email Input with validation
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Email address",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_mail),
                                contentDescription = "Email",
                                tint = if (email.isNotEmpty()) {
                                    if (isEmailValid) Color(0xFF059669) else Color(0xFFEF4444)
                                } else Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        isError = email.isNotEmpty() && !isEmailValid,
                        supportingText = if (email.isNotEmpty() && !isEmailValid) {
                            {
                                Text(
                                    "Please enter a valid email",
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF059669),
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Phone Input with validation
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Phone number",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_phone),
                                contentDescription = "Phone",
                                tint = if (phone.isNotEmpty()) {
                                    if (isPhoneValid) Color(0xFF059669) else Color(0xFFEF4444)
                                } else Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        isError = phone.isNotEmpty() && !isPhoneValid,
                        supportingText = if (phone.isNotEmpty() && !isPhoneValid) {
                            {
                                Text(
                                    "Please enter a valid phone number",
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        } else null,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF059669),
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Password Input with strength indicator
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Password (min. 6 characters)",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock),
                                contentDescription = "Password",
                                tint = if (password.isNotEmpty()) {
                                    if (isPasswordValid) Color(0xFF059669) else Color(0xFFEF4444)
                                } else Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { passwordVisible = !passwordVisible }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (passwordVisible) R.drawable.ic_eye_off
                                        else R.drawable.ic_eye
                                    ),
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = password.isNotEmpty() && !isPasswordValid,
                        supportingText = if (password.isNotEmpty() && !isPasswordValid) {
                            {
                                Text(
                                    "Password must be at least 6 characters",
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF059669),
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Confirm Password Input
                    OutlinedTextField(
                        value = confirmPassword,
                        onValueChange = { confirmPassword = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = {
                            Text(
                                "Confirm password",
                                color = Color(0xFF9CA3AF),
                                fontSize = 15.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_lock),
                                contentDescription = "Confirm Password",
                                tint = if (confirmPassword.isNotEmpty()) {
                                    if (doPasswordsMatch) Color(0xFF059669) else Color(0xFFEF4444)
                                } else Color(0xFF9CA3AF),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = { confirmPasswordVisible = !confirmPasswordVisible }
                            ) {
                                Icon(
                                    painter = painterResource(
                                        id = if (confirmPasswordVisible) R.drawable.ic_eye_off
                                        else R.drawable.ic_eye
                                    ),
                                    contentDescription = if (confirmPasswordVisible) "Hide password" else "Show password",
                                    tint = Color(0xFF9CA3AF),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        isError = confirmPassword.isNotEmpty() && !doPasswordsMatch,
                        supportingText = if (confirmPassword.isNotEmpty() && !doPasswordsMatch) {
                            {
                                Text(
                                    "Passwords do not match",
                                    color = Color(0xFFEF4444),
                                    fontSize = 12.sp
                                )
                            }
                        } else null,
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFF059669),
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            cursorColor = Color(0xFF059669),
                            focusedTextColor = Color(0xFF111827),
                            unfocusedTextColor = Color(0xFF111827),
                            focusedContainerColor = Color(0xFFF9FAFB),
                            unfocusedContainerColor = Color(0xFFF9FAFB)
                        )
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    // Terms Checkbox - Improved design
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = termsAccepted,
                            onCheckedChange = { termsAccepted = it },
                            colors = CheckboxDefaults.colors(
                                checkedColor = Color(0xFF059669)
                            )
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        Text(
                            text = "I agree to the Terms and Privacy",
                            color = Color(0xFF6B7280),
                            fontSize = 14.sp
                        )
                    }


                    Spacer(modifier = Modifier.height(24.dp))

                    // Register Button
                    Button(
                        onClick = {
                            if (validateRegistration(
                                    name, email, phone, password, confirmPassword, termsAccepted,
                                    isEmailValid, isPasswordValid, doPasswordsMatch, isPhoneValid
                                )) {
                                onRegisterClick(email, password, name, phone) // Removed accountType
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF059669),
                            disabledContainerColor = Color(0xFF9CA3AF).copy(alpha = 0.5f),
                            contentColor = Color.White,
                            disabledContentColor = Color.White
                        ),
                        enabled = !isLoading && validateRegistration(
                            name, email, phone, password, confirmPassword, termsAccepted,
                            isEmailValid, isPasswordValid, doPasswordsMatch, isPhoneValid
                        )
                    ) {
                        if (isLoading) {
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Creating Account...",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        } else {
                            Text(
                                text = "Create Account",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    // Login Link
                    Row(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .clickable { onNavigate("login") },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Already have an account? ",
                            color = Color(0xFF6B7280),
                            fontSize = 15.sp
                        )
                        Text(
                            text = "Sign In",
                            color = Color(0xFF059669),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

// Validation function
private fun validateRegistration(
    name: String,
    email: String,
    phone: String,
    password: String,
    confirmPassword: String,
    termsAccepted: Boolean,
    isEmailValid: Boolean,
    isPasswordValid: Boolean,
    doPasswordsMatch: Boolean,
    isPhoneValid: Boolean
): Boolean {
    return name.isNotEmpty() &&
            email.isNotEmpty() && isEmailValid &&
            phone.isNotEmpty() && isPhoneValid &&
            password.isNotEmpty() && isPasswordValid &&
            doPasswordsMatch &&
            termsAccepted
}