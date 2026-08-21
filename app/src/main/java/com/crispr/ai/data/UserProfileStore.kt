package com.crispr.ai.data

import android.content.Context
import android.content.SharedPreferences

/**
 * Simple local storage for user profile (name, avatar URI, stats).
 * No cloud. Data stays on device only.
 */
object UserProfileStore {

    private const val PREFS_NAME = "crispr_user_profile"
    private const val KEY_NAME = "display_name"
    private const val KEY_AVATAR_URI = "avatar_uri"
    private const val KEY_TOTAL_CHATS = "total_chats"
    private const val KEY_SETUP_DONE = "setup_done"
    private const val KEY_THEME = "theme_pref"
    private const val KEY_LANGUAGE = "language_pref"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun getDisplayName(ctx: Context): String =
        prefs(ctx).getString(KEY_NAME, "") ?: ""

    fun setDisplayName(ctx: Context, name: String) =
        prefs(ctx).edit().putString(KEY_NAME, name).apply()

    fun getAvatarUri(ctx: Context): String =
        prefs(ctx).getString(KEY_AVATAR_URI, "") ?: ""

    fun setAvatarUri(ctx: Context, uri: String) =
        prefs(ctx).edit().putString(KEY_AVATAR_URI, uri).apply()

    fun getTotalChats(ctx: Context): Int =
        prefs(ctx).getInt(KEY_TOTAL_CHATS, 0)

    fun incrementChats(ctx: Context) {
        val cur = getTotalChats(ctx)
        prefs(ctx).edit().putInt(KEY_TOTAL_CHATS, cur + 1).apply()
    }

    fun isSetupDone(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_SETUP_DONE, false)

    fun markSetupDone(ctx: Context) =
        prefs(ctx).edit().putBoolean(KEY_SETUP_DONE, true).apply()

    fun getTheme(ctx: Context): String =
        prefs(ctx).getString(KEY_THEME, "Dark (Forest)") ?: "Dark (Forest)"

    fun setTheme(ctx: Context, theme: String) =
        prefs(ctx).edit().putString(KEY_THEME, theme).apply()

    fun getLanguage(ctx: Context): String =
        prefs(ctx).getString(KEY_LANGUAGE, "English") ?: "English"

    fun setLanguage(ctx: Context, lang: String) =
        prefs(ctx).edit().putString(KEY_LANGUAGE, lang).apply()
}
