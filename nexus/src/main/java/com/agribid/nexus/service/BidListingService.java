package com.agribid.nexus.service;

import com.agribid.nexus.dto.request.BidListingCreateRequest;
import com.agribid.nexus.dto.request.ListingFilterRequest;
import com.agribid.nexus.dto.response.BidListingResponse;
import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.security.UserPrincipal;
import org.springframework.data.domain.Page;

public interface BidListingService {

    BidListingResponse publishListing(BidListingCreateRequest request, UserPrincipal farmer);

    Page<BidListingResponse> search(ListingFilterRequest filter);

    BidListingResponse getListing(Long listingId);

    /**
     * Converts the current highest bid on a CLOSED listing into a
     * ForwardContract. Enforced idempotent by the OneToOne unique
     * constraint on ForwardContract.sourceListing — calling this
     * twice on the same listing throws rather than creating a
     * second competing contract.
     */
    ForwardContractResponse convertToContract(Long listingId, UserPrincipal farmer);
}
