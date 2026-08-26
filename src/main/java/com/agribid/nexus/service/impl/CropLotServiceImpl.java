package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.crop.Category;
import com.agribid.nexus.domain.crop.CropLot;
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

import java.time.Duration;
import java.time.Instant;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class CropLotServiceImpl implements CropLotService {

    /**
     * A video timestamped further in the past than this is rejected
     * outright — closes off "record a good lot once, resubmit it for
     * every new listing" as a strategy. 10 minutes gives real slack
     * for upload time over a slow rural connection without allowing
     * meaningfully stale footage.
     */
    private static final Duration MAX_CAPTURE_AGE = Duration.ofMinutes(10);

    private static final Set<String> ALLOWED_VIDEO_TYPES = Set.of("video/mp4", "video/quicktime", "video/webm");

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

        Category category = entityManager.getReference(Category.class, categoryId);

        CropLot lot = new CropLot(farmer, category, request.quantityKg());
        cropLotRepository.save(lot);
        return CropLotMapper.toResponse(lot);
    }

    @Override
    @Transactional
    public CropLotResponse attachVideo(Long lotId, MultipartFile video, Double latitude, Double longitude, Instant capturedAt, UserPrincipal farmerPrincipal) {
        CropLot lot = requireOwnedLot(lotId, farmerPrincipal);

        validateVideoFile(video);
        validateGps(latitude, longitude);
        validateFreshness(capturedAt);

        String storedPath = fileStorageUtil.store(video, "crop-lots/" + lotId);
        lot.setVideoUrl(storedPath);
        lot.setCaptureLatitude(latitude);
        lot.setCaptureLongitude(longitude);
        lot.setCapturedAt(capturedAt);
        return CropLotMapper.toResponse(lot);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<CropLotResponse> getLotsForFarmer(Long farmerId, Pageable pageable) {
        return cropLotRepository.findByOwnerId(farmerId, pageable).map(CropLotMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public CropLotResponse getLot(Long lotId) {
        return CropLotMapper.toResponse(findLotOrThrow(lotId));
    }

    private void validateVideoFile(MultipartFile video) {
        if (video == null || video.isEmpty()) {
            throw new IllegalArgumentException("A video file is required");
        }
        String contentType = video.getContentType();
        if (contentType == null || !ALLOWED_VIDEO_TYPES.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Unsupported video format: " + contentType + " — accepted: " + ALLOWED_VIDEO_TYPES);
        }
    }

    private void validateGps(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("GPS coordinates are required with every video submission");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("GPS coordinates out of valid range");
        }
        // NOTE: this only validates the coordinates are well-formed —
        // it does NOT cross-check them against the farmer's registered
        // district/state. That would need a reverse-geocoding call
        // (or a static district-boundary lookup), which is a real,
        // honest gap: right now a farmer could submit GPS coordinates
        // from anywhere, not necessarily their own field. Flagging
        // this rather than pretending it's already handled.
    }

    private void validateFreshness(Instant capturedAt) {
        if (capturedAt == null) {
            throw new IllegalArgumentException("capturedAt timestamp is required");
        }
        Duration age = Duration.between(capturedAt, Instant.now());
        if (age.isNegative() || age.compareTo(MAX_CAPTURE_AGE) > 0) {
            throw new IllegalArgumentException(
                    "Video capture timestamp is stale or invalid — must be within " + MAX_CAPTURE_AGE.toMinutes() + " minutes of upload");
        }
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