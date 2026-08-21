-- domain/logistics/Warehouse.java
CREATE TABLE warehouses (
                            id                      BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            name                    VARCHAR(255) NOT NULL,
                            region                  VARCHAR(120) NOT NULL,
                            capacity_kg             NUMERIC(12, 2) NOT NULL,
                            current_occupied_kg     NUMERIC(12, 2) NOT NULL DEFAULT 0
);

CREATE INDEX idx_warehouses_region ON warehouses (region);

-- Referenced by ai/tools/MspLookupTool and ai/mcp/server tools via
-- plain JdbcTemplate queries (not a full JPA entity — this is
-- read-only reference data seeded by government MSP circulars, not
-- part of the transactional auction domain model).
CREATE TABLE msp_rates (
                           id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           crop_code           VARCHAR(50) NOT NULL,
                           region              VARCHAR(120) NOT NULL,
                           price_per_kg        NUMERIC(12, 2) NOT NULL,
                           effective_date      DATE NOT NULL
);

CREATE INDEX idx_msp_rates_lookup ON msp_rates (crop_code, region, effective_date DESC);

-- ============================================================
-- Seed / reference data for immediate usability
-- (15 records per domain table)
-- ============================================================

-- 1. Crop Categories (15 items)
INSERT INTO categories (code, name, description) VALUES
                                                     ('WHEAT', 'Wheat', 'Common wheat, durum, and various commercial grades'),
                                                     ('TOMATO', 'Tomato', 'Fresh tomato cultivars including vine and processing variants'),
                                                     ('RICE', 'Rice', 'Paddy, basmati, and non-basmati milled rice'),
                                                     ('ONION', 'Onion', 'Fresh red, white, and yellow onion bulbs'),
                                                     ('POTATO', 'Potato', 'Table, seed, and processing-grade potato'),
                                                     ('MAIZE', 'Maize', 'Yellow and white corn / maize grains'),
                                                     ('COTTON', 'Cotton', 'Long and medium staple raw seed cotton'),
                                                     ('SOYBEAN', 'Soybean', 'Yellow and black seed oilseed varieties'),
                                                     ('MUSTARD', 'Mustard / Rapeseed', 'High-oil content mustard seeds'),
                                                     ('SUGARCANE', 'Sugarcane', 'Commercial milling sugarcane stalks'),
                                                     ('CHANA', 'Gram / Chickpea', 'Desi and Kabuli chickpea varieties'),
                                                     ('TUR', 'Arhar / Pigeon Pea', 'Whole and split red gram pulses'),
                                                     ('GROUNDNUT', 'Groundnut / Peanut', 'In-shell and shelled peanut pods'),
                                                     ('MANGO', 'Mango', 'Fresh horticultural varieties (Alphonso, Dasheri, etc.)'),
                                                     ('CHILI', 'Chili', 'Dry red chili and green fresh peppers');

-- 2. Pest Tags (15 items)
INSERT INTO pest_tags (code, label, severity_default) VALUES
                                                          ('BLIGHT', 'Blight', 'HIGH'),
                                                          ('APHID_DAMAGE', 'Aphid damage', 'MEDIUM'),
                                                          ('FUNGAL_SPOTTING', 'Fungal spotting', 'MEDIUM'),
                                                          ('ROOT_ROT', 'Root rot', 'HIGH'),
                                                          ('INSECT_BORING', 'Insect boring', 'MEDIUM'),
                                                          ('POWDERY_MILDEW', 'Powdery mildew', 'MEDIUM'),
                                                          ('RUST', 'Rust / Smut', 'MEDIUM'),
                                                          ('WILTING', 'Vascular wilt', 'HIGH'),
                                                          ('STEM_BORER', 'Stem borer infestation', 'HIGH'),
                                                          ('FRUIT_BORER', 'Fruit / pod borer', 'MEDIUM'),
                                                          ('LEAF_MINER', 'Leaf miner trails', 'LOW'),
                                                          ('WHITEFLY', 'Whitefly infestation', 'HIGH'),
                                                          ('DOWNY_MILDEW', 'Downy mildew', 'MEDIUM'),
                                                          ('MEALYBUG', 'Mealybug cluster', 'LOW'),
                                                          ('LEAF_CURL_VIRUS', 'Leaf curl virus', 'HIGH');

-- 3. Minimum Support Price Rates (15 items)
INSERT INTO msp_rates (crop_code, region, price_per_kg, effective_date) VALUES
                                                                            ('WHEAT', 'PUNJAB', 22.75, '2026-04-01'),
                                                                            ('WHEAT', 'UTTAR PRADESH', 22.50, '2026-04-01'),
                                                                            ('WHEAT', 'HARYANA', 22.75, '2026-04-01'),
                                                                            ('RICE', 'PUNJAB', 21.83, '2026-04-01'),
                                                                            ('RICE', 'KARNATAKA', 21.60, '2026-04-01'),
                                                                            ('RICE', 'WEST BENGAL', 21.83, '2026-04-01'),
                                                                            ('TOMATO', 'MAHARASHTRA', 18.00, '2026-04-01'),
                                                                            ('ONION', 'MAHARASHTRA', 15.50, '2026-04-01'),
                                                                            ('ONION', 'MADHYA PRADESH', 15.00, '2026-04-01'),
                                                                            ('POTATO', 'UTTAR PRADESH', 12.25, '2026-04-01'),
                                                                            ('MAIZE', 'BIHAR', 20.90, '2026-04-01'),
                                                                            ('COTTON', 'GUJARAT', 66.20, '2026-04-01'),
                                                                            ('SOYBEAN', 'MADHYA PRADESH', 46.00, '2026-04-01'),
                                                                            ('MUSTARD', 'RAJASTHAN', 56.50, '2026-04-01'),
                                                                            ('CHANA', 'MADHYA PRADESH', 54.40, '2026-04-01');

-- 4. Warehouses (15 items)
INSERT INTO warehouses (name, region, capacity_kg, current_occupied_kg) VALUES
                                                                            ('Nashik Central Storage', 'MAHARASHTRA', 500000.00, 120000.00),
                                                                            ('Ludhiana Grain Terminal', 'PUNJAB', 800000.00, 340000.00),
                                                                            ('Bengaluru Fresh Hub', 'KARNATAKA', 300000.00, 90000.00),
                                                                            ('Karnal Agri Depot', 'HARYANA', 650000.00, 210000.00),
                                                                            ('Indore Cold Logistics', 'MADHYA PRADESH', 450000.00, 180000.00),
                                                                            ('Lucknow Agro Reserve', 'UTTAR PRADESH', 700000.00, 310000.00),
                                                                            ('Rajkot Cotton & Grain Vault', 'GUJARAT', 600000.00, 250000.00),
                                                                            ('Jaipur Mandi Warehouse', 'RAJASTHAN', 400000.00, 110000.00),
                                                                            ('Hooghly Rice & Produce Hub', 'WEST BENGAL', 550000.00, 200000.00),
                                                                            ('Guntur Spices & Grain Facility', 'ANDHRA PRADESH', 350000.00, 130000.00),
                                                                            ('Patna Grain Silo', 'BIHAR', 500000.00, 175000.00),
                                                                            ('Nizamabad Agri Cold Storage', 'TELANGANA', 300000.00, 85000.00),
                                                                            ('Coimbatore Farmers Cold Chain', 'TAMIL NADU', 400000.00, 140000.00),
                                                                            ('Shimla Cold Vault', 'HIMACHAL PRADESH', 250000.00, 60000.00),
                                                                            ('Cuttack Central Depot', 'ODISHA', 450000.00, 190000.00);