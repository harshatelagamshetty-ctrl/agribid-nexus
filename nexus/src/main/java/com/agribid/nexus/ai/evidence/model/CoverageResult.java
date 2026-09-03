package com.agribid.nexus.ai.evidence.model;

/**
 * NOT_AVAILABLE is not a failure, matching the same philosophy as
 * ChallengeResult.NOT_ISSUED — a client that hasn't been updated to
 * capture a track yet (or a farmer on an app version that doesn't
 * support it) must never be penalized as if they were hiding
 * something. INSUFFICIENT means a track WAS submitted but didn't
 * meet the movement/spread thresholds — a genuine signal, but one
 * with real innocent explanations (a very small field, a farmer who
 * stood in one central spot filming a full 360-degree pan), so it
 * routes to review rather than automatic rejection.
 */
public enum CoverageResult {
    SUFFICIENT,
    INSUFFICIENT,
    NOT_AVAILABLE
}
