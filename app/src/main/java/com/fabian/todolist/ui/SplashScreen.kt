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

    // Floating bob animation
    val infiniteTransition = rememberInfiniteTransition(label = "splash_infinite")
    val bobOffset by infiniteTransition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )

    // Pulsing outer aura ring
    val auraPulse by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "aura"
    )

    // Sparkle rotation
    val sparkleRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkle_rotation"
    )

    val currentFloatY = bobOffset

    LaunchedEffect(Unit) {
        introProgress.animateTo(
            targetValue = 1f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioLowBouncy,
                stiffness = Spring.StiffnessLow
            )
        )

        delay(250)
        checkmarkDrawProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = 950,
                easing = FastOutSlowInEasing
            )
        )

        textAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(600, easing = EaseOutCubic)
        )
        textOffsetY.animateTo(
            targetValue = 0f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessMediumLow
            )
        )

        delay(120)
        subtitleAlpha.animateTo(
            targetValue = 1f,
            animationSpec = tween(500, easing = LinearOutSlowInEasing)
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        primaryColor,
                        primaryColor.copy(alpha = 0.92f)
                    )
                )
            )
            .graphicsLayer {
                alpha = (1f - t).coerceIn(0f, 1f)
                scaleX = lerp(1f, 1.05f, t)
                scaleY = lerp(1f, 1.05f, t)
            },
        contentAlignment = Alignment.Center
    ) {
        // Floating Ambient Sparkles background canvas
        Canvas(modifier = Modifier.fillMaxSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            
            // Draw floating ambient starlets
            rotate(sparkleRotation, pivot = androidx.compose.ui.geometry.Offset(cx, cy)) {
                val r1 = 140.dp.toPx()
                val r2 = 200.dp.toPx()
                val r3 = 260.dp.toPx()
                val radii = listOf(r1, r2, r3)
                val angles = listOf(30f, 120f, 210f, 300f, 75f, 255f)
                angles.forEachIndexed { idx, angle ->
                    val r = radii[idx % radii.size]
                    val rad = Math.toRadians(angle.toDouble())
                    val x = cx + (r * kotlin.math.cos(rad)).toFloat()
                    val y = cy + (r * kotlin.math.sin(rad)).toFloat()
                    val starRadius = (3 + (idx % 3) * 2).dp.toPx()
                    drawCircle(
                        color = Color.White.copy(alpha = 0.25f),
                        radius = starRadius,
                        center = androidx.compose.ui.geometry.Offset(x, y)
                    )
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(170.dp)
                    .graphicsLayer {
                        translationY = currentFloatY * density
                        scaleX = introProgress.value
                        scaleY = introProgress.value
                    }
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height

                    // Pulsing Outer Glowing Ring
                    drawCircle(
                        color = Color.White.copy(alpha = 0.12f * (2f - auraPulse)),
                        radius = (w / 2f) * auraPulse
                    )

                    // Solid Inner Circle
                    drawCircle(
                        color = Color.White.copy(alpha = 0.22f),
                        radius = w / 2f
                    )

                    // Border Accent
                    drawCircle(
                        color = Color.White.copy(alpha = 0.4f),
                        radius = w / 2f,
                        style = Stroke(width = 3.dp.toPx())
                    )

                    // Draw progressive checkmark logic
                    val p = checkmarkDrawProgress.value
                    val p0x = w * 0.33f
                    val p0y = h * 0.54f

                    val p1x = w * 0.47f
                    val p1y = h * 0.67f

                    val p2x = w * 0.73f
                    val p2y = h * 0.33f

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
                                width = 18f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round
                            )
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(36.dp))

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
                        fontSize = 42.sp,
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
                    color = Color.White.copy(alpha = subtitleAlpha.value * 0.85f)
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
