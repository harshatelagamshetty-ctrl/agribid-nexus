package com.agribid.nexus.ai.util;

import java.util.Map;

/**
 * The single fix that makes every farmer-facing AI response (crop
 * grading notes, reserve-price reasoning, demand forecasts) respect
 * the user's language instead of defaulting to English — directly
 * answers the "English-only" accessibility gap for the AI layer of
 * the product. It does not, on its own, make the REST API itself
 * usable by a non-technical first-time smartphone user — that still
 * needs a real client (see README "Known Limitations" / Roadmap) —
 * but every word Gemini generates for a farmer can now come back in
 * their own language, with vocabulary calibrated for basic literacy.
 */
public final class LanguageInstructions {

    private static final Map<String, String> LANGUAGE_NAMES = Map.ofEntries(
            Map.entry("en", "English"),
            Map.entry("hi", "Hindi"),
            Map.entry("ta", "Tamil"),
            Map.entry("te", "Telugu"),
            Map.entry("mr", "Marathi"),
            Map.entry("pa", "Punjabi"),
            Map.entry("gu", "Gujarati"),
            Map.entry("kn", "Kannada"),
            Map.entry("bn", "Bengali"),
            Map.entry("ml", "Malayalam"),
            Map.entry("or", "Odia")
    );

    private LanguageInstructions() {
    }

    /**
     * Renders a clause to append to any farmer-facing prompt.
     * Unrecognized codes fall back to English rather than guessing a
     * script the model hasn't been told about.
     */
    public static String instructionFor(String languageCode) {
        String code = languageCode == null ? "en" : languageCode.trim().toLowerCase();
        String languageName = LANGUAGE_NAMES.getOrDefault(code, "English");

        return "Respond in %s, using its native script. Use short sentences and plain, " +
                "everyday vocabulary a farmer with basic literacy can follow — avoid financial " +
                "or agronomic jargon; when a technical term is unavoidable, briefly explain it " +
                "in the same sentence.".formatted(languageName);
    }
}
