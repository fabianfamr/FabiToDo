package com.fabian.todolist.ui;

/**
 * Enlace compatible con Appicons.java.
 * Todas las definiciones centrales residen en Appicons.java.
 */
public final class AppIcons {

    private AppIcons() {}

    public static final int DRAWABLE_SVG_ADD = Appicons.ICON_ADD;
    public static final int DRAWABLE_SVG_CHECK = Appicons.ICON_CHECK;
    public static final int DRAWABLE_SVG_CALENDAR = Appicons.ICON_CALENDAR;
    public static final int DRAWABLE_SVG_CLOCK = Appicons.ICON_CLOCK;
    public static final int DRAWABLE_SVG_BELL = Appicons.ICON_BELL;
    public static final int DRAWABLE_SVG_TRASH = Appicons.ICON_TRASH;
    public static final int DRAWABLE_SVG_SETTINGS = Appicons.ICON_SETTINGS;
    public static final int DRAWABLE_SVG_TAG = Appicons.ICON_TAG;
    public static final int DRAWABLE_SVG_SEARCH = Appicons.ICON_SEARCH;
    public static final int DRAWABLE_SVG_EDIT = Appicons.ICON_EDIT;
    public static final int DRAWABLE_SVG_REPEAT = Appicons.ICON_REPEAT;

    public static final int DRAWABLE_SVG_FLAG_LOW = Appicons.ICON_FLAG_LOW;
    public static final int DRAWABLE_SVG_FLAG_MEDIUM = Appicons.ICON_FLAG_MEDIUM;
    public static final int DRAWABLE_SVG_FLAG_HIGH = Appicons.ICON_FLAG_HIGH;
    public static final int DRAWABLE_SVG_FLAG_URGENT = Appicons.ICON_FLAG_URGENT;

    public static final long COLOR_CRITICAL = Appicons.COLOR_CRITICAL;
    public static final long COLOR_HIGH = Appicons.COLOR_HIGH;
    public static final long COLOR_MEDIUM = Appicons.COLOR_MEDIUM;
    public static final long COLOR_LOW = Appicons.COLOR_LOW;
    public static final long COLOR_NONE = Appicons.COLOR_NONE;

    public static int getPriorityDrawable(String priority) {
        return Appicons.getPriorityDrawable(priority);
    }

    public static int getPriorityLabelRes(String priority) {
        return Appicons.getPriorityLabelRes(priority);
    }

    public static long getPriorityColor(String priority) {
        return Appicons.getPriorityColor(priority);
    }
}
