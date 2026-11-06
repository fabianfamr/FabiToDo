package com.fabian.todolist.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import com.fabian.todolist.R
import com.fabian.todolist.ui.components.settings.SettingsSubmenuScaffold

data class AppContributor(
    val id: String,
    val displayName: String,
    val role: String,
    val detail: String? = null,
    val badge: String? = null,
    val iconVector: ImageVector = Icons.Rounded.Person,
    val url: String? = null
)

private val OwnerAndDeveloper = AppContributor(
    id = "fabian_owner",
    displayName = "Fabián",
    role = "Dueño y Desarrollador Principal",
    detail = "Creador, propietario exclusivo y arquitecto de la aplicación FabiToDo.",
    badge = "Dueño y Creador",
    iconVector = Icons.Rounded.Code,
    url = "https://github.com/fabianfamr"
)

private val FutureContributorsPlaceholder = AppContributor(
    id = "future_contributors",
    displayName = "Futuros Colaboradores",
    role = "Comunidad & Código Abierto",
    detail = "Espacio preparado para integrar y reconocer a los desarrolladores que colaboren en el futuro.",
    badge = "Abierto a Colaborar",
    iconVector = Icons.Rounded.GroupAdd,
    url = "https://github.com/fabianfamr"
)

private val AppFeaturesList = listOf(
    AppContributor(
        id = "cloud_sync",
        displayName = "Sincronización en la Nube",
        role = "Google Cloud Firestore",
        detail = "Copia de seguridad en tiempo real y sincronización en múltiples dispositivos.",
        badge = "Firestore",
        iconVector = Icons.Rounded.CloudSync
    ),
    AppContributor(
        id = "ai_gemini",
        displayName = "Inteligencia Artificial Gemini",
        role = "Firebase AI Logic",
        detail = "Desglose automático de tareas en pasos estructurados e intuitivos.",
        badge = "IA Gemini",
        iconVector = Icons.Rounded.AutoAwesome
    ),
    AppContributor(
        id = "m3_design",
        displayName = "Material Design 3",
        role = "Jetpack Compose Expressive",
        detail = "Interfaz adaptable con colores dinámicos, modo oscuro y animaciones fluidas.",
        badge = "Material 3",
        iconVector = Icons.Rounded.Palette
    )
)

@Composable
fun AboutScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val versionName: String = remember {
        try {
            val packageInfo = context.packageManager.getPackageInfo(context.packageName, 0)
            packageInfo.versionName ?: com.fabian.todolist.BuildConfig.VERSION_NAME
        } catch (_: Exception) {
            com.fabian.todolist.BuildConfig.VERSION_NAME
        }
    }

    val lazyListState = rememberLazyListState()

    SettingsSubmenuScaffold(
        title = stringResource(R.string.about_screen_title),
        onBack = onBackClick,
        modifier = modifier
    ) {
        LazyColumn(
            state = lazyListState,
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Hero Banner Card
            item(key = "about_hero") {
                AboutHeroCard(
                    versionName = versionName,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Owner & Lead Developer Header & Card
            item(key = "maintainer_header") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_maintainer_title),
                    subtitle = stringResource(R.string.about_maintainer_subtitle)
                )
            }

            item(key = "maintainer_card") {
                ContributorCard(
                    contributor = OwnerAndDeveloper,
                    modifier = Modifier.fillMaxWidth(),
                    onCardClick = OwnerAndDeveloper.url?.let { url -> { openUrl(context, url) } }
                )
            }

            // Future Contributors Section
            item(key = "future_contributors_header") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_contributors_section_title),
                    subtitle = stringResource(R.string.about_contributors_section_subtitle)
                )
            }

            item(key = "future_contributors_card") {
                ContributorCard(
                    contributor = FutureContributorsPlaceholder,
                    modifier = Modifier.fillMaxWidth(),
                    onCardClick = FutureContributorsPlaceholder.url?.let { url -> { openUrl(context, url) } }
                )
            }

            // Features Header & Items
            item(key = "features_header") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_spotlight_title),
                    subtitle = stringResource(R.string.about_spotlight_subtitle)
                )
            }

            itemsIndexed(
                items = AppFeaturesList,
                key = { _, feature -> "feature_${feature.id}" }
            ) { _, feature ->
                ContributorCard(
                    contributor = feature,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Open Source Licenses Header & Card
            item(key = "licenses_header") {
                AboutSectionHeader(
                    title = stringResource(R.string.about_open_source_licenses),
                    subtitle = stringResource(R.string.about_open_source_licenses_subtitle)
                )
            }

            item(key = "licenses_card") {
                OpenSourceLicensesCard(
                    modifier = Modifier.fillMaxWidth()
                )
            }

            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
private fun AboutHeroCard(
    versionName: String,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(26.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f),
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.size(56.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "FabiToDo",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.settings_app_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.tertiaryContainer,
                modifier = Modifier.pointerInput(Unit) {
                    detectTapGestures(
                        onLongPress = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        }
                    )
                }
            ) {
                Text(
                    text = stringResource(R.string.about_version_format, versionName),
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            CommunitySignalsRow()
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CommunitySignalsRow() {
    val signals = listOf(
        "100% Android Native" to Icons.Rounded.Android,
        "Jetpack Compose" to Icons.Rounded.Code,
        "Google Cloud" to Icons.Rounded.Cloud
    )

    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        signals.forEach { (label, icon) ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun AboutSectionHeader(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ContributorCard(
    contributor: AppContributor,
    modifier: Modifier = Modifier,
    onCardClick: (() -> Unit)? = null
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .then(if (onCardClick != null) Modifier.clickable { onCardClick() } else Modifier),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = contributor.iconVector,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contributor.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = contributor.role,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                contributor.detail?.let { detail ->
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }

            contributor.badge?.let { badge ->
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.secondaryContainer
                ) {
                    Text(
                        text = badge,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
private fun OpenSourceLicensesCard(
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.secondaryContainer,
                modifier = Modifier.size(44.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Rounded.Gavel,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.about_open_source_licenses),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.about_open_source_licenses_subtitle),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private fun openUrl(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, url.toUri()).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: ActivityNotFoundException) {
    }
}
