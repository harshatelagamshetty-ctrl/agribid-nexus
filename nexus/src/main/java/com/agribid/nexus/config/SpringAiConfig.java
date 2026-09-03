package com.agribid.nexus.config;

import com.agribid.nexus.ai.tools.FulfillmentMatchTool;
import com.agribid.nexus.ai.tools.MspLookupTool;
import com.agribid.nexus.ai.tools.RouteOptimizationTool;
import com.agribid.nexus.ai.tools.TaxCalculationTool;
import com.agribid.nexus.ai.tools.WarehouseCapacityTool;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.repository.jdbc.JdbcChatMemoryRepository;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Gemini's model/temperature/api-key are configured declaratively in
 * application.properties (spring.ai.google.genai.*) and auto-wired
 * by Spring AI's auto-configuration — this class only defines the
 * ChatClient beans built on top of that auto-configured model, since
 * different callers (crop grading, reserve-price advisor, negotiation
 * chat) need different default system prompts and advisors.
 */
@Configuration
public class SpringAiConfig {

    /**
     * Plain ChatClient with no default advisors — used by
     * CropGradingService, which supplies its own system prompt per
     * call and doesn't need conversational memory (each grading
     * request is a one-shot image-in/structured-output-out call).
     */
    @Bean
    public ChatClient visionChatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Backs the RAG-grounded reserve-price advisor. No chat memory
     * here either — each reserve-price request is independent; the
     * QuestionAnswerAdvisor itself is attached per-call in
     * ReservePriceAdvisorService since it needs a request-specific
     * SearchRequest (topK, similarityThreshold).
     */
    @Bean
    public ChatClient pricingChatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * Backs the pre-harvest demand forecast / crop recommendation
     * service (ai/planning). Deliberately a separate bean from
     * pricingChatClient even though the configuration is identical —
     * they're conceptually different callers (post-harvest pricing
     * vs. pre-harvest planning) and keeping them distinct means
     * either one's defaults can diverge later (e.g. a higher
     * temperature for exploratory crop suggestions) without touching
     * the other.
     */
    @Bean
    public ChatClient planningChatClient(GoogleGenAiChatModel chatModel) {
        return ChatClient.builder(chatModel).build();
    }

    /**
     * The multi-turn negotiation co-pilot. This is the one ChatClient
     * that needs persistent memory — a distributor's strategic
     * reasoning across several messages in one auction session
     * shouldn't reset between HTTP requests.
     *
     * ToolCallbackProvider (the interface) is what's actually
     * injectable here — the concrete SyncMcpToolCallbackProvider
     * class does NOT resolve as an autowire candidate from the MCP
     * client auto-configuration (see spring-projects/spring-ai#2543).
     * defaultTools() accepts a ToolCallbackProvider directly, so no
     * manual .getToolCallbacks() unwrapping is needed either.
     */
    @Bean
    public ChatClient negotiationChatClient(
            GoogleGenAiChatModel chatModel,
            ChatMemory chatMemory,
            TaxCalculationTool taxCalculationTool,
            MspLookupTool mspLookupTool,
            WarehouseCapacityTool warehouseCapacityTool,
            FulfillmentMatchTool fulfillmentMatchTool,
            RouteOptimizationTool routeOptimizationTool,
            ToolCallbackProvider mcpToolCallbackProvider,
            @Value("classpath:prompts/negotiation-system-prompt.st") Resource negotiationSystemPromptResource) throws IOException {

        String systemPrompt = negotiationSystemPromptResource.getContentAsString(StandardCharsets.UTF_8);

        return ChatClient.builder(chatModel)
                .defaultSystem(systemPrompt)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultTools(taxCalculationTool, mspLookupTool, warehouseCapacityTool, fulfillmentMatchTool, routeOptimizationTool)
                .defaultTools(mcpToolCallbackProvider)
                .build();
    }

    /**
     * Injects the ALREADY auto-configured JdbcChatMemoryRepository
     * rather than building a new one via .builder()...build().
     *
     * This matters more than it looks: Spring AI's own
     * JdbcChatMemoryRepositoryAutoConfiguration creates its own
     * JdbcChatMemoryRepository bean AND automatically runs the schema
     * script that creates the SPRING_AI_CHAT_MEMORY table — but only
     * for the repository instance IT constructs. Manually building a
     * second, separate JdbcChatMemoryRepository here (as an earlier
     * version of this file did) bypasses that automatic schema
     * initialization entirely, since the auto-configuration has no
     * visibility into an instance built outside of it. The result was
     * a repository that queries a table that was never created —
     * exactly the BadSqlGrammarException this fixes.
     */
    @Bean
    public ChatMemory chatMemory(JdbcChatMemoryRepository jdbcChatMemoryRepository) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(jdbcChatMemoryRepository)
                .maxMessages(20)
                .build();
    }
}