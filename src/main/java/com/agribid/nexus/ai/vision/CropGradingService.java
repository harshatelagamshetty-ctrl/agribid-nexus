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
 * The moment Gemini's vision response comes back, it's mapped via
 * .entity(CropGradeAssessment.class) into a strongly-typed record and
 * immediately persisted as QualityGrade + PestTag associations —
 * there is no point at which raw, untyped AI text is stored or
 * displayed anywhere in the system.
 *
 * The grading prompt is externalized to
 * resources/prompts/crop-grading-prompt.st (versioned like code,
 * reusable across callers) rather than an inline Java string —
 * loaded once at construction via @Value("classpath:...") Resource
 * injection.
 */
@Service
public class CropGradingService {

    private final ChatClient visionChatClient;
    private final CropLotRepository cropLotRepository;
    private final QualityGradeRepository qualityGradeRepository;
    private final PestTagRepository pestTagRepository;
    private final FileStorageUtil fileStorageUtil;
    private final PromptTemplate gradingPromptTemplate;

    public CropGradingService(
            ChatClient visionChatClient,
            CropLotRepository cropLotRepository,
            QualityGradeRepository qualityGradeRepository,
            PestTagRepository pestTagRepository,
            FileStorageUtil fileStorageUtil,
            @Value("classpath:prompts/crop-grading-prompt.st") Resource gradingPromptResource) {
        this.visionChatClient = visionChatClient;
        this.cropLotRepository = cropLotRepository;
        this.qualityGradeRepository = qualityGradeRepository;
        this.pestTagRepository = pestTagRepository;
        this.fileStorageUtil = fileStorageUtil;
        this.gradingPromptTemplate = new PromptTemplate(gradingPromptResource);
    }

    @Transactional
    public CropLot gradeCropLot(Long lotId) {
        CropLot lot = cropLotRepository.findById(lotId)
            .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + lotId));

        if (lot.getImageUrl() == null) {
            throw new IllegalStateException("Crop lot " + lotId + " has no attached image to grade");
        }

        CropGradeAssessment assessment = gradeImage(
            fileStorageUtil.loadBytes(lot.getImageUrl()),
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
        return lot;
    }

    private CropGradeAssessment gradeImage(byte[] imageBytes, String cropTypeName) {
        Media media = Media.builder()
            .mimeType(MimeTypeUtils.IMAGE_JPEG)
            .data(new ByteArrayResource(imageBytes))
            .build();

        String renderedPrompt = gradingPromptTemplate.render(Map.of("cropType", cropTypeName));

        return visionChatClient.prompt()
            .user(u -> u.text(renderedPrompt).media(media))
            .call()
            .entity(CropGradeAssessment.class);
    }

    /**
     * Pest tags come back from Gemini as free-text labels
     * (detectedPestTags), so they're normalized against the existing
     * PestTag reference table by code rather than blindly inserting
     * whatever string the model returned — this keeps the
     * crop_lot_pest_tag join table queryable/consistent instead of
     * accumulating near-duplicate tag rows from minor wording
     * variation across grading calls.
     */
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
