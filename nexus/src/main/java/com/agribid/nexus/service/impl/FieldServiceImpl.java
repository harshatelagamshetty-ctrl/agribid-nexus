package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.crop.Field;
import com.agribid.nexus.domain.user.FarmerProfile;
import com.agribid.nexus.dto.mapper.FieldMapper;
import com.agribid.nexus.dto.request.FieldRegisterRequest;
import com.agribid.nexus.dto.response.FieldResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.FarmerProfileRepository;
import com.agribid.nexus.repository.FieldRepository;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.FieldService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FieldServiceImpl implements FieldService {

    private final FieldRepository fieldRepository;
    private final FarmerProfileRepository farmerProfileRepository;

    @Override
    @Transactional
    public FieldResponse registerField(FieldRegisterRequest request, UserPrincipal farmerPrincipal) {
        FarmerProfile farmer = farmerProfileRepository.findById(farmerPrincipal.getId())
            .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerPrincipal.getId()));

        Field field = new Field(farmer, request.fieldName(), request.latitude(), request.longitude(), request.radiusMeters());
        fieldRepository.save(field);
        return FieldMapper.toResponse(field);
    }

    @Override
    public List<FieldResponse> getMyFields(UserPrincipal farmerPrincipal) {
        return fieldRepository.findByOwnerId(farmerPrincipal.getId()).stream()
            .map(FieldMapper::toResponse)
            .toList();
    }
}
