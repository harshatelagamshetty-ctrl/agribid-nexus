package com.agribid.nexus.domain.contract;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Deliberately mirrors CropLotEvidenceReport's review fields exactly
 * (reviewStatus/reviewedBy/reviewedAt/reviewNote) rather than
 * inventing a parallel shape — the intent is that
 * AgronomistReviewController's existing patterns extend naturally to
 * this too, not that disputes are a separate system bolted on.
 */
@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class Dispute {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(name = "raised_by", nullable = false)
    private Long raisedByUserId;

    @Column(nullable = false, length = 1000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private com.agribid.nexus.ai.evidence.model.ReviewStatus status =
            com.agribid.nexus.ai.evidence.model.ReviewStatus.PENDING;

    @Column(name = "reviewed_by")
    private Long reviewedBy;

    @Column(name = "reviewed_at")
    private Instant reviewedAt;

    @Column(name = "review_note", length = 1000)
    private String reviewNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Dispute(Order order, Long raisedByUserId, String reason) {
        this.order = order;
        this.raisedByUserId = raisedByUserId;
        this.reason = reason;
    }
}
