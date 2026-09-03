package com.agribid.nexus.ai.evidence.model;

/**
 * Two challenge modes, deliberately kept to two: a spoken code
 * (works well when the camera can't see hands clearly, e.g. filming
 * produce close-up) and a hand gesture (works when a noisy field
 * environment makes audio unreliable). Randomly chosen per issuance
 * so neither becomes predictable enough to pre-record around.
 */
public enum ChallengeType {
    SPOKEN_CODE,
    HAND_GESTURE
}
