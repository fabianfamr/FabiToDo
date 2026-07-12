package com.fabian.todolist.ui

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import androidx.compose.ui.res.stringResource
import com.fabian.todolist.R

@Stable
class PixelTransitionState {
    val progress = Animatable(0f)
    val running = mutableStateOf(true)

    suspend fun start() {
        // Optimizing launch speed: reduce delay to 1100ms so checkmark draws beautifully and transitions snappily
        delay(1100)
        progress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 500,
                easing = CubicBezierEasing(0.25f, 1f, 0.5f, 1f) // Ultra-smooth deceleration
            )
        )
        delay(50) // Settle frame transition buffer
        running.value = false
    }
}

fun lerp(start: Float, end: Float, t: Float): Float {
    return start + (end - start) * t
}

@Composable
fun PixelSplashScene(t: Float) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val tertiaryColor = MaterialTheme.colorScheme.tertiary

    // Internal entry animations
    val introProgress = remember { Animatable(0f) }
    val checkmarkDrawProgress = remember { Animatable(0f) }

    // Staggered text fade/offset
    val textAlpha = remember { Animatable(0f) }
    val textOffsetY = remember { Animatable(30f) }
    val subtitleAlpha = remember { Animatable(0f) }

    // Infinite gentle hovering / floating offset animation
    // Adjusted to be subtle (minimal) and continuous from the start of the checkmark drawing
    val infiniteTransition = rememberInfiniteTransition(label = "floating_bob")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -5f, // Minimal, very subtle range
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutQuad), // Slower, softer floating pacing
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    val currentFloatY = bobOffset

    LaunchedEffect(Unit) {
        // Smoothly scale & spring the icon container
        introProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        // Draw checkmark path progressively after a slight spring settle keyframe
        delay(300)
        checkmarkDrawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 1000,
                easing = FastOutSlowInEasing
            )
        )

        // Bring up FabiToDo branding text
        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(700, easing = EaseOutCubic)
        )
        textOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        // Smooth tagline delay transition
        delay(150)
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = LinearOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(primaryColor)
            .graphicsLayer {
                alpha = (1f - t).coerceIn(0f, 1f)
                scaleX = lerp(1f, 1.05f, t)
                scaleY = lerp(1f, 1.05f, t)
            },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(160.dp) // Adjusted for more elegant proportions
                    .graphicsLayer {
                        translationY = currentFloatY * density
                    }
            ) {
                // Core branding icon checkmark container styled beautifully
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Background circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.2f),
                        radius = w / 2f
                    )

                    // Mathematically animate draw progressive checkmark logic
                    val p = checkmarkDrawProgress.value
                    val p0x = w * 0.34f
                    val p0y = h * 0.54f

                    val p1x = w * 0.48f
                    val p1y = h * 0.66f

                    val p2x = w * 0.74f
                    val p2y = h * 0.32f

                    if (p > 0f) {
                        val currentPath = Path().apply {
                            moveTo(p0x, p0y)
                            if (p <= 0.4f) {
                                val ratio = p / 0.4f
                                val curX = lerp(p0x, p1x, ratio)
                                val curY = lerp(p0y, p1y, ratio)
                                lineTo(curX, curY)
                            } else {
                                lineTo(p1x, p1y)
                                val ratio = (p - 0.4f) / 0.6f
                                val curX = lerp(p1x, p2x, ratio)
                                val curY = lerp(p1y, p2y, ratio)
                                lineTo(curX, curY)
                            }
                        }

                        drawPath(
                            path = currentPath,
                            color = Color.White,
                            style = Stroke(
                                width = 16f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Text Title block staggered slide on emergence
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.graphicsLayer {
                    alpha = textAlpha.value
                    translationY = textOffsetY.value
                }
            ) {
                Text(
                    text = "FabiToDo",
                    style = MaterialTheme.typography.displayMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 40.sp,
                        letterSpacing = (-0.5).sp
                    ),
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = stringResource(R.string.splash_subtitle),
                    style = MaterialTheme.typography.titleMedium.copy(
                        letterSpacing = 0.5.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = Color.White.copy(alpha = subtitleAlpha.value * 0.8f)
                )
            }
        }
    }
}

@Composable
fun PixelMorphBackground(t: Float) {
    val targetColor = MaterialTheme.colorScheme.background

    Canvas(Modifier.fillMaxSize()) {
        val maxRadius = size.maxDimension * 1.414f
        val radius = lerp(0f, maxRadius, t)
        drawCircle(
            color = targetColor,
            radius = radius
        )
    }
}

@Composable
fun SplashScreen(onTimeout: () -> Unit) {
    // This file keeps compatibility with old architectural imports if referenced elsewhere.
}
