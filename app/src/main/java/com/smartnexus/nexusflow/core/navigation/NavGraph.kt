package com.smartnexus.nexusflow.core.navigation

import android.app.Activity
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation3.ui.NavDisplay
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import com.smartnexus.nexusflow.core.components.BottomNavBar
import com.smartnexus.nexusflow.core.components.DevDebugDrawer
import com.smartnexus.nexusflow.features.adddevice.AddDeviceScreen
import com.smartnexus.nexusflow.features.auth.AuthScreen
import com.smartnexus.nexusflow.features.devices.DevicesScreen
import com.smartnexus.nexusflow.features.home.HomeScreen
import com.smartnexus.nexusflow.features.onboarding.OnboardingScreen
import com.smartnexus.nexusflow.features.scenes.ScenesScreen
import com.smartnexus.nexusflow.features.schedules.SchedulesScreen
import com.smartnexus.nexusflow.features.settings.DonationScreen
import com.smartnexus.nexusflow.features.settings.SettingsScreen
import com.smartnexus.nexusflow.features.splash.SplashScreen

@Composable
fun MainNavigation() {
    val backStack = rememberNavBackStack(Screen.Splash)
    val currentRoute = (backStack.lastOrNull() as? Screen) ?: Screen.Splash
    val context = LocalContext.current

    val isTabRoute = currentRoute in listOf(
        Screen.Home,
        Screen.Devices,
        Screen.Schedules,
        Screen.Scenes,
        Screen.Settings
    )

    DevDebugDrawer {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (isTabRoute) {
                    BottomNavBar(
                        currentRoute = currentRoute,
                        onTabSelected = { tab ->
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.size - 1)
                                backStack.add(tab)
                            }
                        }
                    )
                }
            }
        ) { innerPadding ->
            NavDisplay(
                backStack = backStack,
                onBack = {
                    when (currentRoute) {
                        // Home screen: close the app
                        Screen.Home -> {
                            (context as? Activity)?.finish()
                        }
                        // Other tab screens: navigate back to Home
                        Screen.Devices, Screen.Schedules, Screen.Scenes, Screen.Settings -> {
                            if (backStack.isNotEmpty()) {
                                backStack.removeAt(backStack.size - 1)
                                backStack.add(Screen.Home)
                            }
                        }
                        // Sub-screens (AddDevice, Donation, etc.): normal pop
                        else -> {
                            if (backStack.size > 1) {
                                backStack.removeLastOrNull()
                            }
                        }
                    }
                },
                entryProvider = entryProvider {
                    entry<Screen.Splash> {
                        SplashScreen(
                            onNavigateForward = { target ->
                                backStack.clear()
                                backStack.add(target)
                            }
                        )
                    }
                    entry<Screen.Onboarding> {
                        OnboardingScreen(
                            onNavigateToAuth = {
                                backStack.clear()
                                backStack.add(Screen.Auth)
                            }
                        )
                    }
                    entry<Screen.Auth> {
                        AuthScreen(
                            onAuthSuccess = {
                                backStack.clear()
                                backStack.add(Screen.Home)
                            }
                        )
                    }
                    entry<Screen.Home> {
                        HomeScreen(
                            onNavigateToTab = { tab ->
                                if (backStack.isNotEmpty()) {
                                    backStack.removeAt(backStack.size - 1)
                                    backStack.add(tab)
                                }
                            }
                        )
                    }
                    entry<Screen.Devices> {
                        DevicesScreen(
                            onNavigateToAddDevice = {
                                backStack.add(Screen.AddDevice)
                            }
                        )
                    }
                    entry<Screen.AddDevice> {
                        AddDeviceScreen(
                            onFinish = {
                                backStack.removeLastOrNull()
                            },
                            onCancel = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }
                    entry<Screen.Schedules> {
                        SchedulesScreen(
                            onNavigateToTab = { tab ->
                                if (backStack.isNotEmpty()) {
                                    backStack.removeAt(backStack.size - 1)
                                    backStack.add(tab)
                                }
                            }
                        )
                    }
                    entry<Screen.Scenes> {
                        ScenesScreen(
                            onNavigateToTab = { tab ->
                                if (backStack.isNotEmpty()) {
                                    backStack.removeAt(backStack.size - 1)
                                    backStack.add(tab)
                                }
                            }
                        )
                    }
                    entry<Screen.Settings> {
                        SettingsScreen(
                            onNavigateToDonation = {
                                backStack.add(Screen.Donation)
                            },
                            onNavigateToAuth = {
                                backStack.clear()
                                backStack.add(Screen.Auth)
                            }
                        )
                    }
                    entry<Screen.Donation> {
                        DonationScreen(
                            onBackPress = {
                                backStack.removeLastOrNull()
                            }
                        )
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}
