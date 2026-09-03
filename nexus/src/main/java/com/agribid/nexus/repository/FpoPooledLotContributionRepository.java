package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.FpoPooledLotContribution;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FpoPooledLotContributionRepository extends JpaRepository<FpoPooledLotContribution, Long> {
    List<FpoPooledLotContribution> findByPooledLotId(Long pooledLotId);

    Optional<FpoPooledLotContribution> findByCropLotId(Long cropLotId);
}
