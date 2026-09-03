package com.agribid.nexus.ai.evidence.model;

/**
 * One point in a GPS track sampled continuously while the farmer was
 * recording, as opposed to the single start/end coordinate
 * (CropLot.captureLatitude/Longitude) captured once. offsetSeconds is
 * relative to the start of recording, not a wall-clock timestamp —
 * only relative ordering and spacing matter for the coverage check.
 */
public record GpsSample(double latitude, double longitude, int offsetSeconds) {
}
