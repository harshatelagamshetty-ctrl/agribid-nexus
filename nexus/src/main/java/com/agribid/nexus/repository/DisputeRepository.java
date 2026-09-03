package com.agribid.nexus.repository;

import com.agribid.nexus.ai.evidence.model.ReviewStatus;
import com.agribid.nexus.domain.contract.Dispute;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DisputeRepository extends JpaRepository<Dispute, Long> {
    Page<Dispute> findByStatus(ReviewStatus status, Pageable pageable);
}
