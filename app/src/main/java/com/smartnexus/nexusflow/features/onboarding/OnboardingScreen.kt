package com.smartnexus.nexusflow.features.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeviceHub
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.DarkColorScheme
import com.smartnexus.nexusflow.core.theme.LightColorScheme
import com.smartnexus.nexusflow.R
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.res.painterResource
import kotlinx.coroutines.launch

// ─── Data model ────────────────────────────────────────────────────────────────

data class FeatureItem(
    val icon: ImageVector,
    val title: String,
    val description: String
)

private val featureItems = listOf(
    FeatureItem(
        icon = Icons.Default.DeviceHub,
        title = "Instant Control",
        description = "Toggle relays and appliances from anywhere"
    ),
    FeatureItem(
        icon = Icons.Default.Thermostat,
        title = "Temp & Humidity",
        description = "Monitor room climate in real time"
    ),
    FeatureItem(
        icon = Icons.Default.CalendarMonth,
        title = "Scheduling",
        description = "Set smart timers for daily routines"
    ),
    FeatureItem(
        icon = Icons.Default.AutoAwesome,
        title = "Scenes",
        description = "One tap to activate preset automations"
    )
)

// ─── Main onboarding screen ─────────────────────────────────────────────────────

@Composable
fun OnboardingScreen(
    onNavigateToAuth: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == 2
    
    // Detect theme by checking background color brightness
    val bgColor = MaterialTheme.colorScheme.background
    val brightness = (bgColor.red * 299 + bgColor.green * 587 + bgColor.blue * 114) / 1000
    val isDarkMode = brightness < 0.5f

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Skip button — use AnimatedVisibility at ColumnScope level to avoid BoxScope conflict
        AnimatedVisibility(
            visible = !isLastPage,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
        ) {
            Box(contentAlignment = Alignment.CenterEnd, modifier = Modifier.fillMaxWidth()) {
                TextButton(onClick = onNavigateToAuth) {
                    Text(
                        text = "Skip",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Pager slides
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> WelcomeSlide()
                1 -> FeaturesSlide()
                2 -> GetStartedSlide()
            }
        }

        // Bottom section: dots + Next/Done button
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Page indicator dots
            DotsIndicator(
                totalDots = 3,
                currentIndex = pagerState.currentPage
            )

            // CTA button
            Button(
                onClick = {
                    if (isLastPage) {
                        onNavigateToAuth()
                    } else {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = if (isLastPage) "Get Started" else "Next",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}

// ─── Slide 1: Welcome ──────────────────────────────────────────────────────────

@Composable
private fun WelcomeSlide(modifier: Modifier = Modifier) {
    // Detect theme by checking background color brightness
    val bgColor = MaterialTheme.colorScheme.background
    val brightness = (bgColor.red * 299 + bgColor.green * 587 + bgColor.blue * 114) / 1000
    val isDarkMode = brightness < 0.5f
    
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(140.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(
                    id = if (isDarkMode) R.drawable.logo_nexusflow_dark else R.drawable.logo_nexusflow_light
                ),
                contentDescription = "NexusFlow Logo",
                modifier = Modifier.size(100.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to NexusFlow",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Smart Home Automation Made Simple",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Slide 2: Features grid ────────────────────────────────────────────────────

@Composable
private fun FeaturesSlide(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Everything You Need",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Manage your smart home effortlessly",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(28.dp))

        // 2×2 grid using two rows
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(item = featureItems[0], modifier = Modifier.weight(1f))
                FeatureCard(item = featureItems[1], modifier = Modifier.weight(1f))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                FeatureCard(item = featureItems[2], modifier = Modifier.weight(1f))
                FeatureCard(item = featureItems[3], modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun FeatureCard(item: FeatureItem, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.Start
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(12.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = item.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = item.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

// ─── Slide 3: Get Started ──────────────────────────────────────────────────────

@Composable
private fun GetStartedSlide(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF10B981).copy(alpha = 0.25f),
                            Color.Transparent
                        )
                    ),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PlayArrow,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.size(60.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Let's Get Started",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Sign in to add your first device and begin automating your home.",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.65f),
            textAlign = TextAlign.Center
        )
    }
}

// ─── Dots Indicator ────────────────────────────────────────────────────────────

@Composable
private fun DotsIndicator(
    totalDots: Int,
    currentIndex: Int,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(totalDots) { index ->
            val isSelected = index == currentIndex
            val dotWidth by animateDpAsState(
                targetValue = if (isSelected) 24.dp else 8.dp,
                label = "dot_width_$index"
            )
            Box(
                modifier = Modifier
                    .height(8.dp)
                    .width(dotWidth)
                    .clip(CircleShape)
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline
                    )
            )
        }
    }
}

// ─── Previews ──────────────────────────────────────────────────────────────────

@Preview(showBackground = true, backgroundColor = 0xFFF9FAFB, name = "Welcome Light")
@Composable
fun OnboardingWelcomeLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        androidx.compose.material3.Surface(
            color = LightColorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            WelcomeSlide()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19, name = "Welcome Dark")
@Composable
fun OnboardingWelcomeDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        androidx.compose.material3.Surface(
            color = DarkColorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            WelcomeSlide()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF9FAFB, name = "Features Light")
@Composable
fun OnboardingFeaturesLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        androidx.compose.material3.Surface(
            color = LightColorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            FeaturesSlide()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19, name = "Features Dark")
@Composable
fun OnboardingFeaturesDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        androidx.compose.material3.Surface(
            color = DarkColorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            FeaturesSlide()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF9FAFB, name = "GetStarted Light")
@Composable
fun OnboardingGetStartedLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        androidx.compose.material3.Surface(
            color = LightColorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            GetStartedSlide()
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19, name = "GetStarted Dark")
@Composable
fun OnboardingGetStartedDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        androidx.compose.material3.Surface(
            color = DarkColorScheme.background,
            modifier = Modifier.fillMaxSize()
        ) {
            GetStartedSlide()
        }
    }
}
