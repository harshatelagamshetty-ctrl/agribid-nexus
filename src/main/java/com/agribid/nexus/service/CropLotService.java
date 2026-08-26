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

    CropLotResponse attachVideo(Long lotId, MultipartFile video, Double latitude, Double longitude, Instant capturedAt, UserPrincipal farmer);

    Page<CropLotResponse> getLotsForFarmer(Long farmerId, Pageable pageable);

    CropLotResponse getLot(Long lotId);
}