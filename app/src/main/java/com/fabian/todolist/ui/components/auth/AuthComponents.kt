package com.fabian.todolist.ui.components.auth

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R

object LoginDimens {
    val ButtonHeight = 60.dp
    val ButtonShape = RoundedCornerShape(30.dp)
}

@Composable
fun AmbientBackground() {
    val infiniteTransition = rememberInfiniteTransition(label = "ambient_aurora")
    val animOffset1 by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(20000, easing = EaseInOutSine), RepeatMode.Reverse), label = "offset1"
    )
    val animOffset2 by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(18000, easing = EaseInOutSine), RepeatMode.Reverse), label = "offset2"
    )

    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val backgroundColor = MaterialTheme.colorScheme.background

    Canvas(modifier = Modifier.fillMaxSize().background(backgroundColor)) {
        val width = size.width
        val height = size.height

        // Premium ambient glows with extremely low opacity to keep them subtle, elegant, and atmospheric
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryColor.copy(alpha = 0.08f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(width * (0.15f + 0.7f * animOffset1), height * 0.25f),
                radius = width * 1.1f
            )
        )
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.06f), Color.Transparent),
                center = androidx.compose.ui.geometry.Offset(width * (0.85f - 0.6f * animOffset2), height * 0.75f),
                radius = width * 1.2f
            )
        )

        // Ultra-elegant designer dot-matrix grid overlay matching high-end premium software interfaces
        val dotSpacing = 28.dp.toPx()
        val dotRadius = 1.dp.toPx()
        val dotColor = primaryColor.copy(alpha = 0.035f)
        for (x in 0..width.toInt() step dotSpacing.toInt()) {
            for (y in 0..height.toInt() step dotSpacing.toInt()) {
                drawCircle(
                    color = dotColor,
                    radius = dotRadius,
                    center = androidx.compose.ui.geometry.Offset(x.toFloat(), y.toFloat())
                )
            }
        }
    }
}

@Composable
fun LogoSegment(isLoading: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "cards_float")
    
    // Smooth floating animation for the entire stack
    val floatAnim by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "stack_float"
    )
    
    // Rotation sweep for adding alive realism
    val rotateAnim by infiniteTransition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(4500, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cards_rotate"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .offset(y = floatAnim.dp)
    ) {
        // --- 1. REAR CARD (Flipped left) ---
        Card(
            modifier = Modifier
                .width(180.dp)
                .height(95.dp)
                .offset(x = (-35).dp, y = (-25).dp)
                .rotate(-12f + rotateAnim)
                .scale(0.88f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f)),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Box(modifier = Modifier.width(65.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(30.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.12f)))
                }
            }
        }

        // --- 2. MIDDLE CARD (Flipped right) ---
        Card(
            modifier = Modifier
                .width(190.dp)
                .height(100.dp)
                .offset(x = 35.dp, y = (-12).dp)
                .rotate(8f - rotateAnim)
                .scale(0.93f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.35f)
            ),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
            shape = RoundedCornerShape(20.dp)
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Box(modifier = Modifier.width(75.dp).height(8.dp).clip(RoundedCornerShape(4.dp)).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.35f)))
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(modifier = Modifier.width(40.dp).height(6.dp).clip(RoundedCornerShape(3.dp)).background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.18f)))
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.Bottom)
                        .padding(bottom = 2.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = stringResource(R.string.today).uppercase(),
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // --- 3. MAIN FRONT CARD (Centered hero) ---
        Card(
            modifier = Modifier
                .width(235.dp)
                .height(115.dp)
                .offset(y = 15.dp)
                .rotate(-1.5f + (rotateAnim * 0.3f)),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(
                width = 1.5.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primary,
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                    )
                )
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp).fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary,
                                    MaterialTheme.colorScheme.secondary
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Task Done",
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
                
                Spacer(modifier = Modifier.width(12.dp))
                
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FabiToDo",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        letterSpacing = (-0.5).sp
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.splash_subtitle),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Star,
                        contentDescription = "Priority",
                        modifier = Modifier.size(13.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}

@Composable
fun LoginHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Text(
            text = buildAnnotatedString {
                withStyle(style = SpanStyle(
                    fontWeight = FontWeight.Light,
                    color = MaterialTheme.colorScheme.onBackground
                )) {
                    append("Fabi")
                }
                withStyle(style = SpanStyle(
                    fontWeight = FontWeight.Black,
                    color = MaterialTheme.colorScheme.primary
                )) {
                    append("ToDo")
                }
            },
            style = MaterialTheme.typography.displayLarge,
            letterSpacing = (-2).sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.login_subtitle).uppercase(),
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.secondary,
            letterSpacing = 2.sp,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = stringResource(R.string.login_desc_main),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            lineHeight = 26.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
    }
}

@Composable
fun LoginErrorCard(error: String) {
    Spacer(modifier = Modifier.height(16.dp))
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f)),
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Rounded.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = error,
                color = MaterialTheme.colorScheme.onErrorContainer,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun GoogleGIcon(modifier: Modifier = Modifier) {
    Icon(
        painter = painterResource(id = R.drawable.ic_google_logo),
        contentDescription = "Google Logo",
        tint = Color.Unspecified,
        modifier = modifier.size(24.dp)
    )
}

@Composable
fun LoginButtonSection(
    isLoading: Boolean,
    onGuestClick: () -> Unit,
    onGoogleClick: () -> Unit
) {
    val haptic = LocalHapticFeedback.current

    // PRIMARY GUEST BUTTON
    Button(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onGuestClick() 
        },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(LoginDimens.ButtonHeight),
        shape = LoginDimens.ButtonShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp, pressedElevation = 2.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.size(24.dp), strokeWidth = 2.5.dp)
        } else {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
                Text(
                    text = stringResource(R.string.login_guest_btn),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
            }
        }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // SLEEK OUTLINED GOOGLE BUTTON
    OutlinedButton(
        onClick = { 
            haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            onGoogleClick() 
        },
        enabled = !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(LoginDimens.ButtonHeight),
        shape = LoginDimens.ButtonShape,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp, pressedElevation = 0.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
            GoogleGIcon()
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = stringResource(R.string.login_google_btn),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp
            )
        }
    }
}

@Composable
fun LoginFooter() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.CloudSync,
            contentDescription = "Safe Sync",
            modifier = Modifier.size(20.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = stringResource(R.string.login_footer_text),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = TextAlign.Start,
            lineHeight = 16.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
fun GoogleLogoEmblem(rotation: Float) {
    // Retained for backward compatibility with GoogleLoginScreen
    val infiniteTransition = rememberInfiniteTransition(label = "google_emblem_anim")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f, targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(2000, easing = EaseInOutSine), RepeatMode.Reverse), label = "pulse_scale"
    )
    val waveScale by infiniteTransition.animateFloat(
        initialValue = 0.95f, targetValue = 1.3f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseOutSine), RepeatMode.Restart), label = "wave_scale"
    )
    val waveAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f, targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(3000, easing = EaseOutSine), RepeatMode.Restart), label = "wave_alpha"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(190.dp).scale(pulseScale)
    ) {
        Box(modifier = Modifier.fillMaxSize().scale(waveScale).border(1.5.dp, MaterialTheme.colorScheme.primary.copy(alpha = waveAlpha), CircleShape))
        Box(modifier = Modifier.size(160.dp).scale(1.12f).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.15f), CircleShape))
        Canvas(modifier = Modifier.size(140.dp).rotate(rotation)) {
            val stroke = Stroke(width = 5.dp.toPx(), cap = StrokeCap.Round)
            drawArc(Color(0xFF4285F4), 0f, 70f, false, style = stroke)
            drawArc(Color(0xFF34A853), 90f, 70f, false, style = stroke)
            drawArc(Color(0xFFFBBC05), 180f, 70f, false, style = stroke)
            drawArc(Color(0xFFEA4335), 270f, 70f, false, style = stroke)
        }
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.size(102.dp),
            tonalElevation = 6.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                Icon(painter = painterResource(id = R.drawable.ic_google_logo), contentDescription = "Google Logo", tint = Color.Unspecified, modifier = Modifier.size(52.dp))
            }
        }
    }
}
