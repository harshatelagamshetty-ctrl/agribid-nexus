package com.agribid.nexus.repository;

import com.agribid.nexus.domain.regional.RegionalSignal;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface RegionalSignalRepository extends JpaRepository<RegionalSignal, Long> {
    Optional<RegionalSignal> findByDistrictAndCategoryIdAndWeekStart(String district, Long categoryId, LocalDate weekStart);
    List<RegionalSignal> findByDistrictAndCategoryIdOrderByWeekStartDesc(String district, Long categoryId);
    List<RegionalSignal> findByCategoryIdAndWeekStartGreaterThanEqual(Long categoryId, LocalDate since);
}
