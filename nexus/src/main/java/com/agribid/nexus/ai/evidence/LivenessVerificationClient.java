package com.agribid.nexus.ai.evidence;

import com.agribid.nexus.ai.evidence.model.ChallengeResult;
import com.agribid.nexus.ai.evidence.model.ChallengeType;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.content.Media;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

/**
 * Asks Gemini one narrow, specific question about the same video
 * already submitted for grading: "was this exact instruction visibly
 * or audibly performed?" — not a general authenticity judgment, a
 * targeted check against a known, specific target.
 *
 * Honest scope statement, stated here because it matters more than
 * anywhere else in this class: this does NOT prove the video wasn't
 * pre-recorded and later dubbed/edited to include the challenge —
 * that would require frame-level forensic analysis this system does
 * not have. What it DOES prove is that whoever produced this video
 * had access to a specific instruction that did not exist until
 * moments before recording — which rules out truly pre-recorded
 * stock footage and rules out any deepfake generated before the
 * challenge was issued. That is a real, meaningful, honestly-scoped
 * guarantee, not the strongest possible one.
 */
@Component
public class LivenessVerificationClient {

    private final ChatClient visionChatClient;

    public LivenessVerificationClient(ChatClient visionChatClient) {
        this.visionChatClient = visionChatClient;
    }

    public ChallengeResult verify(byte[] videoBytes, ChallengeType type, String expectedValue) {
        try {
            Media media = Media.builder()
                    .mimeType(MimeTypeUtils.parseMimeType("video/mp4"))
                    .data(new ByteArrayResource(videoBytes))
                    .build();

            String instructionDescription = type == ChallengeType.SPOKEN_CODE
                    ? "the person in the video clearly speaks the number \"" + expectedValue + "\" out loud at some point"
                    : "the person in the video clearly performs this hand gesture at some point: \"" + expectedValue + "\"";

            String prompt = """
                    Watch this video and determine only one thing: whether %s.
                    Respond with exactly one word: PASSED if you clearly observe it happening,
                    FAILED if you watched the full video and it clearly does not happen,
                    or UNCERTAIN if the video quality, angle, or audio makes it impossible to tell
                    either way. Do not guess — UNCERTAIN is the correct answer when you are not sure.
                    """.formatted(instructionDescription);

            String raw = visionChatClient.prompt()
                    .user(u -> u.text(prompt).media(media))
                    .call()
                    .content();

            return parseResult(raw);
        } catch (Exception e) {
            // Same honest-degradation contract as WeatherPlausibilityClient:
            // any failure (API error, timeout, malformed response) becomes
            // UNCERTAIN, never silently treated as PASSED or FAILED.
            return ChallengeResult.UNCERTAIN;
        }
    }

    private ChallengeResult parseResult(String raw) {
        if (raw == null) {
            return ChallengeResult.UNCERTAIN;
        }
        String normalized = raw.trim().toUpperCase();
        if (normalized.contains("PASSED")) {
            return ChallengeResult.PASSED;
        }
        if (normalized.contains("FAILED")) {
            return ChallengeResult.FAILED;
        }
        return ChallengeResult.UNCERTAIN;
    }
}
