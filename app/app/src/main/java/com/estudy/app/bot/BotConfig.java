package com.estudy.app.bot;

import com.estudy.app.BuildConfig;

public class BotConfig {

    // Keys are loaded from local.properties (never committed to git)
    public static final String ELEVEN_API_KEY = BuildConfig.ELEVEN_API_KEY;
    public static final String GEMINI_API_KEY  = BuildConfig.GEMINI_API_KEY;

    // ── Voice / model settings ────────────────────────────────────────
    public static final String VOICE_ID     = "JBFqnCBsd6RMkjVDRZzb";
    public static final String TTS_MODEL    = "eleven_v3";
    public static final String GEMINI_MODEL = "gemini-2.5-flash";
}
