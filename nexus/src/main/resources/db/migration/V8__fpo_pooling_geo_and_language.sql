-- ============================================================
-- V8: FPO aggregation, warehouse geocoordinates, user language
-- ============================================================

-- 1. Vernacular/accessibility: every user gets a language
--    preference, defaulted so existing rows stay valid.
ALTER TABLE users ADD COLUMN preferred_language VARCHAR(10) NOT NULL DEFAULT 'en';

-- 2. Logistics: warehouses need a real coordinate before any route
--    can be computed against them. Nullable — a warehouse without a
--    geocode is simply excluded as a routing candidate, never
--    guessed at.
ALTER TABLE warehouses ADD COLUMN latitude DOUBLE;
ALTER TABLE warehouses ADD COLUMN longitude DOUBLE;

UPDATE warehouses SET latitude = 19.9975, longitude = 73.7898 WHERE name = 'Nashik Central Storage';
UPDATE warehouses SET latitude = 30.9010, longitude = 75.8573 WHERE name = 'Ludhiana Grain Terminal';
UPDATE warehouses SET latitude = 12.9716, longitude = 77.5946 WHERE name = 'Bengaluru Fresh Hub';
UPDATE warehouses SET latitude = 29.6857, longitude = 76.9905 WHERE name = 'Karnal Agri Depot';
UPDATE warehouses SET latitude = 22.7196, longitude = 75.8577 WHERE name = 'Indore Cold Logistics';
UPDATE warehouses SET latitude = 26.8467, longitude = 80.9462 WHERE name = 'Lucknow Agro Reserve';
UPDATE warehouses SET latitude = 22.3039, longitude = 70.8022 WHERE name = 'Rajkot Cotton & Grain Vault';
UPDATE warehouses SET latitude = 26.9124, longitude = 75.7873 WHERE name = 'Jaipur Mandi Warehouse';
UPDATE warehouses SET latitude = 22.9012, longitude = 88.3960 WHERE name = 'Hooghly Rice & Produce Hub';
UPDATE warehouses SET latitude = 16.3067, longitude = 80.4365 WHERE name = 'Guntur Spices & Grain Facility';
UPDATE warehouses SET latitude = 25.5941, longitude = 85.1376 WHERE name = 'Patna Grain Silo';
UPDATE warehouses SET latitude = 18.6725, longitude = 78.0941 WHERE name = 'Nizamabad Agri Cold Storage';
UPDATE warehouses SET latitude = 11.0168, longitude = 76.9558 WHERE name = 'Coimbatore Farmers Cold Chain';
UPDATE warehouses SET latitude = 31.1048, longitude = 77.1734 WHERE name = 'Shimla Cold Vault';
UPDATE warehouses SET latitude = 20.4625, longitude = 85.8828 WHERE name = 'Cuttack Central Depot';

-- 3. FPO aggregation. Deliberately does NOT touch crop_lots or
--    bid_listings: a pool aggregates several already-graded CropLots
--    into one new, larger CropLot (owned by the pool's coordinator
--    farmer), which then flows through the existing listing/bidding/
--    contract/fulfillment pipeline completely unchanged. This is why
--    the feature is a handful of new tables, not a rewrite of the
--    auction engine.
CREATE TABLE fpo_pooled_lots (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    fpo_name                VARCHAR(255) NOT NULL,
    category_id             BIGINT NOT NULL REFERENCES categories(id),
    coordinator_farmer_id   BIGINT NOT NULL REFERENCES farmer_profiles(id),
    target_quantity_kg      NUMERIC(12, 2) NOT NULL,
    aggregated_quantity_kg  NUMERIC(12, 2) NOT NULL DEFAULT 0,
    status                  VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    resulting_crop_lot_id   BIGINT REFERENCES crop_lots(id),
    created_at              TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    aggregated_at           TIMESTAMP
);

CREATE INDEX idx_fpo_pooled_lots_fpo_name ON fpo_pooled_lots (fpo_name);
CREATE INDEX idx_fpo_pooled_lots_status ON fpo_pooled_lots (status);

-- Each row is one farmer's contribution of one (already owned,
-- already graded) CropLot into a pool. A CropLot can only be
-- contributed once, ever — the unique constraint is what prevents a
-- farmer double-pledging the same physical harvest into two pools,
-- or into a pool and an individual listing at the same time.
CREATE TABLE fpo_pooled_lot_contributions (
    id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    pooled_lot_id           BIGINT NOT NULL REFERENCES fpo_pooled_lots(id),
    contributor_farmer_id   BIGINT NOT NULL REFERENCES farmer_profiles(id),
    crop_lot_id             BIGINT NOT NULL REFERENCES crop_lots(id),
    contributed_quantity_kg NUMERIC(12, 2) NOT NULL,
    contributed_at          TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_contribution_crop_lot UNIQUE (crop_lot_id)
);

CREATE INDEX idx_contributions_pooled_lot ON fpo_pooled_lot_contributions (pooled_lot_id);
