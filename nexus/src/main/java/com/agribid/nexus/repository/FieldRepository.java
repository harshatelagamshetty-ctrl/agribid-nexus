package com.agribid.nexus.repository;

import com.agribid.nexus.domain.crop.Field;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FieldRepository extends JpaRepository<Field, Long> {
    List<Field> findByOwnerId(Long ownerId);
}
