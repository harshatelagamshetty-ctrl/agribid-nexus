package com.agribid.nexus.service.impl;

import com.agribid.nexus.ai.evidence.EvidenceAssessmentService;
import com.agribid.nexus.domain.crop.Category;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.Field;
import com.agribid.nexus.domain.user.FarmerProfile;
import com.agribid.nexus.dto.mapper.CropLotMapper;
import com.agribid.nexus.dto.request.CropLotCreateRequest;
import com.agribid.nexus.dto.response.CropLotResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.exception.UnauthorizedActionException;
import com.agribid.nexus.repository.CategoryRepository;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.FarmerProfileRepository;
import com.agribid.nexus.repository.FieldRepository;
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
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
    private final FieldRepository fieldRepository;
    private final FileStorageUtil fileStorageUtil;
    private final EntityManager entityManager;
    private final EvidenceAssessmentService evidenceAssessmentService;

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
        lot.setSelfReportedLowWaterUsage(request.selfReportedLowWaterUsage());
        lot.setSelfReportedLowPesticideUsage(request.selfReportedLowPesticideUsage());
        cropLotRepository.save(lot);
        return CropLotMapper.toResponse(lot);
    }

    /**
     * fieldId is required going forward — every new video submission
     * should be tied to a registered Field so EvidenceAssessmentService
     * can actually check GPS against a known location, rather than
     * trusting raw coordinates in isolation. If the referenced field
     * doesn't belong to this farmer, that's treated as an ownership
     * violation, same severity as touching someone else's crop lot.
     *
     * After the video is stored, this method computes its SHA-256
     * hash server-side (never trusts a client-supplied hash), then
     * hands the fully-populated lot to EvidenceAssessmentService,
     * which runs every signal check and persists the explainable
     * report in the same transaction — so a CropLot with a video
     * attached always has a corresponding evidence report by the
     * time this method returns.
     */
    /**
     * Offline capture window — deliberately much wider than the
     * 10-minute online window, and deliberately gated behind an
     * explicit offlineCapture flag rather than silently applied to
     * everyone. This is a real, stated trade-off: offline
     * submissions are honestly less time-verified than online ones,
     * in exchange for working at all without connectivity. That
     * trade is visible in the evidence report (wasOfflineCapture),
     * not hidden.
     */
    private static final Duration MAX_OFFLINE_CAPTURE_AGE = Duration.ofHours(72);

    @Override
    @Transactional
    public CropLotResponse attachVideo(Long lotId, MultipartFile video, Long fieldId, Double latitude, Double longitude,
                                        Instant capturedAt, String gpsTrackEncoded, boolean offlineCapture,
                                        String offlineIdempotencyKey, UserPrincipal farmerPrincipal) {

        if (offlineCapture && offlineIdempotencyKey != null) {
            var existing = cropLotRepository.findByOfflineIdempotencyKey(offlineIdempotencyKey);
            if (existing.isPresent()) {
                // This exact offline submission already synced
                // successfully once — a retried request (e.g. after
                // a dropped connection hid the first response from
                // the client) returns the original result instead of
                // creating a second lot.
                return CropLotMapper.toResponse(existing.get());
            }
        }

        CropLot lot = requireOwnedLot(lotId, farmerPrincipal);

        validateVideoFile(video);
        validateGps(latitude, longitude);

        if (offlineCapture) {
            validateFreshness(capturedAt, MAX_OFFLINE_CAPTURE_AGE);
        } else {
            validateFreshness(capturedAt, MAX_CAPTURE_AGE);
        }

        Field field = requireOwnedField(fieldId, farmerPrincipal);

        byte[] videoBytes = readBytes(video);
        String videoHash = sha256Hex(videoBytes);

        String storedPath = fileStorageUtil.store(video, "crop-lots/" + lotId);
        lot.setVideoUrl(storedPath);
        lot.setVideoHash(videoHash);
        lot.setField(field);
        lot.setCaptureLatitude(latitude);
        lot.setCaptureLongitude(longitude);
        lot.setCapturedAt(capturedAt);
        lot.setWasOfflineCapture(offlineCapture);
        lot.setOfflineIdempotencyKey(offlineCapture ? offlineIdempotencyKey : null);
        // Stored verbatim, unvalidated here — no attempt is made to
        // parse or trust this string beyond what GpsTrackCodec parses
        // defensively (a malformed track degrades to NOT_AVAILABLE at
        // assessment time, it never breaks the upload itself).
        lot.setCaptureTrackEncoded(gpsTrackEncoded);

        evidenceAssessmentService.assess(lot);

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
        // Second, independent layer: the Content-Type header above is
        // client-supplied and can be spoofed. This checks the file's
        // actual binary signature — closes the previously flagged
        // "we trust the header" gap without removing the header check
        // (both layers together are stronger than either alone).
        try (java.io.InputStream in = video.getInputStream()) {
            if (!com.agribid.nexus.util.VideoFileSignatureValidator.isValidVideoContainer(in)) {
                throw new IllegalArgumentException(
                        "File content does not match a recognized video container format, regardless of its declared type");
            }
        } catch (java.io.IOException e) {
            throw new IllegalArgumentException("Could not read uploaded file to verify its format", e);
        }
    }

    private void validateGps(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            throw new IllegalArgumentException("GPS coordinates are required with every video submission");
        }
        if (latitude < -90 || latitude > 90 || longitude < -180 || longitude > 180) {
            throw new IllegalArgumentException("GPS coordinates out of valid range");
        }
        // Cross-checking against a registered Field (not just a raw
        // range check) now happens in EvidenceAssessmentService,
        // AFTER this method returns — that's a deliberate separation:
        // this method rejects outright on malformed input, while a
        // field MISMATCH is evidence to weigh, not a hard block, per
        // the "never hard-reject on a single signal" design rule.
    }

    private void validateFreshness(Instant capturedAt, Duration maxAge) {
        if (capturedAt == null) {
            throw new IllegalArgumentException("capturedAt timestamp is required");
        }
        Duration age = Duration.between(capturedAt, Instant.now());
        if (age.isNegative() || age.compareTo(maxAge) > 0) {
            throw new IllegalArgumentException(
                    "Video capture timestamp is stale or invalid — must be within " + maxAge.toHours() + " hour(s) of upload");
        }
    }

    private Field requireOwnedField(Long fieldId, UserPrincipal farmerPrincipal) {
        Field field = fieldRepository.findById(fieldId)
                .orElseThrow(() -> new ResourceNotFoundException("Field not found: " + fieldId));
        if (!field.getOwner().getId().equals(farmerPrincipal.getId())) {
            throw new UnauthorizedActionException("You do not own field " + fieldId);
        }
        return field;
    }

    private byte[] readBytes(MultipartFile video) {
        try {
            return video.getBytes();
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException("Failed to read uploaded video for hashing", e);
        }
    }

    private String sha256Hex(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(bytes);
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            // SHA-256 is a mandatory algorithm on every standard JVM —
            // this branch is unreachable in practice, but the checked
            // exception still has to go somewhere.
            throw new IllegalStateException("SHA-256 not available on this JVM", e);
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
