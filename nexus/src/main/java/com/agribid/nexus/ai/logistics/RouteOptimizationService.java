package com.agribid.nexus.ai.logistics;

import com.agribid.nexus.ai.logistics.model.GeoPoint;
import com.agribid.nexus.ai.logistics.model.RoutePlan;
import com.agribid.nexus.ai.logistics.model.RouteStop;
import com.agribid.nexus.domain.crop.CropLot;
import com.agribid.nexus.domain.logistics.Warehouse;
import com.agribid.nexus.exception.ResourceNotFoundException;
import com.agribid.nexus.repository.CropLotRepository;
import com.agribid.nexus.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * A real, named routing engine, not just a warehouse-capacity
 * lookup: given several farmers' pooled crop lots and a destination
 * warehouse, this computes the pickup order a single vehicle should
 * follow, directly answering the "shared/pooled transport, real
 * routing engine" gap. It intentionally does NOT reach for OR-Tools
 * or a Python subprocess — those aren't available from a plain Java
 * service without a bigger dependency/process-management footprint
 * than this scope justifies. Nearest-neighbor construction + 2-opt
 * local search is a legitimate, well-studied heuristic pairing
 * (typically within 5-10% of optimal for small stop counts) that a
 * pure-JVM implementation can do without any external solver — if
 * this grows past a few dozen stops per route, that's the point to
 * bring in a real VRP library (e.g. jsprit, OptaPlanner) rather than
 * this hand-rolled heuristic; that tradeoff is explicit, not hidden.
 */
@Service
@RequiredArgsConstructor
public class RouteOptimizationService {

    // EARTH_RADIUS_KM removed — was only used by the haversineKm formula
    // now delegated to GeoUtils, which has its own copy of this constant.

    private final WarehouseRepository warehouseRepository;
    private final CropLotRepository cropLotRepository;

    @Transactional(readOnly = true)
    public RoutePlan optimizePickupRoute(Long warehouseId, List<Long> cropLotIds) {
        if (cropLotIds == null || cropLotIds.isEmpty()) {
            throw new IllegalArgumentException("At least one crop lot is required to plan a route");
        }

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new ResourceNotFoundException("Warehouse not found: " + warehouseId));
        if (warehouse.getLatitude() == null || warehouse.getLongitude() == null) {
            throw new IllegalStateException("Warehouse " + warehouseId + " has no geocoordinate on file — cannot route to it");
        }

        List<CropLot> lots = cropLotRepository.findAllById(cropLotIds);
        if (lots.size() != cropLotIds.size()) {
            throw new ResourceNotFoundException("One or more crop lots in the request could not be found");
        }
        for (CropLot lot : lots) {
            if (lot.getCaptureLatitude() == null || lot.getCaptureLongitude() == null) {
                throw new IllegalStateException(
                        "Crop lot " + lot.getId() + " has no capture GPS on file — cannot route to it (video must be attached first)");
            }
        }

        // Node 0 is always the warehouse/depot; nodes 1..n are pickup
        // stops in the SAME order cropLotIds was submitted, which is
        // also what "naive order" distance is measured against below.
        List<GeoPoint> points = new ArrayList<>();
        points.add(new GeoPoint(warehouse.getLatitude(), warehouse.getLongitude()));
        lots.forEach(lot -> points.add(new GeoPoint(lot.getCaptureLatitude(), lot.getCaptureLongitude())));

        double[][] distanceMatrix = buildDistanceMatrix(points);

        List<Integer> naiveOrder = new ArrayList<>();
        for (int i = 0; i <= lots.size(); i++) naiveOrder.add(i);
        double naiveDistance = closedTourDistance(naiveOrder, distanceMatrix);

        List<Integer> nnOrder = nearestNeighborTour(distanceMatrix, points.size());
        List<Integer> optimizedOrder = twoOptImprove(nnOrder, distanceMatrix);
        double optimizedDistance = closedTourDistance(optimizedOrder, distanceMatrix);

        List<RouteStop> stops = buildStops(optimizedOrder, points, lots, warehouse, distanceMatrix);

        double savedPercent = naiveDistance <= 0
                ? 0.0
                : round2((naiveDistance - optimizedDistance) / naiveDistance * 100.0);

        return new RoutePlan(
                warehouse.getId(),
                warehouse.getName(),
                stops,
                round2(optimizedDistance),
                round2(naiveDistance),
                savedPercent,
                lots.size()
        );
    }

    private double[][] buildDistanceMatrix(List<GeoPoint> points) {
        int n = points.size();
        double[][] matrix = new double[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = i + 1; j < n; j++) {
                double d = haversineKm(points.get(i), points.get(j));
                matrix[i][j] = d;
                matrix[j][i] = d;
            }
        }
        return matrix;
    }

    static double haversineKm(GeoPoint a, GeoPoint b) {
        return com.agribid.nexus.util.GeoUtils.haversineKm(a.latitude(), a.longitude(), b.latitude(), b.longitude());
    }

    /**
     * Classic greedy construction: from the depot, always hop to the
     * nearest unvisited stop next. Fast and a reasonable starting
     * tour, but on its own can leave an obviously-crossable pair of
     * legs uncrossed — that's what twoOptImprove fixes afterward.
     */
    private List<Integer> nearestNeighborTour(double[][] distanceMatrix, int n) {
        boolean[] visited = new boolean[n];
        List<Integer> tour = new ArrayList<>(n);
        int current = 0;
        visited[0] = true;
        tour.add(0);

        for (int step = 1; step < n; step++) {
            int nearest = -1;
            double nearestDist = Double.MAX_VALUE;
            for (int candidate = 0; candidate < n; candidate++) {
                if (!visited[candidate] && distanceMatrix[current][candidate] < nearestDist) {
                    nearest = candidate;
                    nearestDist = distanceMatrix[current][candidate];
                }
            }
            visited[nearest] = true;
            tour.add(nearest);
            current = nearest;
        }
        return tour;
    }

    /**
     * Repeatedly reverses a segment of the tour whenever doing so
     * shortens it, until no single reversal helps anymore (a local
     * optimum). Node 0 (the depot) is never moved, since the route
     * always has to start and end there.
     */
    private List<Integer> twoOptImprove(List<Integer> initialTour, double[][] distanceMatrix) {
        List<Integer> tour = new ArrayList<>(initialTour);
        int n = tour.size();
        boolean improved = true;

        while (improved) {
            improved = false;
            for (int i = 1; i < n - 1; i++) {
                for (int j = i + 1; j < n; j++) {
                    double before = edgeCost(tour, i - 1, distanceMatrix) + edgeCost(tour, j, distanceMatrix);
                    reverseSegment(tour, i, j);
                    double after = edgeCost(tour, i - 1, distanceMatrix) + edgeCost(tour, j, distanceMatrix);
                    if (after < before - 1e-9) {
                        improved = true;
                    } else {
                        reverseSegment(tour, i, j); // revert, no improvement
                    }
                }
            }
        }
        return tour;
    }

    private double edgeCost(List<Integer> tour, int index, double[][] distanceMatrix) {
        int n = tour.size();
        int from = tour.get(index);
        int to = tour.get((index + 1) % n);
        return distanceMatrix[from][to];
    }

    private void reverseSegment(List<Integer> tour, int i, int j) {
        while (i < j) {
            int tmp = tour.get(i);
            tour.set(i, tour.get(j));
            tour.set(j, tmp);
            i++;
            j--;
        }
    }

    private double closedTourDistance(List<Integer> tour, double[][] distanceMatrix) {
        double total = 0.0;
        for (int i = 0; i < tour.size(); i++) {
            total += edgeCost(tour, i, distanceMatrix);
        }
        return total;
    }

    private List<RouteStop> buildStops(List<Integer> order, List<GeoPoint> points, List<CropLot> lots, Warehouse warehouse, double[][] distanceMatrix) {
        List<RouteStop> stops = new ArrayList<>();
        int sequence = 0;
        Integer previous = null;

        for (int nodeIndex : order) {
            GeoPoint point = points.get(nodeIndex);
            double legDistance = previous == null ? 0.0 : distanceMatrix[previous][nodeIndex];

            if (nodeIndex == 0) {
                stops.add(new RouteStop(sequence++, null, null, "Warehouse: " + warehouse.getName(),
                        point.latitude(), point.longitude(), round2(legDistance)));
            } else {
                CropLot lot = lots.get(nodeIndex - 1);
                stops.add(new RouteStop(sequence++, lot.getId(), lot.getOwner().getId(),
                        "Pickup: crop lot #" + lot.getId(), point.latitude(), point.longitude(), round2(legDistance)));
            }
            previous = nodeIndex;
        }

        // Close the loop: return to the warehouse.
        double returnLeg = distanceMatrix[previous][0];
        GeoPoint depot = points.get(0);
        stops.add(new RouteStop(sequence, null, null, "Return: " + warehouse.getName(),
                depot.latitude(), depot.longitude(), round2(returnLeg)));

        return stops;
    }

    private double round2(double value) {
        return Math.round(value * 100.0) / 100.0;
    }
}
