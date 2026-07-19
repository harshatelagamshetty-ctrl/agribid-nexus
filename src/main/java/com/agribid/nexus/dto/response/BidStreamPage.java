package com.agribid.nexus.dto.response;

import java.time.Instant;
import java.util.List;

/**
 * Cursor-based page for the live bid-stream endpoint. nextCursor is
 * the bidTimestamp of the oldest bid in this page — the client
 * passes it back as lastTimestamp on the next call. hasMore lets the
 * client stop polling deeper without an extra count query.
 */
public record BidStreamPage(
    List<BidResponse> bids,
    Instant nextCursor,
    boolean hasMore
) {
}