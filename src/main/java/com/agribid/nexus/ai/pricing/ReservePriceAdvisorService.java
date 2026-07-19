package com.agribid.nexus.ai.pricing;

import com.agribid.nexus.ai.pricing.model.ReservePriceSuggestion;
import com.agribid.nexus.domain.crop.CropLot;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.Map;

/**
 * Every reserve-price recommendation is retrieval-grounded: before
 * Gemini generates a single word, a QuestionAnswerAdvisor pulls top-k
 * semantically similar chunks from the MSP circulars and mandi
 * bulletins that MarketDocumentIngestor put into Qdrant. The model is
 * synthesizing an answer from retrieved, citable context — never
 * pricing a crop from parametric memory.
 *
 * NOTE: QuestionAnswerAdvisor moved into its own artifact
 * (spring-ai-vector-store-advisor) as of Spring AI 2.0 rather than
 * living directly in spring-ai-core — already reflected in pom.xml.
 */
@Service
public class ReservePriceAdvisorService {

    private static final int TOP_K = 5;
    private static final double SIMILARITY_THRESHOLD = 0.75;

    private final ChatClient pricingChatClient;
    private final VectorStore vectorStore;
    private final PromptTemplate reservePricePromptTemplate;

    public ReservePriceAdvisorService(
            ChatClient pricingChatClient,
            VectorStore vectorStore,
            @Value("classpath:prompts/reserve-price-prompt.st") Resource reservePricePromptResource) {
        this.pricingChatClient = pricingChatClient;
        this.vectorStore = vectorStore;
        this.reservePricePromptTemplate = new PromptTemplate(reservePricePromptResource);
    }

    public ReservePriceSuggestion suggestReservePrice(CropLot lot) {
        String cropName = lot.getCategory() != null ? lot.getCategory().getName() : "this crop";
        String district = lot.getOwner().getDistrict();
        String marketQuery = "Historical mandi prices and MSP data for %s in %s".formatted(cropName, district);

        QuestionAnswerAdvisor retrievalAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
            .searchRequest(SearchRequest.builder()
                .topK(TOP_K)
                .similarityThreshold(SIMILARITY_THRESHOLD)
                .build())
            .build();

        String userMessage = reservePricePromptTemplate.render(Map.of(
            "marketQuery", marketQuery,
            "quantityKg", lot.getQuantityKg(),
            "qualityGrade", lot.getQualityGrade() != null ? lot.getQualityGrade().getGradeLabel() : "ungraded"
        ));

        return pricingChatClient.prompt()
            .advisors(retrievalAdvisor)
            .user(userMessage)
            .call()
            .entity(ReservePriceSuggestion.class);
    }
}
