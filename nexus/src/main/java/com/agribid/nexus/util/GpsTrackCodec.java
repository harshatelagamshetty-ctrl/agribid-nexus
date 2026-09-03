package com.agribid.nexus.util;

import com.agribid.nexus.ai.evidence.model.GpsSample;

import java.util.ArrayList;
import java.util.List;

/**
 * Deliberately not a real JSON library. This project's dependency
 * graph has genuine, already-encountered Jackson 2 vs Jackson 3
 * ambiguity under Spring Boot 4 (springdoc pulls in Jackson 2
 * transitively; the web starter defaults to Jackson 3) that cannot
 * be verified without a real build this environment can't run. For a
 * data shape this trivial — a list of three-field records — a
 * dependency-free delimited encoding sidesteps that risk entirely
 * rather than adding an assumption on top of an already-uncertain
 * one.
 *
 * Format: "lat,lng,offset;lat,lng,offset;..." — semicolon-separated
 * samples, comma-separated fields within each.
 */
public final class GpsTrackCodec {

    private GpsTrackCodec() {
    }

    public static String encode(List<GpsSample> track) {
        if (track == null || track.isEmpty()) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < track.size(); i++) {
            GpsSample s = track.get(i);
            if (i > 0) sb.append(';');
            sb.append(s.latitude()).append(',').append(s.longitude()).append(',').append(s.offsetSeconds());
        }
        return sb.toString();
    }

    public static List<GpsSample> decode(String encoded) {
        List<GpsSample> result = new ArrayList<>();
        if (encoded == null || encoded.isBlank()) {
            return result;
        }
        for (String part : encoded.split(";")) {
            String[] fields = part.split(",");
            if (fields.length != 3) {
                continue; // skip a malformed sample rather than fail the whole track
            }
            try {
                result.add(new GpsSample(
                        Double.parseDouble(fields[0]),
                        Double.parseDouble(fields[1]),
                        Integer.parseInt(fields[2])));
            } catch (NumberFormatException ignored) {
                // same principle as above — one bad sample doesn't
                // invalidate an otherwise-valid track
            }
        }
        return result;
    }
}
