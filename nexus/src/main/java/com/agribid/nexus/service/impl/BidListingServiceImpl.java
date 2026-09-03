package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.auction.AuctionStatus;
import com.agribid.nexus.domain.auction.Bid;
import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.domain.contract.ForwardContract;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.crop.LotStatus;
import com.agribid.nexus.dto.mapper.BidListingMapper;
import com.agribid.nexus.dto.mapper.ForwardContractMapper;
import com.agribid.nexus.dto.request.BidListingCreateRequest;
import com.agribid.nexus.dto.request.ListingFilterRequest;
import com.agribid.nexus.dto.response.BidListingResponse;
import com.agribid.nexus.dto.response.ForwardContractResponse;
import com.agribid.nexus.exception.AuctionClosedException;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.exception.UnauthorizedActionException;
import com.agribid.nexus.repository.BidListingRepository;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.ForwardContractRepository;
import com.agribid.nexus.repository.specification.BidListingSpecifications;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.BidListingService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneOffset;

@Service
@RequiredArgsConstructor
public class BidListingServiceImpl implements BidListingService {

    private static final int DEFAULT_DELIVERY_WINDOW_DAYS = 14;

    private final BidListingRepository bidListingRepository;
    private final CropLotRepository cropLotRepository;
    private final ForwardContractRepository forwardContractRepository;
    private final com.agribid.nexus.repository.CropLotEvidenceReportRepository evidenceReportRepository;
    private final com.agribid.nexus.ai.regional.RegionalSignalAggregationService regionalSignalAggregationService;

    @Override
    @Transactional
    public BidListingResponse publishListing(BidListingCreateRequest request, UserPrincipal farmerPrincipal) {
        CropLot lot = cropLotRepository.findById(request.cropLotId())
            .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + request.cropLotId()));

        if (!lot.getOwner().getId().equals(farmerPrincipal.getId())) {
            throw new UnauthorizedActionException("You do not own crop lot " + request.cropLotId());
        }

        if (lot.getStatus() != LotStatus.GRADED) {
            throw new IllegalStateException("Crop lot " + lot.getId() + " must be GRADED before listing (current status: " + lot.getStatus() + ")");
        }

        // The actual enforcement point for the whole evidence engine:
        // grading itself is never blocked (a farmer can always see
        // their AI quality grade), but a lot whose evidence report
        // came out NEEDS_REVIEW or LOW cannot reach real distributors
        // until an agronomist has explicitly approved it. HIGH and
        // MEDIUM evidence lots (the large majority) pass through here
        // with zero added friction.
        evidenceReportRepository.findByCropLotId(lot.getId()).ifPresent(report -> {
            var status = report.getReviewStatus();
            if (status == com.agribid.nexus.ai.evidence.model.ReviewStatus.PENDING) {
                throw new IllegalStateException(
                        "Crop lot " + lot.getId() + " is awaiting agronomist review before it can be listed "
                        + "(evidence tier: " + report.getOverallEvidence() + ")");
            }
            if (status == com.agribid.nexus.ai.evidence.model.ReviewStatus.REJECTED) {
                throw new IllegalStateException(
                        "Crop lot " + lot.getId() + " was rejected during evidence review and cannot be listed. "
                        + "Attach a new video to request re-assessment.");
            }
        });

        BidListing listing = new BidListing(lot, request.reservePrice(), java.time.Instant.now(), request.auctionCloseTime());
        lot.setStatus(LotStatus.LISTED);
        bidListingRepository.save(listing);

        return BidListingMapper.toResponse(listing);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<BidListingResponse> search(ListingFilterRequest filter) {
        Specification<BidListing> spec = Specification
            .where(BidListingSpecifications.hasCropType(filter.cropTypeCode()))
            .and(BidListingSpecifications.minQuantity(filter.minQuantityKg()))
            .and(BidListingSpecifications.inDistrict(filter.district()))
            .and(BidListingSpecifications.closingBefore(filter.closingBefore()))
            .and(BidListingSpecifications.hasStatus(filter.status()));

        PageRequest pageRequest = PageRequest.of(
            filter.page(), filter.size(), Sort.by(Sort.Direction.DESC, "currentHighestBid"));

        return bidListingRepository.findAll(spec, pageRequest).map(BidListingMapper::toResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public BidListingResponse getListing(Long listingId) {
        return BidListingMapper.toResponse(findListingOrThrow(listingId));
    }

    /**
     * The ForwardContract's OneToOne + unique constraint on
     * sourceListing_id is the actual atomicity guarantee here — even
     * if this method were somehow invoked twice concurrently for the
     * same listing, the second insert would fail at the database
     * constraint level, not merely be prevented by this in-memory
     * status check (which is still the first line of defense for the
     * common, non-racing case).
     */
    @Override
    @Transactional
    public ForwardContractResponse convertToContract(Long listingId, UserPrincipal farmerPrincipal) {
        BidListing listing = findListingOrThrow(listingId);

        if (!listing.getCropLot().getOwner().getId().equals(farmerPrincipal.getId())) {
            throw new UnauthorizedActionException("You do not own the crop lot behind listing " + listingId);
        }

        if (listing.getStatus() != AuctionStatus.CLOSED) {
            throw new AuctionClosedException("Listing " + listingId + " must be CLOSED before contract conversion (current status: " + listing.getStatus() + ")");
        }

        if (forwardContractRepository.findBySourceListingId(listingId).isPresent()) {
            throw new IllegalStateException("Listing " + listingId + " has already been converted to a contract");
        }

        Bid winningBid = listing.getBids().stream()
            .max((a, b) -> a.getAmount().compareTo(b.getAmount()))
            .orElseThrow(() -> new IllegalStateException("Listing " + listingId + " closed with no bids — cannot convert to contract"));

        LocalDate deliveryDeadline = LocalDate.now(ZoneOffset.UTC).plusDays(DEFAULT_DELIVERY_WINDOW_DAYS);
        ForwardContract contract = new ForwardContract(listing, winningBid.getAmount(), deliveryDeadline);

        forwardContractRepository.save(contract);
        listing.setStatus(AuctionStatus.CONTRACTED);
        listing.getCropLot().setStatus(LotStatus.SOLD);

        // Feeds AgriPulse's regional price benchmark — only once a
        // price is genuinely settled (a real winning bid locked into
        // a contract), never a reserve price or an in-progress bid.
        regionalSignalAggregationService.recordSettledPrice(listing.getCropLot(), winningBid.getAmount());

        return ForwardContractMapper.toResponse(contract);
    }

    private BidListing findListingOrThrow(Long listingId) {
        return bidListingRepository.findById(listingId)
            .orElseThrow(() -> new ResourceNotFoundException("Listing not found: " + listingId));
    }
}
