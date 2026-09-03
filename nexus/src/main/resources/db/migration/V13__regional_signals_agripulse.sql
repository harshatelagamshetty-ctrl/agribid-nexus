-- AgriPulse: the trust-weighted regional intelligence layer. Rows
-- here are written ONLY by RegionalSignalAggregationService, which
-- is only ever called for HIGH/MEDIUM evidence submissions (see
-- CropGradingService) and genuinely settled contract prices (see
-- BidListingServiceImpl.convertToContract) — never from a raw,
-- unverified listing.
CREATE TABLE regional_signals (
    id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    district                    VARCHAR(120) NOT NULL,
    category_id                 BIGINT NOT NULL REFERENCES categories (id),
    week_start                  DATE NOT NULL,
    verified_submission_count   INT NOT NULL DEFAULT 0,
    avg_quality_score           DOUBLE PRECISION,
    total_verified_quantity_kg  DECIMAL(14,2) NOT NULL DEFAULT 0,
    avg_settled_price_per_kg    DECIMAL(12,2),
    settled_transaction_count   INT NOT NULL DEFAULT 0,
    pest_tag_occurrences        VARCHAR(2000) DEFAULT '',
    last_updated_at             TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uq_regional_signal UNIQUE (district, category_id, week_start)
);

CREATE INDEX idx_regional_signals_lookup ON regional_signals (district, category_id, week_start);
