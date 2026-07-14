package com.smartnexus.nexusflow.features.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.foundation.isSystemInDarkTheme
import com.smartnexus.nexusflow.R
import com.smartnexus.nexusflow.core.theme.NexusFlowTheme
import com.smartnexus.nexusflow.core.theme.DarkColorScheme
import com.smartnexus.nexusflow.core.theme.LightColorScheme

import com.smartnexus.nexusflow.core.navigation.Screen

@Composable
fun SplashScreen(
    onNavigateForward: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SplashViewModel = hiltViewModel()
) {
    // Collect navigation event and forward
    LaunchedEffect(Unit) {
        viewModel.navigationEvent.collect { target ->
            onNavigateForward(target)
        }
    }

    SplashContent(modifier = modifier)
}

@Composable
private fun SplashContent(modifier: Modifier = Modifier) {
    // Icon entrance animation
    val scale = remember { Animatable(0.7f) }
    val alpha = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = FastOutSlowInEasing)
        )
    }
    LaunchedEffect(Unit) {
        alpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500)
        )
    }

    // Detect theme by checking brightness of background color
    val bgColor = MaterialTheme.colorScheme.background
    val brightness = (bgColor.red * 299 + bgColor.green * 587 + bgColor.blue * 114) / 1000
    val isDarkMode = brightness < 0.5f
    
    val bgGradientStart = if (isDarkMode) Color(0xFF1E2A4A) else Color(0xFFE0E7FF)
    val bgGradientEnd = if (isDarkMode) Color(0xFF0B0F19) else Color(0xFFF9FAFB)
    val nexusColor = if (isDarkMode) Color.White else Color(0xFF111827)
    val flowColor = MaterialTheme.colorScheme.primary
    val taglineColor = if (isDarkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primary
    val textColor = if (isDarkMode) Color.White.copy(alpha = 0.7f) else Color(0xFF374151)
    val accentColor = if (isDarkMode) Color(0xFF38BDF8) else MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                brush = Brush.radialGradient(
                    colors = listOf(bgGradientStart, bgGradientEnd),
                    radius = 900f
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            
            // App icon with background fix for transparency/checkerboard
            Image(
                painter = painterResource(
                    id = if (isDarkMode) R.drawable.logo_nexusflow_dark else R.drawable.logo_nexusflow_light
                ),
                contentDescription = "NexusFlow icon",
                modifier = Modifier
                    .size(240.dp)
                    .scale(scale.value)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // App name with glowing styled split color (NexusFlow)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Nexus",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = nexusColor
                )
                Text(
                    text = "Flow",
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 32.sp
                    ),
                    color = flowColor
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Tagline text with subtle cyan glow
            Text(
                text = "Smart Home Automation",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Normal
                ),
                color = taglineColor
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Glowing linear progress bar
            Box(
                modifier = Modifier
                    .width(80.dp)
                    .height(3.dp)
                    .background(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                accentColor,
                                flowColor,
                                accentColor
                            )
                        ),
                        shape = androidx.compose.foundation.shape.RoundedCornerShape(1.5.dp)
                    )
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Multi-line status prompt texts
            Text(
                text = "Connecting your world.",
                style = MaterialTheme.typography.bodyLarge,
                color = textColor,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Intelligently.",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = taglineColor,
                textAlign = TextAlign.Center
            )
        }

        // Curved structure at the bottom
        CurvedStructureGraphic(
            modifier = Modifier.align(Alignment.BottomCenter),
            isDarkMode = isDarkMode
        )
    }
}

@Composable
private fun CurvedStructureGraphic(modifier: Modifier = Modifier, isDarkMode: Boolean = true) {
    val curveColor = if (isDarkMode) Color(0xFF0EA5E9) else Color(0xFF4F46E5)
    androidx.compose.foundation.Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
    ) {
        val width = size.width
        val height = size.height

        val path = androidx.compose.ui.graphics.Path()
        // Draw multiple nested sine wave curve lines
        for (i in 0..3) {
            val offset = i * 15f
            path.reset()
            path.moveTo(0f, height)
            path.cubicTo(
                width * 0.25f, height - 20f - offset,
                width * 0.75f, height - 70f + offset,
                width, height
            )
            drawPath(
                path = path,
                color = curveColor.copy(alpha = 0.15f - (i * 0.03f)),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFF0B0F19)
@Composable
fun SplashScreenDarkPreview() {
    NexusFlowTheme(darkTheme = true) {
        SplashContent()
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFAFAFA)
@Composable
fun SplashScreenLightPreview() {
    NexusFlowTheme(darkTheme = false) {
        SplashContent()
    }
}
