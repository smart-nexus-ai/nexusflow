package com.smartnexus.nexusflow.features.scenes

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SortByAlpha
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.smartnexus.nexusflow.core.components.DevDebugConfig
import com.smartnexus.nexusflow.core.components.EmptyState
import com.smartnexus.nexusflow.core.navigation.Screen
import com.smartnexus.nexusflow.core.theme.isAppInDarkTheme
import com.smartnexus.nexusflow.features.scenes.components.SceneAddEditSheet
import com.smartnexus.nexusflow.features.scenes.components.SceneGridCard

@Composable
fun ScenesScreen(
    onNavigateToTab: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ScenesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    if (!uiState.hasDevices) {
        EmptyState(
            icon = Icons.Default.Devices,
            title = "No Devices Connected",
            description = "You need to pair a NexusFlow smart relay device first before you can configure scenes.",
            buttonText = "Add Device",
            onButtonClick = { onNavigateToTab(Screen.Devices) },
            modifier = modifier.fillMaxSize()
        )
        return
    }

    val effectiveScenes = uiState.scenes
    val activeDetailsScene = effectiveScenes.find { it.id == uiState.activeDetailsSceneId }

    LaunchedEffect(Unit) {
        viewModel.activationSuccessEvent.collect { message ->
            Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
        }
    }

    // Render Scene Details Screen if a scene is selected
    if (activeDetailsScene != null) {
        BackHandler { viewModel.onCloseSceneDetails() }
        SceneDetailsScreen(
            scene = activeDetailsScene,
            onBackPress = viewModel::onCloseSceneDetails,
            onEditScene = { viewModel.onOpenEditScene(activeDetailsScene) },
            onEditSceneHeader = { newName, newIcon, newDesc -> viewModel.onEditSceneHeader(activeDetailsScene.id, newName, newIcon, newDesc) },
            onDeleteScene = { viewModel.onDeleteScene(activeDetailsScene.id) },
            onActivateScene = { viewModel.onActivateScene(activeDetailsScene.id) },
            onToggleShowOnHome = { show -> viewModel.onToggleShowOnHome(activeDetailsScene.id, show) },
            onToggleSceneRelay = { relayId, relayName -> viewModel.onToggleSceneRelay(activeDetailsScene.id, relayId, relayName) },
            onRemoveRelay = { relayId, relayName -> viewModel.onRemoveRelayFromScene(activeDetailsScene.id, relayId, relayName) },
            onAddRelay = { rName, dName, rId, dType -> viewModel.onAddRelayToScene(activeDetailsScene.id, rName, dName, rId, dType) },
            availableRelays = uiState.availableRelays,
            modifier = modifier
        )
        return
    }

    ScenesScreenContent(
        uiState = uiState,
        effectiveScenes = effectiveScenes,
        onOpenDetails = viewModel::onOpenSceneDetails,
        onToggleFavorite = viewModel::onToggleFavorite,
        onActivateScene = viewModel::onActivateScene,
        onOpenEditScene = viewModel::onOpenEditScene,
        onDeleteScene = viewModel::onDeleteScene,
        onFilterChanged = viewModel::onFilterChanged,
        onOpenAddScene = {
            if (effectiveScenes.size >= 6) {
                Toast.makeText(context, "Maximum limit of 6 scenes reached", Toast.LENGTH_SHORT).show()
            } else {
                viewModel.onOpenAddScene()
            }
        },
        modifier = modifier
    )

    // Render Add/Edit Scene Sheet Modal
    if (uiState.isAddEditSheetOpen) {
        SceneAddEditSheet(
            initialData = uiState.editingScene,
            onDismiss = viewModel::onCloseAddEditSheet,
            onSave = viewModel::onSaveScene,
            availableRelays = uiState.availableRelays
        )
    }
}

@Composable
fun ScenesScreenContent(
    uiState: ScenesUiState,
    effectiveScenes: List<SceneItem>,
    onOpenDetails: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onActivateScene: (String) -> Unit,
    onOpenEditScene: (SceneItem) -> Unit,
    onDeleteScene: (String) -> Unit,
    onFilterChanged: (String) -> Unit,
    onOpenAddScene: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isDark = isAppInDarkTheme()
    val context = LocalContext.current
    var filterMenuExpanded by remember { mutableStateOf(false) }

    val filteredScenes = when (uiState.selectedFilter) {
        "favorites" -> effectiveScenes.filter { it.isFavorite }
        "last_used" -> effectiveScenes.sortedBy { it.lastUsedText }
        "a_z" -> effectiveScenes.sortedBy { it.name }
        "z_a" -> effectiveScenes.sortedByDescending { it.name }
        else -> effectiveScenes
    }

    val totalCount = effectiveScenes.size

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Header Row (Title + Filter Button, Removed Search Icon)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Smart Scenes",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Create and activate custom scenes (Max 6)",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f)
                    )
                }

                Box {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                            .clickable { filterMenuExpanded = true },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Filter Scenes",
                            tint = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = filterMenuExpanded,
                        onDismissRequest = { filterMenuExpanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        mapOf(
                            "all" to "All Scenes",
                            "favorites" to "Favorites",
                            "last_used" to "Last Used",
                            "a_z" to "A-Z",
                            "z_a" to "Z-A"
                        ).forEach { (key, label) ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = label,
                                        fontWeight = if (uiState.selectedFilter == key) FontWeight.Bold else FontWeight.Normal,
                                        color = if (uiState.selectedFilter == key) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                    )
                                },
                                onClick = {
                                    onFilterChanged(key)
                                    filterMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Filter Pills Row (Favorites, Last Used, A-Z, Z-A)
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Pill 1: All
                item {
                    val isSelected = uiState.selectedFilter == "all"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("all") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.GridView,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "All ($totalCount)",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }

                // Pill 2: Favorites
                item {
                    val isSelected = uiState.selectedFilter == "favorites"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("favorites") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFF59E0B)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Favorites",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFFFBBF24) else Color(0xFFD97706)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }

                // Pill 3: Last Used
                item {
                    val isSelected = uiState.selectedFilter == "last_used"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("last_used") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF1E3A8A) else Color(0xFFEFF6FF)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF60A5FA) else Color(0xFF3B82F6)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Last Used",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFF60A5FA) else Color(0xFF2563EB)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }

                // Pill 4: A-Z
                item {
                    val isSelected = uiState.selectedFilter == "a_z"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("a_z") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "A-Z",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }

                // Pill 5: Z-A
                item {
                    val isSelected = uiState.selectedFilter == "z_a"
                    Surface(
                        modifier = Modifier
                            .height(40.dp)
                            .clickable { onFilterChanged("z_a") },
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) (if (isDark) Color(0xFF064E3B) else Color(0xFFDCFCE7)) else (if (isDark) Color(0xFF1E293B) else Color(0xFFF9FAFB)),
                        border = BorderStroke(1.dp, if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF10B981)) else (if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.SortByAlpha,
                                contentDescription = null,
                                tint = if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFF94A3B8) else Color(0xFF6B7280)),
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Z-A",
                                style = MaterialTheme.typography.labelLarge.copy(
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 13.sp
                                ),
                                color = if (isSelected) (if (isDark) Color(0xFF34D399) else Color(0xFF059669)) else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF4B5563))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Main 2-Column Scene Grid or Empty State
            if (filteredScenes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    EmptyState(
                        icon = Icons.Default.AutoAwesome,
                        title = "No Scenes Found",
                        description = "Create custom scenes to trigger multiple relay actions with a single tap",
                        buttonText = "Create Scene",
                        onButtonClick = onOpenAddScene
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    items(filteredScenes, key = { it.id }) { scene ->
                        SceneGridCard(
                            item = scene,
                            onClickCard = { onOpenDetails(scene.id) },
                            onToggleFavorite = { onToggleFavorite(scene.id) },
                            onActivate = { onActivateScene(scene.id) },
                            onEdit = { onOpenEditScene(scene) },
                            onDelete = { onDeleteScene(scene.id) }
                        )
                    }
                }
            }
        }

        // Bottom Summary Bar & Dual FABs (Removed Manage Scenes Button, Display Limit)
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary FAB: Auto-wizard
                FloatingActionButton(
                    onClick = { Toast.makeText(context, "Scene Auto-Wizard", Toast.LENGTH_SHORT).show() },
                    containerColor = if (isDark) Color(0xFF374151) else Color(0xFFF3F4F6),
                    contentColor = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                    shape = CircleShape,
                    modifier = Modifier.size(46.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "Scene Wizard",
                        modifier = Modifier.size(22.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                // Primary FAB: Add Scene
                FloatingActionButton(
                    onClick = onOpenAddScene,
                    containerColor = if (totalCount >= 6) Color(0xFF9CA3AF) else Color(0xFF4F46E5),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add Scene",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            // Summary Card (Shows Max 6 Limit, Removed Manage Scenes Button)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)),
                border = BorderStroke(1.dp, if (isDark) Color(0xFF374151) else Color(0xFFE5E7EB)),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Scene Capacity",
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold, fontSize = 13.sp),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (totalCount >= 6) "Maximum capacity reached (6/6)" else "You can create up to 6 scenes",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 11.sp),
                                color = if (totalCount >= 6) Color(0xFFEF4444) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f)
                            )
                        }
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (totalCount >= 6) Color(0xFFFEE2E2) else (if (isDark) Color(0xFF2E2A52) else Color(0xFFEEF2FF)))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = "$totalCount / 6 Max",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (totalCount >= 6) Color(0xFFEF4444) else (if (isDark) Color(0xFF818CF8) else Color(0xFF4F46E5))
                        )
                    }
                }
            }
        }
    }
}
