-- Liveness challenge: the random spoken-code / hand-gesture
-- instruction a farmer must perform while recording, defending
-- against pre-recorded and AI-generated video (see
-- LivenessChallengeService and LivenessVerificationClient).
CREATE TABLE liveness_challenges (
    id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    crop_lot_id         BIGINT NOT NULL UNIQUE REFERENCES crop_lots (id),
    challenge_type      VARCHAR(20) NOT NULL,
    challenge_value     VARCHAR(120) NOT NULL,
    issued_at           TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at          TIMESTAMP WITH TIME ZONE NOT NULL
);

-- Extends the evidence report with the challenge outcome and the
-- human-in-the-loop review state. reviewed_by references users, not
-- specifically agronomist_profiles, since the FK only needs to point
-- at a real user row — AgronomistReviewController's own
-- @PreAuthorize is what actually restricts who can perform a review,
-- not this constraint.
ALTER TABLE crop_lot_evidence_reports ADD COLUMN challenge_result VARCHAR(20) NOT NULL DEFAULT 'NOT_ISSUED';
ALTER TABLE crop_lot_evidence_reports ADD COLUMN review_status VARCHAR(20) NOT NULL DEFAULT 'NOT_REQUIRED';
ALTER TABLE crop_lot_evidence_reports ADD COLUMN reviewed_by BIGINT REFERENCES users (id);
ALTER TABLE crop_lot_evidence_reports ADD COLUMN reviewed_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE crop_lot_evidence_reports ADD COLUMN review_note VARCHAR(1000);

CREATE INDEX idx_evidence_review_status ON crop_lot_evidence_reports (review_status);
