package com.agribid.nexus.ai.negotiation;

import com.agribid.nexus.ai.negotiation.model.NegotiationMessage;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

/**
 * The negotiationChatClient bean (see config/SpringAiConfig) already
 * has MessageChatMemoryAdvisor and all four @Tool beans registered
 * as defaults — this service's only job is scoping each call to the
 * right conversation, so a distributor's multi-turn reasoning about
 * one auction doesn't bleed into another auction's session.
 */
@Service
public class NegotiationChatService {

    private final ChatClient negotiationChatClient;

    public NegotiationChatService(ChatClient negotiationChatClient) {
        this.negotiationChatClient = negotiationChatClient;
    }

    public NegotiationMessage send(Long listingId, Long distributorId, String userMessage) {
        String conversationId = "listing-%d-distributor-%d".formatted(listingId, distributorId);

        String reply = negotiationChatClient.prompt()
            .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
            .user(userMessage)
            .call()
            .content();

        return new NegotiationMessage(conversationId, reply);
    }
}
