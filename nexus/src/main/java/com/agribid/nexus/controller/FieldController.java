package com.agribid.nexus.controller;

import com.agribid.nexus.dto.request.FieldRegisterRequest;
import com.agribid.nexus.dto.response.FieldResponse;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.FieldService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Field registration is a one-time (or occasional) action per
 * physical plot — a farmer registers a field once, then reuses it
 * across many crop lots' video submissions. This is intentionally
 * its own small controller rather than nested under crop-lots, since
 * a Field's lifecycle is independent of any single lot.
 */
@RestController
@RequestMapping("/api/v1/fields")
@RequiredArgsConstructor
public class FieldController {

    private final FieldService fieldService;

    @PostMapping
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<FieldResponse> registerField(
            @Valid @RequestBody FieldRegisterRequest request,
            @AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fieldService.registerField(request, farmer));
    }

    @GetMapping("/mine")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<List<FieldResponse>> getMyFields(@AuthenticationPrincipal UserPrincipal farmer) {
        return ResponseEntity.ok(fieldService.getMyFields(farmer));
    }
}
