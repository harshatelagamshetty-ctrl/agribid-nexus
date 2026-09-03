package com.agribid.nexus.ai.evidence.model;

/**
 * NOT_ISSUED is not a failure — it means the farmer didn't request
 * or complete a liveness challenge before attaching video, which is
 * allowed (the workflow must never hard-block a farmer with a
 * connectivity problem or an older phone). It simply means this
 * particular defense against pre-recorded/deepfake video wasn't
 * available for this submission, and the composed evidence tier
 * reflects that honestly rather than assuming the worst.
 *
 * UNCERTAIN exists because Gemini's video understanding is not
 * infallible — a genuine "we couldn't tell" outcome is reported as
 * exactly that, never silently rounded up to PASSED.
 */
public enum ChallengeResult {
    NOT_ISSUED,
    PASSED,
    FAILED,
    UNCERTAIN
}
