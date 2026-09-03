package com.agribid.nexus.ai.rag;

import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;

/**
 * Deliberately ROLE_ADMIN-only: ingesting a document is what makes
 * it authoritative context for every future reserve-price
 * recommendation, so this isn't exposed to farmers or distributors —
 * only vetted government/market bulletins should ever enter the
 * Qdrant collection this backs.
 */
@RestController
@RequestMapping("/api/v1/admin/rag/documents")
@RequiredArgsConstructor
public class DocumentIngestionController {

    private final MarketDocumentIngestor documentIngestor;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> ingest(
            @RequestParam("file") MultipartFile file,
            @RequestParam("sourceLabel") String sourceLabel) {
        try {
            documentIngestor.ingestPdf(new InputStreamResource(file.getInputStream()), sourceLabel);
            return ResponseEntity.status(HttpStatus.ACCEPTED).build();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded document for ingestion", e);
        }
    }
}