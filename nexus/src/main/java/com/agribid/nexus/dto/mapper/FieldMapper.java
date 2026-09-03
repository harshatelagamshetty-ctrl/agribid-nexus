package com.agribid.nexus.dto.mapper;

import com.agribid.nexus.domain.crop.Field;
import com.agribid.nexus.dto.response.FieldResponse;

public final class FieldMapper {

    private FieldMapper() {
    }

    public static FieldResponse toResponse(Field field) {
        return new FieldResponse(
            field.getId(),
            field.getOwner().getId(),
            field.getFieldName(),
            field.getLatitude(),
            field.getLongitude(),
            field.getRadiusMeters(),
            field.getCreatedAt()
        );
    }
}
