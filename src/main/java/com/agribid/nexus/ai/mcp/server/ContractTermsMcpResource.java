package com.agribid.nexus.ai.mcp.server;

import com.agribid.nexus.dto.mapper.ForwardContractMapper;
import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.ForwardContractRepository;
import io.modelcontextprotocol.spec.McpSchema.ReadResourceResult;
import io.modelcontextprotocol.spec.McpSchema.TextResourceContents;
import org.springframework.ai.mcp.annotation.McpArg;
import org.springframework.ai.mcp.annotation.McpResource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Exposes ForwardContract terms as an MCP Resource rather than a
 * Tool — resources are the right MCP primitive for "fetch this piece
 * of data by identifier" (as opposed to Tools, which are for
 * actions/computations). An external logistics-partner agent can
 * read contract-terms://{contractId} the same way it would read any
 * other MCP resource, with no bespoke API contract on our side.
 */
@Component
public class ContractTermsMcpResource {
    
    private final ForwardContractRepository forwardContractRepository;

    // Simple hand-rolled JSON serialization to avoid pulling an extra
    // ObjectMapper dependency into this narrowly-scoped class; swap
    // for a real Jackson/Gson serializer if the response shape grows.
    public ContractTermsMcpResource(ForwardContractRepository forwardContractRepository) {
        this.forwardContractRepository = forwardContractRepository;
    }

    @McpResource(uri = "contract-terms://{contractId}", name = "Forward Contract Terms")
    public ReadResourceResult getContractTerms(
            @McpArg(name = "contractId", description = "The forward contract's numeric ID", required = true) String contractId) {

        var contract = forwardContractRepository.findById(Long.valueOf(contractId))
            .orElseThrow(() -> new ResourceNotFoundException("Contract not found: " + contractId));

        ForwardContractResponse response = ForwardContractMapper.toResponse(contract);

        String json = """
            {"id":%d,"sourceListingId":%d,"lockedPrice":%s,"deliveryDeadline":"%s","status":"%s"}
            """.formatted(
                response.id(), response.sourceListingId(), response.lockedPrice(),
                response.deliveryDeadline(), response.status()
            ).strip();


        return ReadResourceResult.builder(
                List.of(
                        new TextResourceContents(
                                "contract-terms://" + contractId,
                                "application/json",
                                json
                        )
                )
        ).build();
    }
}