package com.agribid.nexus.controller;

import com.agribid.nexus.ai.regional.VoiceTranscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final VoiceTranscriptionService voiceTranscriptionService;

    /**
     * Returns a draft only — the farmer must review and confirm
     * before POST /api/v1/crop-lots actually creates anything. See
     * VoiceTranscriptionService's class-level Javadoc for why this
     * is deliberately not a one-step "speak and it's listed" flow.
     */
    @PostMapping(value = "/crop-lot-draft", consumes = "multipart/form-data")
    @PreAuthorize("hasRole('FARMER')")
    public ResponseEntity<VoiceTranscriptionService.VoiceLotDraft> transcribeLotDraft(
            @RequestParam("audio") MultipartFile audio,
            @RequestParam(value = "languageHint", required = false) String languageHint) throws IOException {
        return ResponseEntity.ok(voiceTranscriptionService.transcribeToLotDraft(audio.getBytes(), languageHint));
    }
}
