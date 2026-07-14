package com.smartnexus.nexusflow.features.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.R
import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.DarkColorScheme
import com.smartnexus.nexusflow.core.theme.LightColorScheme

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AuthViewModel = hiltViewModel()
) {
    val isLoading by viewModel.isLoading.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.authSuccessEvent.collect {
            onAuthSuccess()
        }
    }

    val gso = remember {
        GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken("574085590971-o5gh01qvokli40pp5p9dlnsba7mb40b7.apps.googleusercontent.com")
            .requestEmail()
            .build()
    }
    val googleSignInClient = remember { GoogleSignIn.getClient(context, gso) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(com.google.android.gms.common.api.ApiException::class.java)
                val idToken = account.idToken
                if (idToken != null) {
                    viewModel.signInWithGoogle(idToken)
                } else {
                    android.util.Log.e("AuthScreen", "Google Sign In: ID Token is null")
                    android.widget.Toast.makeText(context, "Sign-in failed: ID Token is null", android.widget.Toast.LENGTH_LONG).show()
                }
            } catch (e: com.google.android.gms.common.api.ApiException) {
                android.util.Log.e("AuthScreen", "Google Sign In ApiException: status=${e.statusCode}, message=${e.message}", e)
                android.widget.Toast.makeText(context, "Google Sign-In Error Code: ${e.statusCode}\nMake sure SHA-1 is registered in Firebase Console", android.widget.Toast.LENGTH_LONG).show()
            } catch (e: Exception) {
                android.util.Log.e("AuthScreen", "Google Sign In Exception: message=${e.message}", e)
                android.widget.Toast.makeText(context, "Sign-in failed: ${e.localizedMessage}", android.widget.Toast.LENGTH_LONG).show()
            }
        } else {
            android.util.Log.e("AuthScreen", "Google Sign In activity result code is not OK: result=${result.resultCode}")
            android.widget.Toast.makeText(context, "Sign-in cancelled or failed (Code: ${result.resultCode})", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    AuthContent(
        isLoading = isLoading,
        onGoogleSignInClicked = {
            launcher.launch(googleSignInClient.signInIntent)
        },
        modifier = modifier
    )
}

@Composable
private fun AuthContent(
    isLoading: Boolean,
    onGoogleSignInClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Theme-aware colors
    val isDarkMode = MaterialTheme.colorScheme.background == DarkColorScheme.background
    
    // Dark mode colors
    val darkBackground = Color(0xFF0F1B2E)
    val darkAccentCyan = Color(0xFF00D9FF)
    val darkLightGray = Color(0xFFE8E8E8)
    val darkDarkGray = Color(0xFF8B8B8B)
    
    // Light mode colors (from theme palette)
    val lightBackground = LightColorScheme.background
    val lightAccent = LightColorScheme.primary
    val lightText = LightColorScheme.onBackground
    val lightMuted = LightColorScheme.onBackground.copy(alpha = 0.6f)
    
    val background = if (isDarkMode) darkBackground else lightBackground
    val accentCyan = if (isDarkMode) darkAccentCyan else lightAccent
    val lightGray = if (isDarkMode) darkLightGray else lightText
    val darkGray = if (isDarkMode) darkDarkGray else lightMuted
    val buttonBg = if (isDarkMode) Color(0xFFE8E8E8) else Color(0xFFF3F4F6)
    val buttonText = if (isDarkMode) Color(0xFF1F2937) else Color(0xFF111827)

    var isChecked by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // Branding Section with Logo and Text
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                // Logo - Circuit board house icon
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            color = if (isDarkMode) darkBackground else MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        painter = painterResource(
                            id = if (isDarkMode) R.drawable.logo_nexusflow_dark else R.drawable.logo_nexusflow_light
                        ),
                        contentDescription = "NexusFlow Logo",
                        modifier = Modifier.size(200.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                // App Name
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Nexus",
                        style = MaterialTheme.typography.headlineMedium,
                        color = lightGray,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                    Text(
                        text = "Flow",
                        style = MaterialTheme.typography.headlineMedium,
                        color = accentCyan,
                        fontWeight = FontWeight.Bold,
                        fontSize = 36.sp
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Tagline
                Text(
                    text = "Smart Home Automation",
                    style = MaterialTheme.typography.bodyLarge,
                    color = accentCyan,
                    textAlign = TextAlign.Center,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Main description
                Text(
                    text = "Control your home from anywhere.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = lightGray,
                    textAlign = TextAlign.Center,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )

                Spacer(modifier = Modifier.height(32.dp))

                // Features Row (Secure | Fast | Free)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeatureItem(
                        icon = "🛡️",
                        label = "Secure",
                        color = accentCyan
                    )
                    Divider()
                    FeatureItem(
                        icon = "⚡",
                        label = "Fast",
                        color = accentCyan
                    )
                    Divider()
                    FeatureItem(
                        icon = "🎁",
                        label = "Free",
                        color = accentCyan
                    )
                }
            }

            // Sign In Action Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Button(
                    onClick = onGoogleSignInClicked,
                    enabled = !isLoading && isChecked,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = buttonBg,
                        contentColor = buttonText,
                        disabledContainerColor = buttonBg.copy(alpha = 0.6f),
                        disabledContentColor = buttonText.copy(alpha = 0.6f)
                    ),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 0.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(24.dp),
                            color = accentCyan,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_google_logo),
                                contentDescription = "Google Logo",
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Terms and Privacy
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Checkbox(
                        checked = isChecked,
                        onCheckedChange = { isChecked = it },
                        modifier = Modifier.size(18.dp),
                        colors = CheckboxDefaults.colors(
                            checkedColor = accentCyan,
                            uncheckedColor = darkGray,
                            checkmarkColor = darkBackground
                        )
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "By signing up, you agree to our ",
                        style = MaterialTheme.typography.labelSmall,
                        color = darkGray,
                        fontSize = 12.sp
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Terms",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { uriHandler.openUri("https://nexusflow.vercel.app/terrms") }
                    )
                    Text(
                        text = " and ",
                        style = MaterialTheme.typography.labelSmall,
                        color = darkGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "Privacy",
                        style = MaterialTheme.typography.labelSmall,
                        color = accentCyan,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable { uriHandler.openUri("https://nexusflow.vercel.app/privacy") }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Security badge
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = accentCyan,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Secure Authentication by Google",
                        style = MaterialTheme.typography.labelSmall,
                        color = darkGray,
                        fontSize = 12.sp
                    )
                }
            }

            // Footer Section
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "v1.0.0",
                    style = MaterialTheme.typography.labelMedium,
                    color = darkGray,
                    textAlign = TextAlign.Center,
                    fontSize = 12.sp
                )

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun FeatureItem(
    icon: String,
    label: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = icon,
            fontSize = 24.sp
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun Divider(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .height(32.dp)
            .background(Color(0xFF8B8B8B).copy(alpha = 0.3f))
    )
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1B2E)
@Composable
private fun AuthScreenDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        Surface(
            color = Color(0xFF0F1B2E)
        ) {
            AuthContent(
                isLoading = false,
                onGoogleSignInClicked = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAFA)
@Composable
private fun AuthScreenLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        Surface(
            color = MaterialTheme.colorScheme.background
        ) {
            AuthContent(
                isLoading = false,
                onGoogleSignInClicked = {}
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0F1B2E)
@Composable
private fun AuthScreenLoadingPreview() {
    NexusFlowTheme(darkTheme = true) {
        Surface(
            color = Color(0xFF0F1B2E)
        ) {
            AuthContent(
                isLoading = true,
                onGoogleSignInClicked = {}
            )
        }
    }
}
