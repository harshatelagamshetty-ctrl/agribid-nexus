package com.agribid.nexus.ai.evidence;

import com.agribid.nexus.ai.evidence.model.ChallengeType;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.LivenessChallenge;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.LivenessChallengeRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Issues challenges with fixed, pre-written phrasing per language
 * rather than an AI call to translate the instruction — deliberately.
 * The instruction sentence is a handful of fixed templates, not
 * free-form content, so a small hand-maintained translation table is
 * both cheaper and more reliable than a Gemini call: it works even
 * if Gemini is completely unreachable, and it costs nothing per
 * issuance. Only VERIFYING the challenge (LivenessVerificationClient)
 * genuinely needs the model — generating the instruction text does
 * not.
 *
 * Honest limitation: the translations below cover the same language
 * set as LanguageInstructions and were written for this specific,
 * narrow purpose — they have not been verified by a native speaker
 * of every listed language. Flagging this directly rather than
 * presenting them as verified-correct.
 */
@Service
public class LivenessChallengeService {

    private static final Duration CHALLENGE_VALIDITY = Duration.ofMinutes(10);
    private static final SecureRandom RANDOM = new SecureRandom();

    private static final List<String> GESTURES = List.of(
            "hold up 2 fingers", "hold up 3 fingers", "hold up 5 fingers",
            "give a thumbs up", "make a fist"
    );

    private static final Map<String, String> SPOKEN_INSTRUCTION_TEMPLATE = Map.ofEntries(
            Map.entry("en", "Say this number out loud clearly while filming your crop: %s"),
            Map.entry("hi", "अपनी फसल फिल्माते समय इस संख्या को ज़ोर से और स्पष्ट रूप से बोलें: %s"),
            Map.entry("ta", "உங்கள் பயிரை படமாக்கும்போது இந்த எண்ணை உரக்கச் சொல்லுங்கள்: %s"),
            Map.entry("te", "మీ పంటను చిత్రీకరిస్తున్నప్పుడు ఈ సంఖ్యను బిగ్గరగా చెప్పండి: %s"),
            Map.entry("mr", "तुमचे पीक चित्रित करताना हा क्रमांक मोठ्याने बोला: %s")
    );

    private static final Map<String, String> GESTURE_INSTRUCTION_TEMPLATE = Map.ofEntries(
            Map.entry("en", "While filming your crop, also clearly show this with your hand: %s"),
            Map.entry("hi", "अपनी फसल फिल्माते समय, अपने हाथ से यह भी स्पष्ट रूप से दिखाएं: %s"),
            Map.entry("ta", "உங்கள் பயிரை படமாக்கும்போது, உங்கள் கையால் இதைக் காட்டுங்கள்: %s"),
            Map.entry("te", "మీ పంటను చిత్రీకరిస్తున్నప్పుడు, మీ చేతితో దీన్ని కూడా చూపించండి: %s"),
            Map.entry("mr", "तुमचे पीक चित्रित करताना, तुमच्या हाताने हे देखील दाखवा: %s")
    );

    private final CropLotRepository cropLotRepository;
    private final LivenessChallengeRepository challengeRepository;

    public LivenessChallengeService(CropLotRepository cropLotRepository, LivenessChallengeRepository challengeRepository) {
        this.cropLotRepository = cropLotRepository;
        this.challengeRepository = challengeRepository;
    }

    public record IssuedChallenge(String displayInstruction, Instant expiresAt) {
    }

    @Transactional
    public IssuedChallenge issueChallenge(Long lotId) {
        CropLot lot = cropLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + lotId));

        ChallengeType type = RANDOM.nextBoolean() ? ChallengeType.SPOKEN_CODE : ChallengeType.HAND_GESTURE;
        String value = type == ChallengeType.SPOKEN_CODE
                ? String.valueOf(1000 + RANDOM.nextInt(9000))
                : GESTURES.get(RANDOM.nextInt(GESTURES.size()));

        Instant expiresAt = Instant.now().plus(CHALLENGE_VALIDITY);

        LivenessChallenge challenge = challengeRepository.findByCropLotId(lotId)
                .orElseGet(() -> new LivenessChallenge(lot, type, value, expiresAt));
        challenge.setCropLot(lot);
        challenge.setChallengeType(type);
        challenge.setChallengeValue(value);
        challenge.setIssuedAt(Instant.now());
        challenge.setExpiresAt(expiresAt);
        challengeRepository.save(challenge);

        String language = lot.getOwner().getPreferredLanguage();
        Map<String, String> templates = type == ChallengeType.SPOKEN_CODE
                ? SPOKEN_INSTRUCTION_TEMPLATE : GESTURE_INSTRUCTION_TEMPLATE;
        String template = templates.getOrDefault(language, templates.get("en"));

        return new IssuedChallenge(template.formatted(value), expiresAt);
    }
}
