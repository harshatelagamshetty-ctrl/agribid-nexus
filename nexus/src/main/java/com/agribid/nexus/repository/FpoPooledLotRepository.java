package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.FpoPooledLot;
import com.agribid.nexus.domain.crop.PoolStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FpoPooledLotRepository extends JpaRepository<FpoPooledLot, Long> {
    List<FpoPooledLot> findByFpoNameAndStatus(String fpoName, PoolStatus status);
}
