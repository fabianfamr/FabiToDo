package com.fabian.todolist.ui.components.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.fabian.todolist.R

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun OnboardingHeader(
    currentStep: Int,
    totalSteps: Int,
    selectedLang: String,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val progress = currentStep.toFloat() / totalSteps.toFloat()
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "HeaderProgress"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top navigation bar (Back button & Step counter badge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            if (currentStep > 1) {
                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.nav_back),
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(38.dp))
            }

            // Step Counter Badge
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                )
            ) {
                Text(
                    text = "${onboardingString("steps_label", selectedLang)} $currentStep ${onboardingString("of_word", selectedLang)} $totalSteps",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Animated Progress Indicator
        LinearProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp)),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Animated Step Badge Icon with Pulsing Background Glow
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                (fadeIn(animationSpec = tween(220)) + scaleIn(initialScale = 0.8f)) togetherWith
                        (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 0.8f))
            },
            label = "StepIconAnimation"
        ) { step ->
            val icon = getStepIcon(step)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.25f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    )
                    .border(
                        width = 2.dp,
                        brush = Brush.sweepGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary,
                                MaterialTheme.colorScheme.secondary,
                                MaterialTheme.colorScheme.primary
                            )
                        ),
                        shape = CircleShape
                    )
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(34.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Dynamic Title & Description with Crossfade Transition
        AnimatedContent(
            targetState = currentStep,
            transitionSpec = {
                fadeIn(animationSpec = tween(250)) togetherWith fadeOut(animationSpec = tween(150))
            },
            label = "StepTitleTransition"
        ) { step ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = onboardingString(getStepTitleKey(step), selectedLang),
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = onboardingString(getStepDescKey(step), selectedLang),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

private fun getStepIcon(step: Int): ImageVector {
    return when (step) {
        1 -> Icons.Default.WavingHand
        2 -> Icons.Default.AccountCircle
        3 -> Icons.Default.Language
        4 -> Icons.Default.Palette
        5 -> Icons.Default.NotificationsActive
        6 -> Icons.Default.VolumeUp
        7 -> Icons.Default.Repeat
        8 -> Icons.Default.Psychology
        9 -> Icons.Default.Tune
        10 -> Icons.Default.Security
        11 -> Icons.Default.Lock
        12 -> Icons.Default.CheckCircle
        else -> Icons.Default.Check
    }
}

private fun getStepTitleKey(step: Int): String {
    return when (step) {
        1 -> "welcome_title"
        2 -> "account_title"
        3 -> "language_title"
        4 -> "visual_title"
        5 -> "notifs_title"
        6 -> "sound_title"
        7 -> "habits_title"
        8 -> "ai_title"
        9 -> "ai_complexity_title"
        10 -> "security_title"
        11 -> "security_title"
        12 -> "all_set"
        else -> "welcome_title"
    }
}

private fun getStepDescKey(step: Int): String {
    return when (step) {
        1 -> "welcome_desc"
        2 -> "account_desc"
        3 -> "language_desc"
        4 -> "visual_desc"
        5 -> "notifs_desc"
        6 -> "sound_desc"
        7 -> "habits_desc"
        8 -> "ai_desc"
        9 -> "ai_complexity_desc"
        10 -> "confirm_delete_desc"
        11 -> "security_title"
        12 -> "step_finish"
        else -> "welcome_desc"
    }
}
