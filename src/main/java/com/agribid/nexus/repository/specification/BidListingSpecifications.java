package com.agribid.nexus.repository.specification;

import com.agribid.nexus.domain.auction.AuctionStatus;
import com.agribid.nexus.domain.auction.BidListing;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Composable Specification builders for BidListing search. Each
 * filter is expressed once and combined at runtime via
 * Specification.where(...).and(...), avoiding a combinatorial
 * explosion of repository finder methods for every filter combo.
 */
public final class BidListingSpecifications {

    private BidListingSpecifications() {
    }

    public static Specification<BidListing> hasCropType(String categoryCode) {
        return (root, query, cb) -> categoryCode == null ? null :
                cb.equal(root.get("cropLot").get("category").get("code"), categoryCode);
    }

    public static Specification<BidListing> minQuantity(BigDecimal minKg) {
        return (root, query, cb) -> minKg == null ? null :
                cb.greaterThanOrEqualTo(root.get("cropLot").get("quantityKg"), minKg);
    }

    public static Specification<BidListing> inDistrict(String district) {
        return (root, query, cb) -> district == null ? null :
                cb.equal(root.get("cropLot").get("owner").get("district"), district);
    }

    public static Specification<BidListing> closingBefore(Instant deadline) {
        return (root, query, cb) -> deadline == null ? null :
                cb.lessThanOrEqualTo(root.get("auctionCloseTime"), deadline);
    }

    public static Specification<BidListing> hasStatus(AuctionStatus status) {
        return (root, query, cb) -> status == null ? null :
                cb.equal(root.get("status"), status);
    }
}