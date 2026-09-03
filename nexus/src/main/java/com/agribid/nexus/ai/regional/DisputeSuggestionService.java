package com.agribid.nexus.ai.regional;

import com.agribid.nexus.domain.contract.Dispute;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.DisputeRepository;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.vectorstore.QuestionAnswerAdvisor;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

/**
 * AI-ASSISTED, never AI-DECIDED — the name is deliberate. This
 * retrieves genuinely similar past resolved disputes (see
 * DisputeService.recordDecision(), which is what actually populates
 * this corpus) and drafts a suggestion an agronomist can accept,
 * modify, or ignore entirely. The binding decision always still goes
 * through DisputeService's normal PENDING -> APPROVED/REJECTED flow;
 * this service cannot change a dispute's status itself.
 *
 * Honest limitation: for the first several disputes ever resolved on
 * this platform, the corpus is small or empty, and the model is
 * instructed to say so rather than invent a plausible-sounding
 * precedent that doesn't exist.
 */
@Service
public class DisputeSuggestionService {

    private final ChatClient pricingChatClient; // reused, not a new AI bean — same model, different prompt
    private final VectorStore vectorStore;
    private final DisputeRepository disputeRepository;

    public DisputeSuggestionService(ChatClient pricingChatClient, VectorStore vectorStore, DisputeRepository disputeRepository) {
        this.pricingChatClient = pricingChatClient;
        this.vectorStore = vectorStore;
        this.disputeRepository = disputeRepository;
    }

    public record DisputeSuggestion(Long disputeId, String suggestion) {}

    public DisputeSuggestion getSuggestion(Long disputeId) {
        Dispute dispute = disputeRepository.findById(disputeId)
                .orElseThrow(() -> new ResourceNotFoundException("Dispute not found: " + disputeId));

        QuestionAnswerAdvisor retrievalAdvisor = QuestionAnswerAdvisor.builder(vectorStore)
                .searchRequest(SearchRequest.builder().topK(3).similarityThreshold(0.5).build())
                .build();

        String prompt = """
                An agronomist is reviewing this dispute: "%s"
                Based only on similar past resolved disputes retrieved as context, suggest how this
                one might reasonably be resolved. If no genuinely similar past dispute is available
                in the retrieved context, say exactly that — do not invent a precedent. This is an
                advisory suggestion only; the agronomist makes the actual binding decision.
                """.formatted(dispute.getReason());

        String suggestion = pricingChatClient.prompt()
                .advisors(a -> a.advisors(retrievalAdvisor))
                .user(prompt)
                .call()
                .content();

        return new DisputeSuggestion(disputeId, suggestion);
    }
}
