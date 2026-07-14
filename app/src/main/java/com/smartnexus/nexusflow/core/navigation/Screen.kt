package com.smartnexus.nexusflow.core.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Screen : NavKey {
    @Serializable data object Splash : Screen
    @Serializable data object Onboarding : Screen
    @Serializable data object Auth : Screen
    
    // Bottom Tab Destinations
    @Serializable data object Home : Screen
    @Serializable data object Devices : Screen
    @Serializable data object Schedules : Screen
    @Serializable data object Scenes : Screen
    @Serializable data object Settings : Screen
    
    // Custom subflows
    @Serializable data object AddDevice : Screen
    @Serializable data object Donation : Screen
}
