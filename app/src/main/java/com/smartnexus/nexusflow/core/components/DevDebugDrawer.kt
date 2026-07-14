package com.smartnexus.nexusflow.core.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class UpdateType {
    NONE, SOFT, FORCE
}

object DevDebugConfig {
    private val _deviceCount = MutableStateFlow(4)
    val deviceCount = _deviceCount.asStateFlow()

    private val _bleConnected = MutableStateFlow(true)
    val bleConnected = _bleConnected.asStateFlow()

    private val _wifiOnline = MutableStateFlow(true)
    val wifiOnline = _wifiOnline.asStateFlow()

    private val _updateSimulation = MutableStateFlow(UpdateType.NONE)
    val updateSimulation = _updateSimulation.asStateFlow()

    private val _maintenanceMode = MutableStateFlow(false)
    val maintenanceMode = _maintenanceMode.asStateFlow()

    fun setDeviceCount(count: Int) { _deviceCount.value = count }
    fun setBleConnected(connected: Boolean) { _bleConnected.value = connected }
    fun setWifiOnline(online: Boolean) { _wifiOnline.value = online }
    fun setUpdateSimulation(type: UpdateType) { _updateSimulation.value = type }
    fun setMaintenanceMode(active: Boolean) { _maintenanceMode.value = active }
}

@Composable
fun DevDebugDrawer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        content()
    }
}
