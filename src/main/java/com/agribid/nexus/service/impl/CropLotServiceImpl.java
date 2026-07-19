package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.crop.Category;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.LotStatus;
import com.agribid.nexus.domain.user.FarmerProfile;
import com.agribid.nexus.dto.mapper.CropLotMapper;
import com.agribid.nexus.dto.request.CropLotCreateRequest;
import com.agribid.nexus.dto.response.CropLotResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.exception.UnauthorizedActionException;
import com.agribid.nexus.repository.CategoryRepository;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.FarmerProfileRepository;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.CropLotService;
import com.agribid.nexus.util.FileStorageUtil;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
public class CropLotServiceImpl implements CropLotService {

    private final CropLotRepository cropLotRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final FileStorageUtil fileStorageUtil;
    private final EntityManager entityManager;

    @Override
    @Transactional
    public CropLotResponse createLot(CropLotCreateRequest request, UserPrincipal farmerPrincipal) {
        FarmerProfile farmer = farmerProfileRepository.findById(farmerPrincipal.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerPrincipal.getId()));

        Long categoryId = categoryRepository.findByCode(request.categoryCode())
                .map(Category::getId)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown category code: " + request.categoryCode()));

        // getReference avoids a full Category SELECT just to attach the FK,
        // now that we already know the id exists via the lookup above
        Category category = entityManager.getReference(Category.class, categoryId);

        CropLot lot = new CropLot(farmer, category, request.quantityKg(), null);
        cropLotRepository.save(lot);
        return CropLotMapper.toResponse(lot);
    }

    @Override
    @Transactional
    public CropLotResponse attachImage(Long lotId, MultipartFile image, UserPrincipal farmerPrincipal) {
        CropLot lot = requireOwnedLot(lotId, farmerPrincipal);
        String storedPath = fileStorageUtil.store(image, "crop-lots/" + lotId);
        lot.setImageUrl(storedPath);
        return CropLotMapper.toResponse(lot);
    }

    @Override
    public Page<CropLotResponse> getLotsForFarmer(Long farmerId, Pageable pageable) {
        return cropLotRepository.findByOwnerId(farmerId, pageable).map(CropLotMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CropLotResponse getLot(Long lotId) {
        return CropLotMapper.toResponse(findLotOrThrow(lotId));
    }

    private CropLot requireOwnedLot(Long lotId, UserPrincipal farmerPrincipal) {
        CropLot lot = findLotOrThrow(lotId);
        if (!lot.getOwner().getId().equals(farmerPrincipal.getId())) {
            throw new UnauthorizedActionException("You do not own crop lot " + lotId);
        }
        return lot;
    }

    private CropLot findLotOrThrow(Long lotId) {
        return cropLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + lotId));
    }
}