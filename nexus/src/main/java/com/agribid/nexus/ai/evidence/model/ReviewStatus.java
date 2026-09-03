package com.agribid.nexus.ai.evidence.model;

/**
 * NOT_REQUIRED is the default for HIGH/MEDIUM evidence tiers — most
 * submissions never need a human to look at them at all. PENDING is
 * set automatically when overallEvidence comes out NEEDS_REVIEW or
 * LOW (see EvidenceAssessmentService); an agronomist must then move
 * it to APPROVED or REJECTED before the lot can be listed for
 * auction (see BidListingServiceImpl.publishListing).
 */
public enum ReviewStatus {
    NOT_REQUIRED,
    PENDING,
    APPROVED,
    REJECTED
}
