package com.agribid.nexus.service.impl;

import com.agribid.nexus.domain.auction.BidListing;
import com.agribid.nexus.domain.contract.ForwardContract;
import com.agribid.nexus.domain.crop.*;
import com.agribid.nexus.domain.user.FarmerProfile;
import com.agribid.nexus.dto.response.FpoPayoutResponse;
import com.agribid.nexus.dto.response.FpoPoolResponse;
import com.agribid.nexus.exception.ConcurrentPoolContributionException;
import com.agribid.nexus.exception.InvalidPoolStateException;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.exception.UnauthorizedActionException;
import com.agribid.nexus.repository.*;
import com.agribid.nexus.security.UserPrincipal;
import com.agribid.nexus.service.FpoPoolingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class FpoPoolingServiceImpl implements FpoPoolingService {

    private final FpoPooledLotRepository poolRepository;
    private final FpoPooledLotContributionRepository contributionRepository;
    private final FarmerProfileRepository farmerProfileRepository;
    private final CategoryRepository categoryRepository;
    private final CropLotRepository cropLotRepository;
    private final BidListingRepository bidListingRepository;
    private final ForwardContractRepository forwardContractRepository;

    @Override
    @Transactional
    public FpoPoolResponse createPool(String categoryCode, BigDecimal targetQuantityKg, UserPrincipal coordinatorPrincipal) {
        FarmerProfile coordinator = requireFarmerWithFpo(coordinatorPrincipal.getId());
        Category category = categoryRepository.findByCode(categoryCode)
                .orElseThrow(() -> new ResourceNotFoundException("Unknown category code: " + categoryCode));

        FpoPooledLot pool = new FpoPooledLot(coordinator.getFpoAffiliation(), category, coordinator, targetQuantityKg);
        poolRepository.save(pool);
        return toResponse(pool);
    }

    @Override
    @Transactional(readOnly = true)
    public List<FpoPoolResponse> listOpenPoolsForMyFpo(UserPrincipal farmerPrincipal) {
        FarmerProfile farmer = requireFarmerWithFpo(farmerPrincipal.getId());
        return poolRepository.findByFpoNameAndStatus(farmer.getFpoAffiliation(), PoolStatus.OPEN)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * A farmer may only contribute a lot they own, that is already
     * GRADED (an ungraded lot has no verified quality to bargain
     * with), that matches the pool's category, and that belongs to
     * the same FPO as the pool — this last check is what stops a
     * pool from being diluted by produce from outside the group it's
     * meant to give bargaining power to.
     */
    @Override
    @Transactional
    public FpoPoolResponse contribute(Long poolId, Long cropLotId, BigDecimal quantityKg, UserPrincipal contributorPrincipal) {
        FpoPooledLot pool = findPoolOrThrow(poolId);
        if (!pool.isOpen()) {
            throw new InvalidPoolStateException("Pool " + poolId + " is not open for contributions (status: " + pool.getStatus() + ")");
        }

        FarmerProfile contributor = requireFarmerWithFpo(contributorPrincipal.getId());
        // Cross-FPO pooling: pool.getFpoName() may now be a
        // comma-separated list of eligible FPO names set at pool
        // creation, not only a single exact FPO — real farmers in
        // neighboring FPOs can combine into one larger listing.
        boolean isMember = java.util.Arrays.stream(pool.getFpoName().split(","))
                .map(String::trim)
                .anyMatch(name -> name.equalsIgnoreCase(contributor.getFpoAffiliation()));
        if (!isMember) {
            throw new UnauthorizedActionException("Farmer's FPO is not eligible for pool '" + pool.getFpoName() + "'");
        }

        CropLot lot = cropLotRepository.findById(cropLotId)
                .orElseThrow(() -> new ResourceNotFoundException("Crop lot not found: " + cropLotId));
        if (!lot.getOwner().getId().equals(contributor.getId())) {
            throw new UnauthorizedActionException("You do not own crop lot " + cropLotId);
        }
        if (lot.getStatus() != LotStatus.GRADED) {
            throw new InvalidPoolStateException("Crop lot " + cropLotId + " must be GRADED before it can be pooled (current status: " + lot.getStatus() + ")");
        }
        if (lot.getCategory() == null || !lot.getCategory().getId().equals(pool.getCategory().getId())) {
            throw new InvalidPoolStateException("Crop lot " + cropLotId + " category does not match pool category " + pool.getCategory().getCode());
        }
        if (contributionRepository.findByCropLotId(cropLotId).isPresent()) {
            throw new InvalidPoolStateException("Crop lot " + cropLotId + " has already been contributed to a pool");
        }
        if (quantityKg.compareTo(lot.getQuantityKg()) > 0) {
            throw new InvalidPoolStateException("Contribution quantity exceeds the crop lot's own quantity");
        }

        contributionRepository.save(new FpoPooledLotContribution(pool, contributor, lot, quantityKg));
        pool.addContribution(quantityKg);
        try {
            // saveAndFlush forces the version-checked UPDATE to execute
            // NOW, inside this method, rather than deferred to
            // end-of-transaction commit — so a conflict is caught and
            // translated here, not leaked as a raw Hibernate exception
            // from somewhere else in the call stack later.
            poolRepository.saveAndFlush(pool);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            throw new ConcurrentPoolContributionException(
                    "Pool " + poolId + " changed since you last read it — refresh and resubmit your contribution");
        }
        return toResponse(pool);
    }

    /**
     * Merges every contribution into one new CropLot, owned by the
     * pool's coordinator, carrying the combined quantity and the
     * most conservative (lowest) quality grade among contributors —
     * a buyer bidding on the pooled lot should never be surprised by
     * quality worse than what was advertised. The resulting CropLot
     * is otherwise perfectly ordinary: it can be listed, bid on, and
     * converted to a contract through the existing pipeline with no
     * further FPO-specific code involved.
     */
    @Override
    @Transactional
    public FpoPoolResponse aggregate(Long poolId, UserPrincipal coordinatorPrincipal) {
        FpoPooledLot pool = findPoolOrThrow(poolId);
        if (!pool.isOpen()) {
            throw new InvalidPoolStateException("Pool " + poolId + " is not open (status: " + pool.getStatus() + ")");
        }
        if (!pool.getCoordinatorFarmer().getId().equals(coordinatorPrincipal.getId())) {
            throw new UnauthorizedActionException("Only the pool's coordinator can trigger aggregation");
        }
        if (!pool.hasReachedTarget()) {
            throw new InvalidPoolStateException(
                    "Pool has not reached its target yet (%s / %s kg)".formatted(pool.getAggregatedQuantityKg(), pool.getTargetQuantityKg()));
        }

        List<FpoPooledLotContribution> contributions = contributionRepository.findByPooledLotId(poolId);
        if (contributions.isEmpty()) {
            throw new InvalidPoolStateException("Pool has no contributions to aggregate");
        }

        // NOTE: deliberately NOT ordering by gradeLabel (e.g. "A"/"B"/"C")
        // to find the "worst" grade — string ordering has no reliable
        // relationship to quality (a scale where "A" is best would pick
        // exactly the wrong contributor). estimatedShelfLifeDays is an
        // unambiguous numeric proxy for conservatism instead: the
        // aggregate lot inherits the shortest shelf-life estimate among
        // its contributors, since a buyer needs to know the batch's
        // true urgency, not its best-case one.
        QualityGrade mostConservativeGrade = contributions.stream()
                .map(c -> c.getCropLot().getQualityGrade())
                .filter(java.util.Objects::nonNull)
                .min(java.util.Comparator.comparing(
                        QualityGrade::getEstimatedShelfLifeDays,
                        java.util.Comparator.nullsLast(java.util.Comparator.naturalOrder())))
                .orElse(null);

        Set<PestTag> combinedTags = new HashSet<>();
        contributions.forEach(c -> combinedTags.addAll(c.getCropLot().getPestTags()));

        CropLot aggregateLot = new CropLot(pool.getCoordinatorFarmer(), pool.getCategory(), pool.getAggregatedQuantityKg());
        cropLotRepository.save(aggregateLot);
        if (mostConservativeGrade != null) {
            aggregateLot.applyGrading(mostConservativeGrade, combinedTags);
        } else {
            aggregateLot.setStatus(LotStatus.GRADED);
        }

        pool.markAggregated(aggregateLot);
        try {
            poolRepository.saveAndFlush(pool);
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException ex) {
            // A concurrent aggregate() call (or a duplicate retried
            // request) already completed this pool between our read
            // and this write — reject ours rather than silently
            // creating a second, orphaned CropLot from the same
            // contributions.
            throw new ConcurrentPoolContributionException(
                    "Pool " + poolId + " was already aggregated by another request — refresh to see the result");
        }
        return toResponse(pool);
    }

    /**
     * Only meaningful once the resulting CropLot has actually been
     * listed, bid on, and converted into a ForwardContract — this
     * reads the real settled price rather than re-deriving one, so
     * the payout always matches what the buyer actually agreed to pay.
     */
    @Override
    @Transactional(readOnly = true)
    public FpoPayoutResponse getPayoutBreakdown(Long poolId) {
        FpoPooledLot pool = findPoolOrThrow(poolId);
        if (pool.getResultingCropLot() == null) {
            throw new InvalidPoolStateException("Pool " + poolId + " has not been aggregated yet");
        }

        BidListing listing = bidListingRepository.findByCropLotId(pool.getResultingCropLot().getId())
                .orElseThrow(() -> new InvalidPoolStateException("Pool's aggregate lot has not been published as a listing yet"));

        ForwardContract contract = forwardContractRepository.findBySourceListingId(listing.getId())
                .orElseThrow(() -> new InvalidPoolStateException("Pool's listing has not converted to a settled contract yet"));

        BigDecimal pricePerKg = contract.getLockedPrice();
        BigDecimal totalQuantity = pool.getAggregatedQuantityKg();
        BigDecimal totalAmount = pricePerKg.multiply(totalQuantity);

        List<FpoPayoutResponse.Share> shares = contributionRepository.findByPooledLotId(poolId).stream()
                .map(c -> {
                    BigDecimal shareOfTotal = c.getContributedQuantityKg()
                            .divide(totalQuantity, MathContext.DECIMAL64);
                    BigDecimal payout = totalAmount.multiply(shareOfTotal).setScale(2, RoundingMode.HALF_UP);
                    return new FpoPayoutResponse.Share(
                            c.getContributorFarmer().getId(),
                            c.getCropLot().getId(),
                            c.getContributedQuantityKg(),
                            shareOfTotal.setScale(4, RoundingMode.HALF_UP),
                            payout
                    );
                })
                .toList();

        return new FpoPayoutResponse(poolId, totalQuantity, pricePerKg, totalAmount, shares);
    }

    private FarmerProfile requireFarmerWithFpo(Long farmerId) {
        FarmerProfile farmer = farmerProfileRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found: " + farmerId));
        if (farmer.getFpoAffiliation() == null || farmer.getFpoAffiliation().isBlank()) {
            throw new InvalidPoolStateException("Farmer has no FPO affiliation on file — pooling requires one");
        }
        return farmer;
    }

    private FpoPooledLot findPoolOrThrow(Long poolId) {
        return poolRepository.findById(poolId)
                .orElseThrow(() -> new ResourceNotFoundException("FPO pool not found: " + poolId));
    }

    private FpoPoolResponse toResponse(FpoPooledLot pool) {
        BigDecimal percent = pool.getTargetQuantityKg().signum() == 0
                ? BigDecimal.ZERO
                : pool.getAggregatedQuantityKg()
                    .multiply(BigDecimal.valueOf(100))
                    .divide(pool.getTargetQuantityKg(), 1, RoundingMode.HALF_UP);

        return new FpoPoolResponse(
                pool.getId(),
                pool.getFpoName(),
                pool.getCategory().getCode(),
                pool.getCategory().getName(),
                pool.getCoordinatorFarmer().getId(),
                pool.getTargetQuantityKg(),
                pool.getAggregatedQuantityKg(),
                percent,
                pool.getStatus(),
                pool.getResultingCropLot() != null ? pool.getResultingCropLot().getId() : null,
                pool.getCreatedAt(),
                pool.getAggregatedAt()
        );
    }
}
