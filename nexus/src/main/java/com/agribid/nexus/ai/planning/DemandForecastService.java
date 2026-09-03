package com.agribid.nexus.ai.planning;

import com.agribid.nexus.ai.planning.model.CropRecommendationSet;
import com.agribid.nexus.ai.planning.model.DemandForecast;
import com.agribid.nexus.ai.util.LanguageInstructions;
import com.agribid.nexus.domain.crop.Category;
import com.agribid.nexus.domain.user.FarmerProfile;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CategoryRepository;
import com.agribid.nexus.repository.FarmerProfileRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * This is the direct answer to the "AgriDirect predicts what and how
 * much to grow, before harvest even happens" gap. Reuses the exact
 * retrieval discipline ReservePriceAdvisorService already
 * established (QuestionAnswerAdvisor pulling from the same Qdrant
 * store of mandi bulletins/MSP circulars) — the only thing that's
 * different is WHEN in the farmer's timeline it's invoked: before a
 * CropLot exists at all, rather than after harvest.
 */
@Service
public class DemandForecastService {

    private static final int FORECAST_TOP_K = 6;
    private static final int RECOMMENDATION_TOP_K = 10;
    private static final double SIMILARITY_THRESHOLD = 0.15;
    private static final int MAX_CANDIDATE_CATEGORIES = 15;

    private final ChatClient planningChatClient;
    private final VectorStore vectorStore;
    private final PromptTemplate forecastPromptTemplate;
    private final PromptTemplate recommendationPromptTemplate;
    private final CategoryRepository categoryRepository;
    private final FarmerProfileRepository farmerProfileRepository;

    public DemandForecastService(
            ChatClient planningChatClient,
            VectorStore vectorStore,
            CategoryRepository categoryRepository,
            FarmerProfileRepository farmerProfileRepository,
            @Value("classpath:prompts/demand-forecast-prompt.st") Resource forecastPromptResource,
            @Value("classpath:prompts/crop-recommendation-prompt.st") Resource recommendationPromptResource) {
        this.planningChatClient = planningChatClient;
        this.vectorStore = vectorStore;
        this.categoryRepository = categoryRepository;
        this.farmerProfileRepository = farmerProfileRepository;
        this.forecastPromptTemplate = new PromptTemplate(forecastPromptResource);
        this.recommendationPromptTemplate = new PromptTemplate(recommendationPromptResource);
    }

    @Transactional(readOnly = true)
    public DemandForecast forecastDemand(String categoryCode, String region, Long farmerId) {
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown category code: " + categoryCode));

        String languageInstruction = LanguageInstructions.instructionFor(resolveLanguage(farmerId));
        String marketQuery = "Historical mandi prices and MSP trend data for %s in %s across recent seasons"
                .formatted(category.getName(), region);

        String userMessage = forecastPromptTemplate.render(Map.of(
                "marketQuery", marketQuery,
                "cropType", category.getName(),
                "region", region,
                "languageInstruction", languageInstruction
        ));

        return planningChatClient.prompt()
                .advisors(retrievalAdvisor(FORECAST_TOP_K))
                .user(userMessage)
                .call()
                .entity(DemandForecast.class);
    }

    /**
     * Ranks up to MAX_CANDIDATE_CATEGORIES known crops for a region
     * in a single call rather than one call per category — cheaper,
     * and lets the model reason comparatively ("X looks stronger
     * than Y this season") instead of scoring each crop in
     * isolation.
     */
    @Transactional(readOnly = true)
    public CropRecommendationSet recommendCropsToGrow(String region, int topN, Long farmerId) {
        List<Category> candidates = categoryRepository.findAll().stream()
                .limit(MAX_CANDIDATE_CATEGORIES)
                .toList();

        String candidateList = candidates.stream()
                .map(c -> "%s (%s)".formatted(c.getCode(), c.getName()))
                .collect(Collectors.joining(", "));

        String languageInstruction = LanguageInstructions.instructionFor(resolveLanguage(farmerId));
        String marketQuery = "Historical mandi prices and MSP trend data for crops grown in %s across recent seasons"
                .formatted(region);

        String userMessage = recommendationPromptTemplate.render(Map.of(
                "marketQuery", marketQuery,
                "region", region,
                "candidateCategories", candidateList,
                "topN", Math.max(1, Math.min(topN, candidates.size())),
                "languageInstruction", languageInstruction
        ));

        return planningChatClient.prompt()
                .advisors(retrievalAdvisor(RECOMMENDATION_TOP_K))
                .user(userMessage)
                .call()
                .entity(CropRecommendationSet.class);
    }

    private QuestionAnswerAdvisor retrievalAdvisor(int topK) {
        return QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder()
                        .topK(topK)
                        .similarityThreshold(SIMILARITY_THRESHOLD)
                        .build())
                .build();
    }

    /**
     * farmerId is optional (e.g. an unauthenticated/agronomist caller
     * exploring general trends) — falls back to English rather than
     * failing the whole forecast over a missing profile lookup.
     */
    private String resolveLanguage(Long farmerId) {
        if (farmerId == null) {
            return "en";
        }
        return farmerProfileRepository.findById(farmerId)
                .map(FarmerProfile::getPreferredLanguage)
                .orElse("en");
    }
}
