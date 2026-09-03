-- Adds the spatial-coverage defense against "farmer records only the
-- healthiest part of the field" — see SpatialCoverageChecker for the
-- full mechanism and its honest, stated limitations.

ALTER TABLE crop_lots ADD COLUMN capture_track_encoded TEXT;

ALTER TABLE crop_lot_evidence_reports ADD COLUMN coverage_result VARCHAR(20) NOT NULL DEFAULT 'NOT_AVAILABLE';
