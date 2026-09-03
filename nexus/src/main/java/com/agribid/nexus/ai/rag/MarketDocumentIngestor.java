package com.agribid.nexus.ai.rag;

import org.springframework.ai.document.Document;
import org.springframework.ai.reader.pdf.PagePdfDocumentReader;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Ingests historical mandi price bulletins, government MSP circulars,
 * and regional weather-yield reports into Qdrant. This is what
 * ReservePriceAdvisorService's QuestionAnswerAdvisor retrieves
 * against — the model never generates a reserve-price recommendation
 * from parametric memory, only from documents that actually passed
 * through here first.
 */
@Component
public class MarketDocumentIngestor {

    private final VectorStore vectorStore;
    private final TokenTextSplitter splitter;

    public MarketDocumentIngestor(VectorStore vectorStore) {
        this.vectorStore = vectorStore;
        this.splitter = new TokenTextSplitter();
    }

    /**
     * @param sourceLabel attached as document metadata (e.g.
     *                    "MSP-Circular-2026-Q2.pdf") so retrieved
     *                    chunks can be traced back to their origin —
     *                    this is what lets ReservePriceAdvisorService
     *                    return citedSources rather than an
     *                    unattributed number.
     */
    public void ingestPdf(Resource pdfResource, String sourceLabel) {
        PagePdfDocumentReader reader = new PagePdfDocumentReader(pdfResource);
        List<Document> pages = reader.get();

        pages.forEach(doc -> doc.getMetadata().put("source", sourceLabel));

        List<Document> chunks = splitter.apply(pages);
        vectorStore.add(chunks);
    }
}