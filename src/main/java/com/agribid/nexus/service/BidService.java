package com.agribid.nexus.service;

import com.agribid.nexus.dto.request.BidRequest;
import com.agribid.nexus.dto.response.BidResponse;
import com.agribid.nexus.dto.response.BidStreamPage;
import com.agribid.nexus.security.UserPrincipal;

import java.time.Instant;

public interface BidService {

    BidResponse placeBid(BidRequest request, UserPrincipal distributor);

    /**
     * Keyset-paginated bid stream. lastTimestamp is the cursor from
     * the previous page's BidStreamPage.nextCursor(); null fetches
     * the most recent page.
     */
    BidStreamPage streamBids(Long listingId, Instant lastTimestamp, int pageSize);
}
