-- Multi-signal crop verification engine. Deliberately does NOT
-- include any satellite/NDVI columns — that capability was evaluated
-- and explicitly not built. Peer-reviewed studies measured over 90%
-- cloud cover across Indian districts during the July-August monsoon
-- (the exact season most kharif crops are grown in), and Sentinel-2's
-- 10m resolution is marginal against typical Indian smallholder field
-- sizes (often under 0.6 hectares). Every signal below was chosen
-- specifically because it's reliable and buildable at hackathon scope,
-- not because it sounds impressive.

-- domain/crop/Field.java — a farmer registers a field once, GPS +
-- radius, reused across many crop lots' video submissions.
CREATE TABLE fields (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    farmer_id       BIGINT NOT NULL REFERENCES farmer_profiles (id),
    field_name      VARCHAR(120) NOT NULL,
    latitude        DOUBLE PRECISION NOT NULL,
    longitude       DOUBLE PRECISION NOT NULL,
    radius_meters   DOUBLE PRECISION NOT NULL DEFAULT 500.0,
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_fields_farmer_id ON fields (farmer_id);

-- domain/crop/CropLot.java additions: video_hash backs duplicate-
-- submission detection; field_id links a video to a registered
-- location for the GPS-match check.
ALTER TABLE crop_lots ADD COLUMN video_hash VARCHAR(64);
ALTER TABLE crop_lots ADD COLUMN field_id BIGINT REFERENCES fields (id);

CREATE INDEX idx_crop_lots_video_hash ON crop_lots (video_hash);
CREATE INDEX idx_crop_lots_field_id ON crop_lots (field_id);

-- domain/crop/Category.java additions: typical harvest month window,
-- backing the seasonality-plausibility check. Nullable by design —
-- a category with no window configured reports UNKNOWN, never a
-- false failure.
ALTER TABLE categories ADD COLUMN typical_harvest_start_month INTEGER;
ALTER TABLE categories ADD COLUMN typical_harvest_end_month INTEGER;

-- Seed reasonable harvest windows for the categories already seeded
-- in V5. These are illustrative defaults for a demo, not authoritative
-- agronomic data — worth revisiting with a real source before any
-- real deployment.
UPDATE categories SET typical_harvest_start_month = 12, typical_harvest_end_month = 4 WHERE code = 'WHEAT';
UPDATE categories SET typical_harvest_start_month = 1,  typical_harvest_end_month = 12 WHERE code = 'TOMATO'; -- grown year-round in much of India
UPDATE categories SET typical_harvest_start_month = 10, typical_harvest_end_month = 12 WHERE code = 'RICE';
UPDATE categories SET typical_harvest_start_month = 11, typical_harvest_end_month = 3  WHERE code = 'ONION';
UPDATE categories SET typical_harvest_start_month = 1,  typical_harvest_end_month = 3  WHERE code = 'POTATO';

-- domain/crop/CropLotEvidenceReport.java — one persisted, explainable
-- row per graded video submission. Every column here is a named,
-- independently-inspectable signal, not a black-box score.
CREATE TABLE crop_lot_evidence_reports (
    id                              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    crop_lot_id                     BIGINT NOT NULL UNIQUE REFERENCES crop_lots (id),
    field_match                     VARCHAR(20) NOT NULL,
    field_match_distance_meters     DOUBLE PRECISION,
    travel_plausibility             VARCHAR(20) NOT NULL,
    duplicate_check                 VARCHAR(20) NOT NULL,
    seasonality_check               VARCHAR(20) NOT NULL,
    weather_plausibility            VARCHAR(20) NOT NULL,
    weather_note                    VARCHAR(500),
    overall_evidence                VARCHAR(20) NOT NULL,
    assessed_at                     TIMESTAMP WITH TIME ZONE NOT NULL
);
