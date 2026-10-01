package com.fabian.todolist.ui;

import androidx.annotation.DrawableRes;
import androidx.annotation.StringRes;
import com.fabian.todolist.R;
import com.fabian.todolist.data.TaskPriority;

/**
 * Archivo Central Unificado de Iconos de FabiToDo (Appicons.java).
 * Centraliza TODOS los iconos de la aplicacion en un unico punto:
 * - Vectores SVG limpios estilo SVGRepo (https://www.svgrepo.com)
 * - Recursos de dibujo (drawables) del sistema y de la aplicacion
 * - Iconos de prioridades, categorias, estados y acciones
 * - Colores hexadecimales ARGB y cadenas asociadas
 * - Metodos resolutores estaticos para prioridades, categorias y acciones
 */
public final class Appicons {

    private Appicons() {
        // Constructor privado para clase estatica de utilidades
    }

    // =========================================================================
    // 1. ICONOS PRINCIPALES DE ACCION (Vectores SVG / SVGRepo)
    // =========================================================================
    @DrawableRes public static final int ICON_ADD = R.drawable.ic_svg_add;
    @DrawableRes public static final int ICON_CHECK = R.drawable.ic_svg_check;
    @DrawableRes public static final int ICON_CALENDAR = R.drawable.ic_svg_calendar;
    @DrawableRes public static final int ICON_CLOCK = R.drawable.ic_svg_clock;
    @DrawableRes public static final int ICON_BELL = R.drawable.ic_svg_bell;
    @DrawableRes public static final int ICON_TRASH = R.drawable.ic_svg_trash;
    @DrawableRes public static final int ICON_SETTINGS = R.drawable.ic_svg_settings;
    @DrawableRes public static final int ICON_TAG = R.drawable.ic_svg_tag;
    @DrawableRes public static final int ICON_SEARCH = R.drawable.ic_svg_search;
    @DrawableRes public static final int ICON_EDIT = R.drawable.ic_svg_edit;
    @DrawableRes public static final int ICON_REPEAT = R.drawable.ic_svg_repeat;

    // =========================================================================
    // 2. ICONOS DE PRIORIDAD (Banderas SVG / SVGRepo)
    // =========================================================================
    @DrawableRes public static final int ICON_FLAG_LOW = R.drawable.ic_svg_flag_low;
    @DrawableRes public static final int ICON_FLAG_MEDIUM = R.drawable.ic_svg_flag_medium;
    @DrawableRes public static final int ICON_FLAG_HIGH = R.drawable.ic_svg_flag_high;
    @DrawableRes public static final int ICON_FLAG_URGENT = R.drawable.ic_svg_flag_urgent;

    // =========================================================================
    // 3. ICONOS DEL SISTEMA, NOTIFICACIONES Y WIDGETS
    // =========================================================================
    @DrawableRes public static final int ICON_NOTIFICATION_CHECKMARK = R.drawable.ic_notification_checkmark;
    @DrawableRes public static final int ICON_GOOGLE_LOGO = R.drawable.ic_google_logo;
    @DrawableRes public static final int ICON_WIDGET_CIRCLE = R.drawable.ic_widget_circle;
    @DrawableRes public static final int ICON_WIDGET_CHECK_CIRCLE = R.drawable.ic_widget_check_circle;

    // Aliases para maxima compatibilidad directa
    @DrawableRes public static final int DRAWABLE_SVG_ADD = ICON_ADD;
    @DrawableRes public static final int DRAWABLE_SVG_CHECK = ICON_CHECK;
    @DrawableRes public static final int DRAWABLE_SVG_CALENDAR = ICON_CALENDAR;
    @DrawableRes public static final int DRAWABLE_SVG_CLOCK = ICON_CLOCK;
    @DrawableRes public static final int DRAWABLE_SVG_BELL = ICON_BELL;
    @DrawableRes public static final int DRAWABLE_SVG_TRASH = ICON_TRASH;
    @DrawableRes public static final int DRAWABLE_SVG_SETTINGS = ICON_SETTINGS;
    @DrawableRes public static final int DRAWABLE_SVG_TAG = ICON_TAG;
    @DrawableRes public static final int DRAWABLE_SVG_SEARCH = ICON_SEARCH;
    @DrawableRes public static final int DRAWABLE_SVG_EDIT = ICON_EDIT;
    @DrawableRes public static final int DRAWABLE_SVG_REPEAT = ICON_REPEAT;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_LOW = ICON_FLAG_LOW;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_MEDIUM = ICON_FLAG_MEDIUM;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_HIGH = ICON_FLAG_HIGH;
    @DrawableRes public static final int DRAWABLE_SVG_FLAG_URGENT = ICON_FLAG_URGENT;
    @DrawableRes public static final int DRAWABLE_LOW_PRIORITY = ICON_FLAG_LOW;
    @DrawableRes public static final int DRAWABLE_MEDIUM_PRIORITY = ICON_FLAG_MEDIUM;
    @DrawableRes public static final int DRAWABLE_HIGH_PRIORITY = ICON_FLAG_HIGH;
    @DrawableRes public static final int DRAWABLE_URGENT_PRIORITY = ICON_FLAG_URGENT;
    @DrawableRes public static final int DRAWABLE_TASK_ADD = ICON_ADD;
    @DrawableRes public static final int DRAWABLE_TASK_CHECK = ICON_CHECK;
    @DrawableRes public static final int DRAWABLE_NOTIFICATION = ICON_BELL;
    @DrawableRes public static final int DRAWABLE_CALENDAR = ICON_CALENDAR;

    // =========================================================================
    // 4. COLORES ARGB UNIFICADOS DE PRIORIDADES
    // =========================================================================
    public static final long COLOR_CRITICAL = 0xFFB3261EL; // Rojo Urgente
    public static final long COLOR_HIGH = 0xFFEA4335L;     // Rojo/Naranja Alta
    public static final long COLOR_MEDIUM = 0xFFFBBC05L;   // Ambar Media
    public static final long COLOR_LOW = 0xFF34A853L;      // Verde Baja
    public static final long COLOR_NONE = 0xFF808080L;     // Gris Sin Prioridad

    // =========================================================================
    // 5. RESOLUTORES DE PRIORIDADES
    // =========================================================================
    @DrawableRes
    public static int getPriorityDrawable(String priority) {
        if (priority == null) return ICON_FLAG_LOW;
        switch (priority) {
            case TaskPriority.CRITICAL:
                return ICON_FLAG_URGENT;
            case TaskPriority.HIGH:
                return ICON_FLAG_HIGH;
            case TaskPriority.MEDIUM:
                return ICON_FLAG_MEDIUM;
            case TaskPriority.LOW:
            default:
                return ICON_FLAG_LOW;
        }
    }

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

    // =========================================================================
    // 6. RESOLUTORES DE CATEGORIAS
    // =========================================================================
    @DrawableRes
    public static int getCategoryDrawable(String category) {
        if (category == null) return ICON_TAG;
        switch (category.toLowerCase()) {
            case "compras":
            case "shopping":
            case "mercado":
            case "comprar":
                return ICON_CHECK;
            case "trabajo":
            case "work":
            case "laboral":
                return ICON_CALENDAR;
            case "personal":
                return ICON_TAG;
            case "hogar":
            case "home":
            case "casa":
                return ICON_SETTINGS;
            case "salud":
            case "health":
                return ICON_CLOCK;
            default:
                return ICON_TAG;
        }
    }

    // =========================================================================
    // 7. RESOLUTORES DE ACCIONES COMUNES
    // =========================================================================
    @DrawableRes
    public static int getActionDrawable(String actionName) {
        if (actionName == null) return ICON_CHECK;
        switch (actionName.toLowerCase()) {
            case "add":
            case "crear":
            case "nueva":
                return ICON_ADD;
            case "delete":
            case "eliminar":
            case "borrar":
                return ICON_TRASH;
            case "edit":
            case "editar":
            case "modificar":
                return ICON_EDIT;
            case "search":
            case "buscar":
                return ICON_SEARCH;
            case "settings":
            case "ajustes":
            case "configuracion":
                return ICON_SETTINGS;
            case "repeat":
            case "repetir":
                return ICON_REPEAT;
            case "reminder":
            case "recordatorio":
            case "alarma":
                return ICON_BELL;
            case "calendar":
            case "fecha":
                return ICON_CALENDAR;
            case "time":
            case "hora":
                return ICON_CLOCK;
            default:
                return ICON_CHECK;
        }
    }
}
