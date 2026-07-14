package com.smartnexus.nexusflow.core.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.smartnexus.nexusflow.core.navigation.Screen
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme

@Composable
fun BottomNavBar(
    currentRoute: Screen,
    onTabSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    val items = listOf(
        BottomNavItem(
            route = Screen.Home,
            title = "Home",
            selectedIcon = Icons.Default.Home,
            unselectedIcon = Icons.Outlined.Home
        ),
        BottomNavItem(
            route = Screen.Devices,
            title = "Devices",
            selectedIcon = Icons.Default.Devices,
            unselectedIcon = Icons.Outlined.Devices
        ),
        BottomNavItem(
            route = Screen.Schedules,
            title = "Schedule",
            selectedIcon = Icons.Default.Schedule,
            unselectedIcon = Icons.Outlined.Schedule
        ),
        BottomNavItem(
            route = Screen.Scenes,
            title = "Scenes",
            selectedIcon = Icons.Default.AutoAwesome,
            unselectedIcon = Icons.Outlined.AutoAwesome
        ),
        BottomNavItem(
            route = Screen.Settings,
            title = "Settings",
            selectedIcon = Icons.Default.Settings,
            unselectedIcon = Icons.Outlined.Settings
        )
    )

    NavigationBar(
        modifier = modifier
    ) {
        items.forEach { item ->
            val isSelected = currentRoute == item.route
            NavigationBarItem(
                selected = isSelected,
                onClick = { onTabSelected(item.route) },
                icon = {
                    Icon(
                        imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                        contentDescription = item.title
                    )
                },
                label = { Text(text = item.title) }
            )
        }
    }
}

private data class BottomNavItem(
    val route: Screen,
    val title: String,
    val selectedIcon: androidx.compose.ui.graphics.vector.ImageVector,
    val unselectedIcon: androidx.compose.ui.graphics.vector.ImageVector
)

@Preview(showBackground = true)
@Composable
fun BottomNavBarLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        BottomNavBar(currentRoute = Screen.Home, onTabSelected = {})
    }
}

@Preview(showBackground = true)
@Composable
fun BottomNavBarDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        BottomNavBar(currentRoute = Screen.Home, onTabSelected = {})
    }
}
