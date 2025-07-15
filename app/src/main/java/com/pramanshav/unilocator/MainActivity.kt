package com.pramanshav.unilocator

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.pramanshav.unilocator.components.MainApp
import com.pramanshav.unilocator.ui.auth.AuthScreen
import com.pramanshav.unilocator.ui.theme.UniLocatorTheme
import com.pramanshav.unilocator.testing.FirebaseTestActivity
import com.pramanshav.unilocator.utils.AuthenticationManager
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    
    private lateinit var auth: FirebaseAuth
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        auth = FirebaseAuth.getInstance()
        
        setContent {
            UniLocatorTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    var isAuthenticated by remember { mutableStateOf(auth.currentUser != null) }
                    var isLoading by remember { mutableStateOf(false) }
                    var errorMessage by remember { mutableStateOf<String?>(null) }
                    val context = LocalContext.current
                    
                    LaunchedEffect(Unit) {
                        // Listen for auth state changes
                        val authStateListener = FirebaseAuth.AuthStateListener { firebaseAuth ->
                            val wasAuthenticated = isAuthenticated
                            isAuthenticated = firebaseAuth.currentUser != null
                            
                            // Handle successful login (device registration)
                            if (!wasAuthenticated && isAuthenticated) {
                                launch {
                                    try {
                                        val authManager = AuthenticationManager()
                                        val result = authManager.onLoginSuccess(this@MainActivity)
                                        when (result) {
                                            is AuthenticationManager.AuthResult.Success -> {
                                                android.util.Log.d("MainActivity", "✅ Device registered on login")
                                            }
                                            is AuthenticationManager.AuthResult.Error -> {
                                                android.util.Log.e("MainActivity", "❌ Device registration failed: ${result.message}")
                                            }
                                        }
                                    } catch (e: Exception) {
                                        android.util.Log.e("MainActivity", "❌ Error in post-login setup: ${e.message}")
                                    }
                                }
                            }
                        }
                        auth.addAuthStateListener(authStateListener)
                    }
                    
                    if (isAuthenticated) {
                        MainApp(
                            onSignOut = {
                                auth.signOut()
                                isAuthenticated = false
                            }
                        )
                    } else {
                        AuthScreen(
                            onSignIn = { email, password ->
                                isLoading = true
                                errorMessage = null
                                auth.signInWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            isAuthenticated = true
                                        } else {
                                            errorMessage = when (task.exception) {
                                                is FirebaseAuthInvalidCredentialsException -> "Invalid email or password"
                                                else -> task.exception?.message ?: "Sign in failed"
                                            }
                                        }
                                    }
                            },
                            onSignUp = { name, email, password ->
                                isLoading = true
                                errorMessage = null
                                auth.createUserWithEmailAndPassword(email, password)
                                    .addOnCompleteListener { task ->
                                        isLoading = false
                                        if (task.isSuccessful) {
                                            // Update profile with name
                                            val user = auth.currentUser
                                            val profileUpdates = com.google.firebase.auth.UserProfileChangeRequest.Builder()
                                                .setDisplayName(name)
                                                .build()
                                            user?.updateProfile(profileUpdates)
                                            isAuthenticated = true
                                        } else {
                                            errorMessage = when (task.exception) {
                                                is FirebaseAuthWeakPasswordException -> "Password is too weak"
                                                is FirebaseAuthUserCollisionException -> "An account with this email already exists"
                                                else -> task.exception?.message ?: "Sign up failed"
                                            }
                                        }
                                    }
                            },
                            isLoading = isLoading,
                            errorMessage = errorMessage
                        )
                    }
                }
            }
        }
    }
}
