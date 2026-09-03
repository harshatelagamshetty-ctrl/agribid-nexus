package com.agribid.nexus.service;

import com.agribid.nexus.dto.request.CropLotCreateRequest;
import com.agribid.nexus.dto.response.CropLotResponse;
import com.agribid.nexus.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.time.Instant;

public interface CropLotService {

    CropLotResponse createLot(CropLotCreateRequest request, UserPrincipal farmer);

    /**
     * fieldId links this submission to a Field the farmer registered
     * via FieldService — required so EvidenceAssessmentService can
     * check the video's GPS against a known, registered location
     * rather than trusting raw GPS in isolation.
     */
    CropLotResponse attachVideo(Long lotId, MultipartFile video, Long fieldId, Double latitude, Double longitude,
                                 Instant capturedAt, String gpsTrackEncoded, boolean offlineCapture,
                                 String offlineIdempotencyKey, UserPrincipal farmer);

    Page<CropLotResponse> getLotsForFarmer(Long farmerId, Pageable pageable);

    CropLotResponse getLot(Long lotId);
}