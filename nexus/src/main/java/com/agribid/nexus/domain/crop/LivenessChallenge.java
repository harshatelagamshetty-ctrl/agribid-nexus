package com.agribid.nexus.domain.crop;

import com.agribid.nexus.ai.evidence.model.ChallengeType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * A short-lived, randomly-generated instruction the farmer must
 * visibly or audibly perform while recording — the actual defense
 * against a pre-recorded or AI-generated video, since neither can
 * respond to an instruction that didn't exist at the moment it was
 * created.
 *
 * One-to-one with CropLot (a lot can re-request a fresh challenge if
 * the first one expires before recording, overwriting this row —
 * see LivenessChallengeService — rather than accumulating history,
 * since only the most recent unexpired challenge is ever relevant).
 */
@Entity
@Table(name = "liveness_challenges")
@Getter
@Setter
@NoArgsConstructor
public class LivenessChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crop_lot_id", unique = true, nullable = false)
    private CropLot cropLot;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_type", nullable = false)
    private ChallengeType challengeType;

    /**
     * The literal instruction content in English (e.g. "4728" for a
     * spoken code, "hold up 3 fingers" for a gesture) — what
     * LivenessVerificationClient actually checks the video against.
     * The farmer-facing translated phrasing is generated at issuance
     * time and returned directly in the API response, not stored —
     * only the underlying check target needs to persist.
     */
    @Column(name = "challenge_value", nullable = false)
    private String challengeValue;

    @Column(name = "issued_at", nullable = false)
    private Instant issuedAt = Instant.now();

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    public LivenessChallenge(CropLot cropLot, ChallengeType challengeType, String challengeValue, Instant expiresAt) {
        this.cropLot = cropLot;
        this.challengeType = challengeType;
        this.challengeValue = challengeValue;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }
}
