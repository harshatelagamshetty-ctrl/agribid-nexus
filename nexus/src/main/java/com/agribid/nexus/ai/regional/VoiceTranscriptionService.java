package com.agribid.nexus.ai.regional;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.util.MimeTypeUtils;

/**
 * Extracts a crop category code and quantity from spoken audio —
 * "five hundred kilo tomato" becomes { "TOMATO", 500 }. Reuses the
 * exact multimodal pattern CropGradingService already uses for
 * video, just with an audio Media object instead — this is not a
 * new AI capability, it's the same Gemini call shape applied to a
 * different input type.
 *
 * Deliberately returns a structured, farmer-confirmable draft, not
 * an auto-created crop lot — see the honest caveat in the API
 * reference: transcription accuracy for regional languages, farm
 * background noise, and unclear pronunciation are all real failure
 * modes, so the actual lot is only created after the farmer
 * confirms what was heard, via the existing
 * CropLotService.createLot() endpoint.
 */
@Service
public class VoiceTranscriptionService {

    private final ChatClient visionChatClient;

    public VoiceTranscriptionService(ChatClient visionChatClient) {
        this.visionChatClient = visionChatClient;
    }

    public record VoiceLotDraft(String categoryCodeGuess, Double quantityKgGuess, String rawTranscript, boolean confident) {}

    public VoiceLotDraft transcribeToLotDraft(byte[] audioBytes, String languageHint) {
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType("audio/mp3"))
                .data(new ByteArrayResource(audioBytes))
                .build();

        String prompt = """
                Listen to this audio. The speaker (likely speaking %s or a regional Indian
                language) is describing a crop lot they want to sell — a crop type and a
                quantity in kilograms. Respond with exactly three lines:
                TRANSCRIPT: <what you heard, transcribed>
                CATEGORY: <your best guess at the crop, in English, or UNKNOWN if unclear>
                QUANTITY_KG: <the quantity in kilograms as a number, or UNKNOWN if unclear>
                Do not guess a category or quantity you did not actually hear — say UNKNOWN
                rather than fabricate a plausible-sounding value.
                """.formatted(languageHint == null ? "an Indian regional language" : languageHint);

        String raw = visionChatClient.prompt()
                .user(u -> u.text(prompt).media(media))
                .call()
                .content();

        return parse(raw);
    }

    private VoiceLotDraft parse(String raw) {
        if (raw == null) {
            return new VoiceLotDraft(null, null, "", false);
        }
        String transcript = extractLine(raw, "TRANSCRIPT:");
        String categoryRaw = extractLine(raw, "CATEGORY:");
        String quantityRaw = extractLine(raw, "QUANTITY_KG:");

        String category = ("UNKNOWN".equalsIgnoreCase(categoryRaw) || categoryRaw == null)
                ? null : categoryRaw.trim().toUpperCase();
        Double quantity = null;
        if (quantityRaw != null && !"UNKNOWN".equalsIgnoreCase(quantityRaw)) {
            try {
                quantity = Double.parseDouble(quantityRaw.replaceAll("[^0-9.]", ""));
            } catch (NumberFormatException ignored) {
                // stays null — an unparsable quantity is honestly
                // reported as unknown, not defaulted to a guess
            }
        }

        return new VoiceLotDraft(category, quantity, transcript, category != null && quantity != null);
    }

    private String extractLine(String raw, String prefix) {
        return raw.lines()
                .filter(l -> l.trim().toUpperCase().startsWith(prefix))
                .findFirst()
                .map(l -> l.substring(l.indexOf(':') + 1).trim())
                .orElse(null);
    }
}
