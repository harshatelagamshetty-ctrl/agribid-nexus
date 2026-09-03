-- Offline capture support. was_offline_capture makes the freshness
-- trade-off visible rather than hiding it (see CropLotServiceImpl's
-- MAX_OFFLINE_CAPTURE_AGE comment for why the trade-off exists at
-- all). offline_idempotency_key prevents a retried sync from a
-- dropped connection creating a second lot from the same offline
-- capture.
ALTER TABLE crop_lots ADD COLUMN was_offline_capture BOOLEAN;
ALTER TABLE crop_lots ADD COLUMN offline_idempotency_key VARCHAR(100) UNIQUE;
