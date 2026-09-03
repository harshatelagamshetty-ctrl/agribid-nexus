-- Replaces single-photo grading with video + capture metadata, per
-- the correction: a static photo lets a farmer cherry-pick the one
-- good-looking corner of a lot. A video walkthrough, timestamped and
-- geo-tagged at capture, closes that gap — it doesn't eliminate
-- gaming entirely (nothing purely technical can), but it raises the
-- cost of it significantly and pairs with delivery-time
-- re-verification (a separate, later correction) for the rest.

ALTER TABLE crop_lots DROP COLUMN image_url;

ALTER TABLE crop_lots ADD COLUMN video_url VARCHAR(500);
ALTER TABLE crop_lots ADD COLUMN capture_latitude DOUBLE PRECISION;
ALTER TABLE crop_lots ADD COLUMN capture_longitude DOUBLE PRECISION;
ALTER TABLE crop_lots ADD COLUMN captured_at TIMESTAMP WITH TIME ZONE;