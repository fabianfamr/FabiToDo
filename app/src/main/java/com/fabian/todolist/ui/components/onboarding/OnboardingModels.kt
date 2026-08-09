package com.fabian.todolist.ui.components.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.fabian.todolist.R

data class OnboardingUserProfile(
    val displayName: String?,
    val email: String?,
    val photoUrl: String?
)

data class LanguageOption(
    val code: String,
    val name: String,
    val flagEmoji: String
)

data class AccentOption(
    val id: String,
    val nameKey: String,
    val colorHex: Long
)

/**
 * Helper function to retrieve localized string resources for the onboarding flow.
 */
@Composable
fun onboardingString(key: String, lang: String = "system"): String {
    return when (key) {
        "welcome_title" -> stringResource(R.string.onboarding_welcome_title)
        "welcome_desc" -> stringResource(R.string.onboarding_welcome_desc)
        "account_title" -> stringResource(R.string.onboarding_account_title)
        "account_desc" -> stringResource(R.string.onboarding_account_desc)
        "connected_google" -> stringResource(R.string.onboarding_connected_google)
        "guest_btn" -> stringResource(R.string.onboarding_guest_btn)
        "google_btn" -> stringResource(R.string.onboarding_google_btn)
        "back_btn" -> stringResource(R.string.onboarding_back_btn)
        "language_title" -> stringResource(R.string.onboarding_language_title)
        "language_desc" -> stringResource(R.string.onboarding_language_desc)
        "appearance_title" -> stringResource(R.string.onboarding_appearance_title)
        "appearance_desc" -> stringResource(R.string.onboarding_appearance_desc)
        "notifications_title" -> stringResource(R.string.onboarding_notifications_title)
        "notifications_desc" -> stringResource(R.string.onboarding_notifications_desc)
        "habits_title" -> stringResource(R.string.onboarding_habits_title)
        "habits_desc" -> stringResource(R.string.onboarding_habits_desc)
        "ai_title" -> stringResource(R.string.onboarding_ai_title)
        "ai_desc" -> stringResource(R.string.onboarding_ai_desc)
        "name_question" -> stringResource(R.string.onboarding_name_question)
        "name_placeholder" -> stringResource(R.string.onboarding_name_placeholder)
        "app_language_title" -> stringResource(R.string.onboarding_app_language_title)
        "system_default" -> stringResource(R.string.onboarding_system_default)
        "notifs_enabled_btn" -> stringResource(R.string.onboarding_notifs_enabled_btn)
        "appearance_mode_title" -> stringResource(R.string.onboarding_appearance_mode_title)
        "color_accent_title" -> stringResource(R.string.onboarding_color_accent_title)
        "next" -> stringResource(R.string.onboarding_next)
        "start" -> stringResource(R.string.onboarding_start)
        "visual_title" -> stringResource(R.string.onboarding_visual_title)
        "visual_desc" -> stringResource(R.string.onboarding_visual_desc)
        "theme_mode" -> stringResource(R.string.onboarding_theme_mode)
        "theme_system" -> stringResource(R.string.onboarding_theme_system)
        "theme_light" -> stringResource(R.string.onboarding_theme_light)
        "theme_dark" -> stringResource(R.string.onboarding_theme_dark)
        "color_accent" -> stringResource(R.string.onboarding_color_accent)
        "color_system" -> stringResource(R.string.onboarding_color_system)
        "color_blue" -> stringResource(R.string.onboarding_color_blue)
        "color_sunset" -> stringResource(R.string.onboarding_color_sunset)
        "color_green" -> stringResource(R.string.onboarding_color_green)
        "color_rose" -> stringResource(R.string.onboarding_color_rose)
        "color_amber" -> stringResource(R.string.onboarding_color_amber)
        "notifs_title" -> stringResource(R.string.onboarding_notifs_title)
        "notifs_desc" -> stringResource(R.string.onboarding_notifs_desc)
        "notifs_banner_title" -> stringResource(R.string.onboarding_notifs_banner_title)
        "notifs_banner_desc" -> stringResource(R.string.onboarding_notifs_banner_desc)
        "notifs_grant_btn" -> stringResource(R.string.onboarding_notifs_grant_btn)
        "notifs_customize" -> stringResource(R.string.onboarding_notifs_customize)
        "sound_title" -> stringResource(R.string.onboarding_sound_title)
        "sound_desc" -> stringResource(R.string.onboarding_sound_desc)
        "vibrate_title" -> stringResource(R.string.onboarding_vibrate_title)
        "vibrate_desc" -> stringResource(R.string.onboarding_vibrate_desc)
        "quick_reminders_title" -> stringResource(R.string.onboarding_quick_reminders_title)
        "quick_reminders_desc" -> stringResource(R.string.onboarding_quick_reminders_desc)
        "interval_off" -> stringResource(R.string.onboarding_interval_off)
        "security_title" -> stringResource(R.string.onboarding_security_title)
        "confirm_delete_title" -> stringResource(R.string.onboarding_confirm_delete_title)
        "confirm_delete_desc" -> stringResource(R.string.onboarding_confirm_delete_desc)
        "ai_model_title" -> stringResource(R.string.onboarding_ai_model_title)
        "model_flash" -> stringResource(R.string.onboarding_model_flash)
        "model_lite" -> stringResource(R.string.onboarding_model_lite)
        "ai_complexity_title" -> stringResource(R.string.onboarding_ai_complexity_title)
        "ai_complexity_desc" -> stringResource(R.string.onboarding_ai_complexity_desc)
        "subtasks_count_label" -> stringResource(R.string.onboarding_subtasks_count_label)
        "of_word" -> stringResource(R.string.onboarding_of_word)
        "all_set" -> stringResource(R.string.onboarding_all_set)
        "next_step" -> stringResource(R.string.onboarding_next_step)
        "start_excl" -> stringResource(R.string.onboarding_start_excl)
        "steps_label" -> stringResource(R.string.onboarding_steps_label)
        "step_welcome" -> stringResource(R.string.onboarding_step_welcome)
        "step_account" -> stringResource(R.string.onboarding_step_account)
        "step_lang" -> stringResource(R.string.onboarding_step_lang)
        "step_visual" -> stringResource(R.string.onboarding_step_visual)
        "step_alerts" -> stringResource(R.string.onboarding_step_alerts)
        "step_habits" -> stringResource(R.string.onboarding_step_habits)
        "step_ai" -> stringResource(R.string.onboarding_step_ai)
        "step_time" -> stringResource(R.string.onboarding_step_time)
        "step_quickadd" -> stringResource(R.string.onboarding_step_quickadd)
        "step_security" -> stringResource(R.string.onboarding_step_security)
        "step_privacy" -> stringResource(R.string.onboarding_step_privacy)
        "step_finish" -> stringResource(R.string.onboarding_step_finish)
        else -> key
    }
}
