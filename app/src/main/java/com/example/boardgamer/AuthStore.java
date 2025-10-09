package com.example.boardgamer;

import android.content.Context;

import androidx.annotation.Nullable;

// AuthStore – Access + Refresh speichern/laden
public final class AuthStore {
    private static final String PREF = "auth";
    private static final String KEY_ACCESS = "access";
    private static final String KEY_REFRESH = "refresh";

    public static void saveTokens(Context ctx, String access, String refresh) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_ACCESS, access)
                .putString(KEY_REFRESH, refresh)
                .apply();
    }

    public static @Nullable String loadAccess(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_ACCESS, null);
    }

    public static @Nullable String loadRefresh(Context ctx) {
        return ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY_REFRESH, null);
    }

    public static void clear(Context ctx) {
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().clear().apply();
    }
}
