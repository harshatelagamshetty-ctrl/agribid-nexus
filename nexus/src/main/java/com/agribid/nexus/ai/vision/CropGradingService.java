package com.agribid.nexus.ai.vision;

import com.agribid.nexus.ai.vision.model.CropGradeAssessment;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.PestTag;
import com.agribid.nexus.domain.crop.QualityGrade;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.PestTagRepository;
import com.agribid.nexus.repository.QualityGradeRepository;
import com.agribid.nexus.util.FileStorageUtil;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.MimeTypeUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Grades from a video walkthrough of the whole lot, not a single
 * staged photo — this is the direct fix for the "farmer photographs
 * only the good corner" gap. Gemini's multimodal API accepts video
 * natively (it samples frames across the clip internally) — no
 * manual frame-extraction/FFmpeg step needed on our side.
 *
 * Honest limitation: this widens the evidence surface (harder to
 * stage a whole video than one photo) but doesn't make gaming
 * impossible. The actual enforcement layer is delivery-time
 * re-verification against this same grade — that's a separate
 * correction, not something this class does.
 */
@Service
public class CropGradingService {

    private final ChatClient visionChatClient;
    private final CropLotRepository cropLotRepository;
    private final QualityGradeRepository qualityGradeRepository;
    private final PestTagRepository pestTagRepository;
    private final FileStorageUtil fileStorageUtil;
    private final PromptTemplate gradingPromptTemplate;
    private final com.agribid.nexus.ai.regional.RegionalSignalAggregationService regionalSignalAggregationService;
    private final com.agribid.nexus.repository.CropLotEvidenceReportRepository evidenceReportRepository;

    public CropGradingService(
            ChatClient visionChatClient,
            CropLotRepository cropLotRepository,
            QualityGradeRepository qualityGradeRepository,
            PestTagRepository pestTagRepository,
            FileStorageUtil fileStorageUtil,
            @Value("classpath:prompts/crop-grading-prompt.st") Resource gradingPromptResource,
            com.agribid.nexus.ai.regional.RegionalSignalAggregationService regionalSignalAggregationService,
            com.agribid.nexus.repository.CropLotEvidenceReportRepository evidenceReportRepository) {
        this.visionChatClient = visionChatClient;
        this.cropLotRepository = cropLotRepository;
        this.qualityGradeRepository = qualityGradeRepository;
        this.pestTagRepository = pestTagRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.gradingPromptTemplate = new PromptTemplate(gradingPromptResource);
        this.regionalSignalAggregationService = regionalSignalAggregationService;
        this.evidenceReportRepository = evidenceReportRepository;
    }

    @Transactional
    public CropLot gradeCropLot(Long lotId) {
        CropLot lot = cropLotRepository.findById(lotId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + lotId));

        if (lot.getVideoUrl() == null) {
            throw new IllegalStateException("Crop lot " + lotId + " has no attached video to grade");
        }

        CropGradeAssessment assessment = gradeVideo(
                fileStorageUtil.loadBytes(lot.getVideoUrl()),
                lot.getCategory() != null ? lot.getCategory().getName() : "unknown crop"
        );

        QualityGrade grade = new QualityGrade(
                assessment.qualityGrade(),
                assessment.estimatedShelfLifeDays(),
                assessment.confidenceScore()
        );
        qualityGradeRepository.save(grade);

        Set<PestTag> tags = resolveOrCreatePestTags(assessment.detectedPestTags());

        lot.applyGrading(grade, tags);

        // The entire AgriPulse trust-filtering mechanism is this one
        // check. A LOW or NEEDS_REVIEW submission simply never calls
        // recordVerifiedSubmission() — it cannot distort the regional
        // signal because the code path to reach it doesn't run, not
        // because of a business rule that could be bypassed later.
        evidenceReportRepository.findByCropLotId(lot.getId()).ifPresent(report -> {
            var tier = report.getOverallEvidence();
            if (tier == com.agribid.nexus.ai.evidence.model.OverallEvidence.HIGH
                    || tier == com.agribid.nexus.ai.evidence.model.OverallEvidence.MEDIUM) {
                regionalSignalAggregationService.recordVerifiedSubmission(lot, report);
            }
        });

        return lot;
    }

    private CropGradeAssessment gradeVideo(byte[] videoBytes, String cropTypeName) {
        Media media = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType("video/mp4"))
                .data(new ByteArrayResource(videoBytes))
                .build();

        String renderedPrompt = gradingPromptTemplate.render(Map.of("cropType", cropTypeName));

        return visionChatClient.prompt()
                .user(u -> u.text(renderedPrompt).media(media))
                .call()
                .entity(CropGradeAssessment.class);
    }

    private Set<PestTag> resolveOrCreatePestTags(Iterable<String> detectedLabels) {
        Set<PestTag> resolved = new HashSet<>();
        for (String label : detectedLabels) {
            String code = label.trim().toUpperCase().replace(' ', '_');
            PestTag tag = pestTagRepository.findByCode(code)
                    .orElseGet(() -> pestTagRepository.save(new PestTag(code, label.trim(), null)));
            resolved.add(tag);
        }
        return resolved;
    }
}