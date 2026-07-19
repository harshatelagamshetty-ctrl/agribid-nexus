package com.agribid.nexus.scheduler;

import com.agribid.nexus.domain.auction.AuctionStatus;
import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.repository.BidListingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

/**
 * Without this job, no BidListing ever transitions OPEN -> CLOSED,
 * and BidListingServiceImpl.convertToContract() hard-requires CLOSED
 * status before creating a ForwardContract — meaning the entire
 * auction-to-contract flow silently dead-ends without this class
 * running. fixedRate (not fixedDelay) is used deliberately: we want
 * a consistent close-check cadence regardless of how long each run
 * takes, since a slow run shouldn't push subsequent closes later and
 * later.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuctionCloseScheduler {

    private final BidListingRepository bidListingRepository;

    @Scheduled(fixedRateString = "${agribid.scheduler.auction-close-check-rate-ms:30000}")
    public void closeExpiredAuctions() {
        List<BidListing> expired = bidListingRepository.findByStatusAndAuctionCloseTimeBefore(
            AuctionStatus.OPEN, Instant.now());

        if (expired.isEmpty()) {
            return;
        }

        for (BidListing listing : expired) {
            try {
                closeOne(listing.getId());
            } catch (ObjectOptimisticLockingFailureException ex) {
                // A bid landed in the exact window between the query above
                // and this write — deliberately NOT retried within this
                // run. Leaving it OPEN means it's simply picked up again on
                // the next scheduled pass with fresh state, rather than us
                // racing a legitimate last-second bid to force a close.
                log.warn("Listing {} changed underneath the close job — will retry next run", listing.getId());
            }
        }
    }

    /**
     * Each listing closes in its own transaction, isolated from the
     * others in the batch. Without this separation, one listing's
     * optimistic-lock conflict inside a single shared @Transactional
     * method would roll back every other listing already closed in
     * the same batch — an unrelated race on listing #7 would silently
     * undo listings #1 through #6.
     */
    @Transactional
    public void closeOne(Long listingId) {
        bidListingRepository.findById(listingId).ifPresent(listing -> {
            listing.setStatus(AuctionStatus.CLOSED);
            bidListingRepository.saveAndFlush(listing);
            log.info("Closed auction for listing {} (auctionCloseTime={})", listing.getId(), listing.getAuctionCloseTime());
        });
    }
}
