package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.QualityGrade;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QualityGradeRepository extends JpaRepository<QualityGrade, Long> {
}
