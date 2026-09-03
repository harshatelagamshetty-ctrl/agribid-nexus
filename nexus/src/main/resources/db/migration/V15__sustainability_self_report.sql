-- Self-reported only — see CropLot.java for why these are never
-- treated as verified facts. The "low input" badge is computed at
-- the DTO mapping layer from both fields being explicitly true,
-- never inferred from one alone or from an unset value.
ALTER TABLE crop_lots ADD COLUMN self_reported_low_water_usage BOOLEAN;
ALTER TABLE crop_lots ADD COLUMN self_reported_low_pesticide_usage BOOLEAN;
