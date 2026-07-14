package com.smartnexus.nexusflow.features.settings

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ColorLens
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.ThemeMode

@Composable
fun SettingsScreen(
    onNavigateToDonation: () -> Unit,
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    SettingsContent(
        uiState = uiState,
        onThemeChanged = viewModel::onThemeModeSelected,
        onOpenProfile = viewModel::onOpenProfile,
        onOpenLogout = viewModel::onOpenLogout,
        onOpenDeleteAccount = viewModel::onOpenDeleteAccount,
        onNavigateToDonation = onNavigateToDonation,
        onHelpClick = { Toast.makeText(context, "Opening email client...", Toast.LENGTH_SHORT).show() },
        onRateClick = { Toast.makeText(context, "Opening Google Play Store...", Toast.LENGTH_SHORT).show() },
        onTermsClick = { Toast.makeText(context, "Opening WebView placeholder...", Toast.LENGTH_SHORT).show() },
        onCheckUpdates = { Toast.makeText(context, "You are on the latest version", Toast.LENGTH_SHORT).show() },
        modifier = modifier
    )

    // 1. Profile Details Dialog
    if (uiState.isProfileDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onCloseProfile,
            icon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, modifier = Modifier.size(36.dp), tint = MaterialTheme.colorScheme.primary) },
            title = { Text(text = "User Profile", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(text = "Name: ${uiState.profile.name}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Email: ${uiState.profile.email}", style = MaterialTheme.typography.bodyLarge)
                    Text(text = "Member Since: ${uiState.profile.memberSince}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Text(text = "Paired Devices: ${uiState.profile.pairedDevicesCount}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::onCloseProfile) {
                    Text("Close")
                }
            }
        )
    }

    // 2. Logout Confirm Dialog
    if (uiState.isLogoutDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onCloseLogout,
            icon = { Icon(imageVector = Icons.Default.Logout, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(text = "Logout", fontWeight = FontWeight.Bold) },
            text = { Text(text = "Are you sure you want to log out of your smart home profile?") },
            dismissButton = {
                TextButton(onClick = viewModel::onCloseLogout) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onCloseLogout()
                        viewModel.onLogout()
                        onNavigateToAuth()
                    }
                ) {
                    Text("Logout", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }

    // 3. Delete Account Dialog
    if (uiState.isDeleteAccountDialogOpen) {
        AlertDialog(
            onDismissRequest = viewModel::onCloseDeleteAccount,
            icon = { Icon(imageVector = Icons.Default.DeleteForever, contentDescription = null, tint = MaterialTheme.colorScheme.error) },
            title = { Text(text = "Delete Account", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "Warning: This action is permanent and deletes all stored automation parameters. Please type \"DELETE\" below to proceed.",
                        color = MaterialTheme.colorScheme.error
                    )
                    OutlinedTextField(
                        value = uiState.deleteConfirmationText,
                        onValueChange = viewModel::onDeleteConfirmationTextChanged,
                        label = { Text("Confirmation text") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::onCloseDeleteAccount) {
                    Text("Cancel")
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.onCloseDeleteAccount()
                        onNavigateToAuth()
                    },
                    enabled = uiState.deleteConfirmationText == "DELETE"
                ) {
                    Text("Delete Permanently", color = MaterialTheme.colorScheme.error)
                }
            }
        )
    }
}

@Composable
private fun SettingsContent(
    uiState: SettingsUiState,
    onThemeChanged: (ThemeMode) -> Unit,
    onOpenProfile: () -> Unit,
    onOpenLogout: () -> Unit,
    onOpenDeleteAccount: () -> Unit,
    onNavigateToDonation: () -> Unit,
    onHelpClick: () -> Unit,
    onRateClick: () -> Unit,
    onTermsClick: () -> Unit,
    onCheckUpdates: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 0.dp, bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "App Settings",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        // User profile Card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onOpenProfile),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier.padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = uiState.profile.avatarInitials,
                        style = MaterialTheme.typography.titleLarge.copy(fontSize = 18.sp),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = uiState.profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = uiState.profile.email,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                Icon(
                    imageVector = Icons.Default.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                )
            }
        }

        // Theme Configuration Selector Panel
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.ColorLens, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(text = "App Theme Mode", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ThemeOptionCard(
                        label = "System",
                        icon = Icons.Default.Settings,
                        isSelected = uiState.currentThemeMode == ThemeMode.SYSTEM,
                        onClick = { onThemeChanged(ThemeMode.SYSTEM) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        label = "Light",
                        icon = Icons.Default.WbSunny,
                        isSelected = uiState.currentThemeMode == ThemeMode.LIGHT,
                        onClick = { onThemeChanged(ThemeMode.LIGHT) },
                        modifier = Modifier.weight(1f)
                    )
                    ThemeOptionCard(
                        label = "Dark",
                        icon = Icons.Default.Bedtime,
                        isSelected = uiState.currentThemeMode == ThemeMode.DARK,
                        onClick = { onThemeChanged(ThemeMode.DARK) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Navigation settings list
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsNavigationRow(icon = Icons.Default.Favorite, label = "❤️ Donate via UPI ID", onClick = onNavigateToDonation)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsNavigationRow(icon = Icons.Default.Star, label = "Rate on Play Store", onClick = onRateClick)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsNavigationRow(icon = Icons.Default.Email, label = "Report an Issue / Help", onClick = onHelpClick)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsNavigationRow(icon = Icons.Default.Info, label = "Check for Updates", onClick = onCheckUpdates)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsNavigationRow(icon = Icons.Default.Info, label = "Terms & Privacy Policy", onClick = onTermsClick)
            }
        }

        // Danger zone
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.medium,
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column {
                SettingsNavigationRow(icon = Icons.Default.Logout, label = "Log Out Profile", tint = MaterialTheme.colorScheme.error, onClick = onOpenLogout)
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), modifier = Modifier.padding(horizontal = 16.dp))
                SettingsNavigationRow(icon = Icons.Default.DeleteForever, label = "Delete Account Permanently", tint = MaterialTheme.colorScheme.error, onClick = onOpenDeleteAccount)
            }
        }

        // Version metadata
        Text(
            text = "v1.0.0 (Build 1)",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.4f),
            textAlign = TextAlign.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp)
        )
    }
}

@Composable
private fun ThemeOptionCard(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val border = if (isSelected) {
        BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
    } else {
        BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    }

    val containerColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
    }

    val contentColor = if (isSelected) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Card(
        modifier = modifier
            .height(76.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        border = border,
        colors = CardDefaults.cardColors(
            containerColor = containerColor,
            contentColor = contentColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(22.dp),
                tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun SettingsNavigationRow(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Spacer(modifier = Modifier.width(16.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = tint, fontWeight = FontWeight.Medium)
        }
        Icon(imageVector = Icons.Default.ChevronRight, contentDescription = null, tint = tint.copy(alpha = 0.4f), modifier = Modifier.size(20.dp))
    }
}

@Preview(showBackground = true, name = "Settings Dark")
@Composable
private fun SettingsScreenDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        Surface {
            SettingsContent(
                uiState = SettingsUiState(),
                onThemeChanged = {},
                onOpenProfile = {},
                onOpenLogout = {},
                onOpenDeleteAccount = {},
                onNavigateToDonation = {},
                onHelpClick = {},
                onRateClick = {},
                onTermsClick = {},
                onCheckUpdates = {}
            )
        }
    }
}

@Preview(showBackground = true, name = "Settings Light")
@Composable
private fun SettingsScreenLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        Surface {
            SettingsContent(
                uiState = SettingsUiState(),
                onThemeChanged = {},
                onOpenProfile = {},
                onOpenLogout = {},
                onOpenDeleteAccount = {},
                onNavigateToDonation = {},
                onHelpClick = {},
                onRateClick = {},
                onTermsClick = {},
                onCheckUpdates = {}
            )
        }
    }
}
