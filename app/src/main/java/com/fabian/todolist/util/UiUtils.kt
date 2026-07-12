package com.fabian.todolist.util

import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes

object UiUtils {

    /**
     * Shows a short Toast notification.
     */
    fun showShortToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows a short Toast notification using a String resource ID.
     */
    fun showShortToast(context: Context, @StringRes stringResId: Int) {
        Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_SHORT).show()
    }

    /**
     * Shows a long Toast notification.
     */
    fun showLongToast(context: Context, message: String) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    /**
     * Shows a long Toast notification using a String resource ID.
     */
    fun showLongToast(context: Context, @StringRes stringResId: Int) {
        Toast.makeText(context, context.getString(stringResId), Toast.LENGTH_LONG).show()
    }

    /**
     * Utility method to format and construct Toast with dynamic formatted arguments.
     */
    fun showFormattedShortToast(context: Context, @StringRes stringResId: Int, vararg args: Any?) {
        Toast.makeText(context, context.getString(stringResId, *args), Toast.LENGTH_SHORT).show()
    }
}
