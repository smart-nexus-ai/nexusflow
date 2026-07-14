package com.smartnexus.nexusflow

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme

import javax.inject.Inject
import dagger.hilt.android.AndroidEntryPoint
import com.smartnexus.nexusflow.core.navigation.MainNavigation
import com.smartnexus.nexusflow.data.remote.RealtimeListenerManager

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var realtimeListenerManager: RealtimeListenerManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        realtimeListenerManager.startSync()
        enableEdgeToEdge()
        setContent {
            NexusFlowTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation()
                }
            }
        }
    }
}
