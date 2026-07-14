package com.smartnexus.nexusflow.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun MaintenanceDialog(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {}, // Empty lambda makes it non-dismissible on outside click
        icon = {
            Icon(
                imageVector = Icons.Default.Construction,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = "System Maintenance",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = "NexusFlow servers are undergoing scheduled maintenance. Remote control features are temporarily offline. Please try again in a few minutes."
            )
        },
        confirmButton = {
            Button(onClick = onRetry) {
                Text("Retry Connection")
            }
        },
        modifier = modifier
    )
}

@Composable
fun UpdateDialog(
    isForceUpdate: Boolean,
    onUpdateNow: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        onDismissRequest = {
            if (!isForceUpdate) onDismiss()
        },
        icon = {
            Icon(
                imageVector = Icons.Default.CloudSync,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(40.dp)
            )
        },
        title = {
            Text(
                text = if (isForceUpdate) "Update Required" else "Update Available",
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        },
        text = {
            Text(
                text = if (isForceUpdate) {
                    "A critical update is required to continue using NexusFlow. Please install the latest version from the Play Store."
                } else {
                    "A new version of NexusFlow is available with improved performance and new custom scene integrations."
                }
            )
        },
        dismissButton = {
            if (!isForceUpdate) {
                TextButton(onClick = onDismiss) {
                    Text("Remind Later")
                }
            }
        },
        confirmButton = {
            Button(onClick = onUpdateNow) {
                Text("Update Now")
            }
        },
        modifier = modifier
    )
}
