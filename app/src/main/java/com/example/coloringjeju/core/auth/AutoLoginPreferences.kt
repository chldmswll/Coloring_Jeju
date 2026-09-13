package com.example.coloringjeju.core.auth

import android.content.Context

/**
 * Whether a signed-in session should still be there the next time the app cold-starts. Firebase
 * itself always persists the session on disk regardless of this flag — [com.example.coloringjeju.MainActivity]
 * is what actually enforces it, signing the user back out at launch whenever this reads false, so
 * unchecking "자동 로그인" on the login form means a fresh [presentation.Auth.AuthScreen] next time.
 *
 * A single boolean in [android.content.SharedPreferences], same shape as
 * [com.example.coloringjeju.core.local.datastore.SavedSpotsStore].
 */
object AutoLoginPreferences {
    private const val PREFS_NAME = "auth_prefs"
    private const val KEY_AUTO_LOGIN = "auto_login"

    /** Defaults to true so a fresh install behaves like sign-in always did before this flag existed. */
    fun isEnabled(context: Context): Boolean =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getBoolean(KEY_AUTO_LOGIN, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().putBoolean(KEY_AUTO_LOGIN, enabled).apply()
    }
}
