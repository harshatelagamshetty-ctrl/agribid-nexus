package com.agribid.nexus.util;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * Verifies a file's actual binary signature (magic bytes) rather
 * than trusting the client-supplied Content-Type header, which the
 * client fully controls and can misrepresent — a malicious upload
 * can claim "video/mp4" in its header while containing anything at
 * all. This was an explicitly, honestly flagged open gap from the
 * security audit; this class closes it.
 *
 * Only checks the container format at the byte level (MP4/MOV share
 * the ISO Base Media container signature; WebM uses the EBML/Matroska
 * signature) — it does NOT validate that the video codec inside is
 * playable, and it does not scan for embedded malicious payloads.
 * That's a real, honest limitation: this raises the bar from "any
 * file with a spoofed header" to "a file that is genuinely a
 * container of the claimed type," which is the realistic, achievable
 * goal at this scope — not a claim of complete file-safety.
 */
public final class VideoFileSignatureValidator {

    private VideoFileSignatureValidator() {
    }

    private static final Set<String> ISO_BMFF_BRANDS = Set.of(
            "isom", "iso2", "mp41", "mp42", "avc1", "M4V ", "qt  "
    );

    public static boolean isValidVideoContainer(InputStream inputStream) {
        try {
            byte[] header = inputStream.readNBytes(16);
            if (header.length < 12) {
                return false;
            }
            return isIsoBaseMediaContainer(header) || isWebmContainer(header);
        } catch (IOException e) {
            return false;
        }
    }

    /**
     * MP4/MOV (ISO Base Media File Format): bytes 4-7 are always the
     * ASCII literal "ftyp", and bytes 8-11 are a 4-character brand
     * code identifying the specific variant. Real cameras and phones
     * only ever emit a small, well-known set of these brands — this
     * checks against that set rather than accepting "ftyp" alone,
     * which alone would be too permissive.
     */
    private static boolean isIsoBaseMediaContainer(byte[] header) {
        String ftypMarker = new String(header, 4, 4, java.nio.charset.StandardCharsets.US_ASCII);
        if (!"ftyp".equals(ftypMarker)) {
            return false;
        }
        String brand = new String(header, 8, 4, java.nio.charset.StandardCharsets.US_ASCII);
        return ISO_BMFF_BRANDS.contains(brand);
    }

    /**
     * WebM/Matroska containers always begin with the fixed 4-byte
     * EBML magic number 0x1A45DFA3.
     */
    private static boolean isWebmContainer(byte[] header) {
        return (header[0] & 0xFF) == 0x1A
                && (header[1] & 0xFF) == 0x45
                && (header[2] & 0xFF) == 0xDF
                && (header[3] & 0xFF) == 0xA3;
    }
}
