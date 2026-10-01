package com.fabian.todolist.ui;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.fabian.todolist.R;
import com.fabian.todolist.data.TaskPriority;

/**
 * Centralized Icon & Asset Repository for FabiToDo (AppIcons.java).
 * Centralizes all SVG-based vector drawables (sourced from SVGRepo / https://www.svgrepo.com)
 * and priority styles for optimal visual fidelity and performance.
 */
public final class AppIcons {

    private AppIcons() {
        // Private constructor for static utility class
    }

    // Core Action Icons (SVGRepo Clean Vectors)
    @DrawableRes public static final int DRAWABLE_SVG_ADD = R.drawable.ic_svg_add;
    @DrawableRes public static final int DRAWABLE_SVG_CHECK = R.drawable.ic_svg_check;
    @DrawableRes public static final int DRAWABLE_SVG_CALENDAR = R.drawable.ic_svg_calendar;
    @DrawableRes public static final int DRAWABLE_SVG_CLOCK = R.drawable.ic_svg_clock;
    @DrawableRes public static final int DRAWABLE_SVG_BELL = R.drawable.ic_svg_bell;
    @DrawableRes public static final int DRAWABLE_SVG_TRASH = R.drawable.ic_svg_trash;
    @DrawableRes public static final int DRAWABLE_SVG_SETTINGS = R.drawable.ic_svg_settings;
    @DrawableRes public static final int DRAWABLE_SVG_TAG = R.drawable.ic_svg_tag;
    @DrawableRes public static final int DRAWABLE_SVG_SEARCH = R.drawable.ic_svg_search;
    @DrawableRes public static final int DRAWABLE_SVG_EDIT = R.drawable.ic_svg_edit;
    @DrawableRes public static final int DRAWABLE_SVG_REPEAT = R.drawable.ic_svg_repeat;

    // Priority Flag Drawables (SVGRepo)
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_LOW = R.drawable.ic_svg_flag_low;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_MEDIUM = R.drawable.ic_svg_flag_medium;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_HIGH = R.drawable.ic_svg_flag_high;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_URGENT = R.drawable.ic_svg_flag_urgent;

    // Backward-compatible aliases
    @DrawableRes public static final int DRAWABLE_LOW_PRIORITY = DRAWABLE_SVG_FLAG_LOW;
    @DrawableRes public static final int DRAWABLE_MEDIUM_PRIORITY = DRAWABLE_SVG_FLAG_MEDIUM;
    @DrawableRes public static final int DRAWABLE_HIGH_PRIORITY = DRAWABLE_SVG_FLAG_HIGH;
    @DrawableRes public static final int DRAWABLE_URGENT_PRIORITY = DRAWABLE_SVG_FLAG_URGENT;
    @DrawableRes public static final int DRAWABLE_TASK_ADD = DRAWABLE_SVG_ADD;
    @DrawableRes public static final int DRAWABLE_TASK_CHECK = DRAWABLE_SVG_CHECK;
    @DrawableRes public static final int DRAWABLE_NOTIFICATION = DRAWABLE_SVG_BELL;
    @DrawableRes public static final int DRAWABLE_CALENDAR = DRAWABLE_SVG_CALENDAR;

    // Color ARGB definitions
    public static final long COLOR_CRITICAL = 0xFFB3261EL;
    public static final long COLOR_HIGH = 0xFFEA4335L;
    public static final long COLOR_MEDIUM = 0xFFFBBC05L;
    public static final long COLOR_LOW = 0xFF34A853L;
    public static final long COLOR_NONE = 0xFF808080L;

    /**
     * Resolves the SVGRepo drawable resource ID for a given task priority.
     */
    @DrawableRes
    public static int getPriorityDrawable(String priority) {
        if (priority == null) return DRAWABLE_SVG_FLAG_LOW;
        switch (priority) {
            case TaskPriority.CRITICAL:
                return DRAWABLE_SVG_FLAG_URGENT;
            case TaskPriority.HIGH:
                return DRAWABLE_SVG_FLAG_HIGH;
            case TaskPriority.MEDIUM:
                return DRAWABLE_SVG_FLAG_MEDIUM;
            case TaskPriority.LOW:
            default:
                return DRAWABLE_SVG_FLAG_LOW;
        }
    }

    /**
     * Resolves the localized string resource ID for a given task priority.
     */
    @StringRes
    public static int getPriorityLabelRes(String priority) {
        if (priority == null) return R.string.priority_none;
        switch (priority) {
            case TaskPriority.CRITICAL:
                return R.string.priority_urgent;
            case TaskPriority.HIGH:
                return R.string.priority_high;
            case TaskPriority.MEDIUM:
                return R.string.priority_medium;
            case TaskPriority.LOW:
                return R.string.priority_low;
            default:
                return R.string.priority_none;
        }
    }

    /**
     * Resolves the ARGB color value for a given task priority.
     */
    public static long getPriorityColor(String priority) {
        if (priority == null) return COLOR_NONE;
        switch (priority) {
            case TaskPriority.CRITICAL:
                return COLOR_CRITICAL;
            case TaskPriority.HIGH:
                return COLOR_HIGH;
            case TaskPriority.MEDIUM:
                return COLOR_MEDIUM;
            case TaskPriority.LOW:
                return COLOR_LOW;
            default:
                return COLOR_NONE;
        }
    }
}
