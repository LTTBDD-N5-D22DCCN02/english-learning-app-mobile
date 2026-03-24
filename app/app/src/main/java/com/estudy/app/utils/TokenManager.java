package com.estudy.app.utils;

import android.content.Context;
import android.content.SharedPreferences;

public class TokenManager {
    private static final String PREF_NAME = "estudy_prefs";
    private static final String KEY_TOKEN = "jwt_token";

    private final SharedPreferences prefs;

    public TokenManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public void saveToken(String token) {
        prefs.edit().putString(KEY_TOKEN, token).apply();
    }

    public String getToken() {
        return prefs.getString(KEY_TOKEN, null);
    }

    public void clearToken() {
        prefs.edit().remove(KEY_TOKEN).apply();
    }

    public boolean hasToken() {
        return getToken() != null;
    }
}