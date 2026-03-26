package com.estudy.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONObject;

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

    // Decode JWT để lấy username (subject)
    public String getCurrentUsername() {
        try {
            String token = getToken();
            if (token == null) return null;

            // JWT có 3 phần: header.payload.signature
            String[] parts = token.split("\\.");
            if (parts.length < 2) return null;

            // Decode phần payload (base64)
            String payload = new String(Base64.decode(parts[1], Base64.URL_SAFE));
            JSONObject json = new JSONObject(payload);
            return json.getString("sub"); // sub = subject = username
        } catch (Exception e) {
            return null;
        }
    }
}