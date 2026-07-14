package com.smartnexus.nexusflow.features.adddevice

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.features.adddevice.components.Step1Preparation
import com.smartnexus.nexusflow.features.adddevice.components.Step2BleScan
import com.smartnexus.nexusflow.features.adddevice.components.Step3PinEntry
import com.smartnexus.nexusflow.features.adddevice.components.Step4WifiProvision
import com.smartnexus.nexusflow.features.adddevice.components.Step5Success
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.widget.Toast
import android.os.Build
import android.Manifest
import androidx.core.content.ContextCompat
import android.content.pm.PackageManager

@Composable
fun AddDeviceScreen(
    onFinish: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AddDeviceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val permissionsToRequest = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    } else {
        arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    val permissionsLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val scanGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_SCAN] == true
        } else true
        val connectGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissions[Manifest.permission.BLUETOOTH_CONNECT] == true
        } else true
        val locationGranted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        
        if (scanGranted && connectGranted && locationGranted) {
            viewModel.nextStep()
        } else {
            Toast.makeText(context, "Bluetooth and Location permissions are required for device provisioning", Toast.LENGTH_LONG).show()
        }
    }

    BackHandler {
        val handled = viewModel.previousStep()
        if (!handled) {
            onCancel()
        }
    }

    AddDeviceContent(
        uiState = uiState,
        onNextStep = {
            val hasScan = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
            } else true
            val hasConnect = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
            } else true
            val hasLocation = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

            if (hasScan && hasConnect && hasLocation) {
                viewModel.nextStep()
            } else {
                permissionsLauncher.launch(permissionsToRequest)
            }
        },
        onPreviousStep = {
            val handled = viewModel.previousStep()
            if (!handled) onCancel()
        },
        onSelectBleDevice = viewModel::selectBleDevice,
        onPinChange = viewModel::updatePin,
        onSubmitPin = viewModel::submitPin,
        onSsidChange = viewModel::updateSsid,
        onPasswordChange = viewModel::updatePassword,
        onProvisionWifi = viewModel::nextStep,
        onFinish = onFinish,
        onAddAnother = viewModel::resetWizard,
        onCancel = onCancel,
        modifier = modifier
    )
}

@Composable
private fun AddDeviceContent(
    uiState: AddDeviceUiState,
    onNextStep: () -> Unit,
    onPreviousStep: () -> Unit,
    onSelectBleDevice: (com.smartnexus.nexusflow.features.adddevice.components.BleDiscoveredDevice) -> Unit,
    onPinChange: (String) -> Unit,
    onSubmitPin: () -> Unit,
    onSsidChange: (String) -> Unit,
    onPasswordChange: (String) -> Unit,
    onProvisionWifi: () -> Unit,
    onFinish: () -> Unit,
    onAddAnother: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        // Wizard Top Header
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Step ${uiState.currentStep} of ${uiState.totalSteps}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )

                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Setup",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
                }
            }

            LinearProgressIndicator(
                progress = { uiState.currentStep.toFloat() / uiState.totalSteps.toFloat() },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            )
        }

        // Animated Step Transition
        Crossfade(
            targetState = uiState.currentStep,
            label = "wizard_step_transition",
            modifier = Modifier.weight(1f)
        ) { step ->
            when (step) {
                1 -> Step1Preparation(onNext = onNextStep)
                2 -> Step2BleScan(
                    devices = uiState.discoveredDevices,
                    onSelectDevice = onSelectBleDevice
                )
                3 -> Step3PinEntry(
                    pin = uiState.pinInput,
                    errorMessage = uiState.pinError,
                    onPinChange = onPinChange,
                    onSubmitPin = onSubmitPin,
                    onCancel = onPreviousStep
                )
                4 -> Step4WifiProvision(
                    selectedSsid = uiState.selectedSsid,
                    password = uiState.wifiPassword,
                    onSsidChange = onSsidChange,
                    onPasswordChange = onPasswordChange,
                    onProvision = onProvisionWifi
                )
                5 -> Step5Success(
                    confirmation = uiState.confirmationData,
                    onFinish = onFinish,
                    onAddAnother = onAddAnother
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AddDeviceScreenPreview() {
    NexusFlowTheme(darkTheme = true) {
        Surface {
            AddDeviceContent(
                uiState = AddDeviceUiState(),
                onNextStep = {},
                onPreviousStep = {},
                onSelectBleDevice = {},
                onPinChange = {},
                onSubmitPin = {},
                onSsidChange = {},
                onPasswordChange = {},
                onProvisionWifi = {},
                onFinish = {},
                onAddAnother = {},
                onCancel = {}
            )
        }
    }
}
