package com.agribid.nexus.ai.evidence;

import com.agribid.nexus.ai.evidence.model.CoverageResult;
import com.agribid.nexus.ai.evidence.model.GpsSample;
import com.agribid.nexus.domain.crop.Field;
import com.agribid.nexus.util.GeoUtils;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Answers a different question than every other check in this
 * package: not "is this evidence genuine" but "does the evidence
 * cover a meaningful portion of the real field, or just one spot in
 * it." A perfectly authentic, correctly-located, freshly-recorded
 * video that only ever points at one healthy corner passes every
 * other check in this system — this is the one built specifically to
 * catch that gap.
 *
 * Two independent thresholds, both required:
 *   1. MIN_PATH_LENGTH_METERS — rules out "stand still and pan the
 *      phone," which produces GPS samples clustered at one point
 *      regardless of how the camera moves.
 *   2. MIN_GRID_CELLS_VISITED — rules out "walk back and forth along
 *      one edge," which can rack up path length without actually
 *      covering the field's area. The field's registered radius is
 *      divided into a coarse grid; the track must touch enough
 *      distinct cells to demonstrate real spatial spread, not just
 *      distance traveled.
 *
 * Honest limitation, stated here because it matters more here than
 * anywhere else: this proves the farmer physically moved across a
 * meaningful portion of their registered field while recording. It
 * does NOT prove every visible patch was representative of the whole
 * — a farmer could still walk across 80% of a field while
 * deliberately steering around a small diseased patch and pass this
 * check. This raises the cost of concentrated staging substantially;
 * it does not mathematically guarantee full representativeness, and
 * no claim to the contrary is made anywhere in this system.
 */
@Component
public class SpatialCoverageChecker {

    private static final double MIN_PATH_LENGTH_METERS = 15.0;
    private static final int GRID_DIMENSION = 4; // 4x4 = 16 cells across the field's bounding square
    private static final int MIN_GRID_CELLS_VISITED = 4; // ~25% spatial spread minimum

    public CoverageResult check(List<GpsSample> track, Field field) {
        if (track == null || track.size() < 2) {
            return CoverageResult.NOT_AVAILABLE;
        }

        double pathLength = totalPathLengthMeters(track);
        int cellsVisited = distinctGridCellsVisited(track, field);

        boolean sufficient = pathLength >= MIN_PATH_LENGTH_METERS && cellsVisited >= MIN_GRID_CELLS_VISITED;
        return sufficient ? CoverageResult.SUFFICIENT : CoverageResult.INSUFFICIENT;
    }

    private double totalPathLengthMeters(List<GpsSample> track) {
        double total = 0.0;
        for (int i = 1; i < track.size(); i++) {
            GpsSample a = track.get(i - 1);
            GpsSample b = track.get(i);
            total += GeoUtils.haversineMeters(a.latitude(), a.longitude(), b.latitude(), b.longitude());
        }
        return total;
    }

    /**
     * A coarse equirectangular approximation (not a true geodesic
     * projection) — entirely adequate at field scale (tens to low
     * hundreds of meters), where the curvature of the earth is
     * negligible. Using a full geodesic grid would be genuine
     * over-engineering for this problem size.
     */
    private int distinctGridCellsVisited(List<GpsSample> track, Field field) {
        double metersPerDegreeLat = 111_320.0;
        double metersPerDegreeLon = 111_320.0 * Math.cos(Math.toRadians(field.getLatitude()));

        double halfExtentMeters = field.getRadiusMeters();
        double cellSizeMeters = (2 * halfExtentMeters) / GRID_DIMENSION;

        Set<String> visitedCells = new HashSet<>();
        for (GpsSample sample : track) {
            double dxMeters = (sample.longitude() - field.getLongitude()) * metersPerDegreeLon;
            double dyMeters = (sample.latitude() - field.getLatitude()) * metersPerDegreeLat;

            // Samples outside the registered field radius entirely
            // don't count toward coverage of THIS field — that's a
            // separate concern already handled by checkFieldMatch's
            // MISMATCH outcome, not this check's job to re-flag.
            if (Math.abs(dxMeters) > halfExtentMeters || Math.abs(dyMeters) > halfExtentMeters) {
                continue;
            }

            int cellX = (int) Math.floor((dxMeters + halfExtentMeters) / cellSizeMeters);
            int cellY = (int) Math.floor((dyMeters + halfExtentMeters) / cellSizeMeters);
            visitedCells.add(cellX + "," + cellY);
        }
        return visitedCells.size();
    }
}
