package com.agribid.nexus.ai.evidence.model;

/**
 * The final composed verdict. Deliberately has no numeric score
 * attached anywhere in this system — a number like "73/100" implies
 * a precision the underlying signals don't actually have. These four
 * categorical states are what get shown to anyone reviewing a lot,
 * and each one is derived from an explicit, inspectable rule (see
 * EvidenceAssessmentService), not a black-box weighted formula.
 */
public enum OverallEvidence {
    HIGH,
    MEDIUM,
    NEEDS_REVIEW,
    LOW
}
