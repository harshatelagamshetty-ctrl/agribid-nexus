package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.PestTag;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PestTagRepository extends JpaRepository<PestTag, Long> {
    Optional<PestTag> findByCode(String code);
}
