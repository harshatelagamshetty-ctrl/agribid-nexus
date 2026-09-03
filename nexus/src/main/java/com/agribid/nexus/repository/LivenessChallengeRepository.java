package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.LivenessChallenge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface LivenessChallengeRepository extends JpaRepository<LivenessChallenge, Long> {
    Optional<LivenessChallenge> findByCropLotId(Long cropLotId);
}
