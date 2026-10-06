package com.fabian.todolist.data

import android.content.Context

class AuthManager(context: Context) {
    private val appContext: Context = context.applicationContext

    fun getContext(): Context = appContext

    fun isWelcomeSeen(): Boolean =
        appContext.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            .getBoolean("welcome_seen", false)

    fun setWelcomeSeen(seen: Boolean) {
        appContext.getSharedPreferences("fabitodo_preferences", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("welcome_seen", seen)
            .apply()
    }
}
