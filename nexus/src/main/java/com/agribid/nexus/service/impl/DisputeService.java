package com.agribid.nexus.service.impl;

import com.agribid.nexus.ai.evidence.model.ReviewStatus;
import com.agribid.nexus.domain.contract.Dispute;
import com.agribid.nexus.domain.contract.Order;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.DisputeRepository;
import com.agribid.nexus.repository.OrderRepository;
import com.agribid.nexus.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

/**
 * The lifecycle here is intentionally identical to
 * AgronomistReviewService's — a dispute is just another thing an
 * agronomist decides PENDING/APPROVED/REJECTED on, reusing the exact
 * same enum and the exact same review-queue mental model rather than
 * inventing a second one.
 */
@Service
public class DisputeService {

    private final DisputeRepository disputeRepository;
    private final OrderRepository orderRepository;
    private final org.springframework.ai.vectorstore.VectorStore vectorStore;

    public DisputeService(DisputeRepository disputeRepository, OrderRepository orderRepository,
                           org.springframework.ai.vectorstore.VectorStore vectorStore) {
        this.disputeRepository = disputeRepository;
        this.orderRepository = orderRepository;
        this.vectorStore = vectorStore;
    }

    @Transactional
    public Dispute raiseDispute(Long orderId, String reason, UserPrincipal raisedBy) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + orderId));
        Dispute dispute = new Dispute(order, raisedBy.getId(), reason);
        return disputeRepository.save(dispute);
    }

    public Page<Dispute> getPendingQueue(Pageable pageable) {
        return disputeRepository.findByStatus(ReviewStatus.PENDING, pageable);
    }

    @Transactional
    public Dispute recordDecision(Long disputeId, boolean approve, String note, UserPrincipal agronomist) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));
        if (dispute.getStatus() != ReviewStatus.PENDING) {
            throw new IllegalStateException("Dispute " + disputeId + " is not pending (current status: " + dispute.getStatus() + ")");
        }
        dispute.setStatus(approve ? ReviewStatus.APPROVED : ReviewStatus.REJECTED);
        dispute.setReviewedBy(agronomist.getId());
        dispute.setReviewedAt(Instant.now());
        dispute.setReviewNote(note);
        Dispute saved = disputeRepository.save(dispute);

        // This is what makes DisputeSuggestionService's RAG grounding
        // genuinely real rather than a claim against an empty corpus
        // — every resolved dispute becomes retrievable context for
        // the NEXT similar one, from this point forward.
        var doc = new org.springframework.ai.document.Document(
                "Dispute reason: " + saved.getReason() + "\nDecision: " + saved.getStatus()
                        + "\nAgronomist note: " + (saved.getReviewNote() == null ? "(none)" : saved.getReviewNote()));
        doc.getMetadata().put("source", "dispute-" + saved.getId());
        vectorStore.add(java.util.List.of(doc));

        return saved;
    }
}
