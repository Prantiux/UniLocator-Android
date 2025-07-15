package com.pramanshav.unilocator.ui.auth

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun AuthScreen(
    onSignIn: (email: String, password: String) -> Unit,
    onSignUp: (name: String, email: String, password: String) -> Unit,
    isLoading: Boolean = false,
    errorMessage: String? = null
) {
    var isLoginMode by remember { mutableStateOf(true) }
    
    // Login fields
    var loginEmail by remember { mutableStateOf("") }
    var loginPassword by remember { mutableStateOf("") }
    
    // Register fields
    var registerName by remember { mutableStateOf("") }
    var registerEmail by remember { mutableStateOf("") }
    var registerPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    
    // Password visibility
    var showLoginPassword by remember { mutableStateOf(false) }
    var showRegisterPassword by remember { mutableStateOf(false) }
    var showConfirmPassword by remember { mutableStateOf(false) }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                        MaterialTheme.colorScheme.surface
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(60.dp))
            
            // App Title
            Text(
                text = "UniLocator",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 42.sp
                ),
                color = MaterialTheme.colorScheme.primary
            )
            
            Spacer(modifier = Modifier.height(40.dp))
            
            // Auth Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                )
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Tab Row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        AuthTab(
                            text = "Sign In",
                            isSelected = isLoginMode,
                            onClick = { isLoginMode = true },
                            modifier = Modifier.weight(1f)
                        )
                        AuthTab(
                            text = "Sign Up",
                            isSelected = !isLoginMode,
                            onClick = { isLoginMode = false },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // Animated Content
                    AnimatedContent(
                        targetState = isLoginMode,
                        transitionSpec = {
                            slideInHorizontally(initialOffsetX = { if (targetState) -it else it }) + 
                            fadeIn() togetherWith 
                            slideOutHorizontally(targetOffsetX = { if (targetState) it else -it }) + 
                            fadeOut()
                        },
                        label = "auth_content"
                    ) { loginMode ->
                        if (loginMode) {
                            // Login Form
                            LoginForm(
                                email = loginEmail,
                                onEmailChange = { loginEmail = it },
                                password = loginPassword,
                                onPasswordChange = { loginPassword = it },
                                showPassword = showLoginPassword,
                                onTogglePasswordVisibility = { showLoginPassword = !showLoginPassword },
                                onSignIn = { onSignIn(loginEmail, loginPassword) },
                                isLoading = isLoading
                            )
                        } else {
                            // Register Form
                            RegisterForm(
                                name = registerName,
                                onNameChange = { registerName = it },
                                email = registerEmail,
                                onEmailChange = { registerEmail = it },
                                password = registerPassword,
                                onPasswordChange = { registerPassword = it },
                                confirmPassword = confirmPassword,
                                onConfirmPasswordChange = { confirmPassword = it },
                                showPassword = showRegisterPassword,
                                onTogglePasswordVisibility = { showRegisterPassword = !showRegisterPassword },
                                showConfirmPassword = showConfirmPassword,
                                onToggleConfirmPasswordVisibility = { showConfirmPassword = !showConfirmPassword },
                                onSignUp = { onSignUp(registerName, registerEmail, registerPassword) },
                                isLoading = isLoading
                            )
                        }
                    }
                    
                    // Error Message
                    if (errorMessage != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = errorMessage,
                                modifier = Modifier.padding(16.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AuthTab(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(40.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primary 
                          else MaterialTheme.colorScheme.surface,
            contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary 
                          else MaterialTheme.colorScheme.onSurface
        ),
        shape = RoundedCornerShape(8.dp),
        elevation = if (isSelected) ButtonDefaults.buttonElevation(defaultElevation = 2.dp) 
                   else ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}

@Composable
private fun LoginForm(
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    onSignIn: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Welcome Back!",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Sign in to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Email Field
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        // Password Field
        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Sign In Button
        Button(
            onClick = onSignIn,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Sign In",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun RegisterForm(
    name: String,
    onNameChange: (String) -> Unit,
    email: String,
    onEmailChange: (String) -> Unit,
    password: String,
    onPasswordChange: (String) -> Unit,
    confirmPassword: String,
    onConfirmPasswordChange: (String) -> Unit,
    showPassword: Boolean,
    onTogglePasswordVisibility: () -> Unit,
    showConfirmPassword: Boolean,
    onToggleConfirmPasswordVisibility: () -> Unit,
    onSignUp: () -> Unit,
    isLoading: Boolean
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Create Account",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.primary
        )
        
        Text(
            text = "Sign up to get started",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Name Field
        AuthTextField(
            value = name,
            onValueChange = onNameChange,
            label = "Full Name",
            leadingIcon = Icons.Default.Person,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )
        
        // Email Field
        AuthTextField(
            value = email,
            onValueChange = onEmailChange,
            label = "Email",
            leadingIcon = Icons.Default.Email,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
        )
        
        // Password Field
        AuthTextField(
            value = password,
            onValueChange = onPasswordChange,
            label = "Password",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            onTrailingIconClick = onTogglePasswordVisibility,
            visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
        )
        
        // Confirm Password Field
        AuthTextField(
            value = confirmPassword,
            onValueChange = onConfirmPasswordChange,
            label = "Confirm Password",
            leadingIcon = Icons.Default.Lock,
            trailingIcon = if (showConfirmPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
            onTrailingIconClick = onToggleConfirmPasswordVisibility,
            visualTransformation = if (showConfirmPassword) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            isError = confirmPassword.isNotEmpty() && password != confirmPassword
        )
        
        if (confirmPassword.isNotEmpty() && password != confirmPassword) {
            Text(
                text = "Passwords don't match",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        
        // Sign Up Button
        Button(
            onClick = onSignUp,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = !isLoading && name.isNotBlank() && email.isNotBlank() && 
                     password.isNotBlank() && password == confirmPassword,
            shape = RoundedCornerShape(12.dp)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = MaterialTheme.colorScheme.onPrimary
                )
            } else {
                Text(
                    text = "Sign Up",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    leadingIcon: ImageVector,
    modifier: Modifier = Modifier,
    trailingIcon: ImageVector? = null,
    onTrailingIconClick: (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    isError: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = {
            Icon(
                imageVector = leadingIcon,
                contentDescription = null,
                tint = if (isError) MaterialTheme.colorScheme.error 
                      else MaterialTheme.colorScheme.primary
            )
        },
        trailingIcon = if (trailingIcon != null) {
            {
                IconButton(onClick = { onTrailingIconClick?.invoke() }) {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else null,
        visualTransformation = visualTransformation,
        keyboardOptions = keyboardOptions,
        isError = isError,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = MaterialTheme.colorScheme.primary,
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
            focusedLabelColor = MaterialTheme.colorScheme.primary,
            cursorColor = MaterialTheme.colorScheme.primary
        )
    )
}
