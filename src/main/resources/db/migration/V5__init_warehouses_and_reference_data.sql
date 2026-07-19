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
-- Minimal seed / reference data so the app is usable immediately
-- after a fresh migration, without requiring manual data entry
-- before the first crop lot can even be categorized.
-- ============================================================

INSERT INTO categories (code, name, description) VALUES
                                                     ('WHEAT', 'Wheat', 'Common wheat, various grades'),
                                                     ('TOMATO', 'Tomato', 'Fresh tomato, various cultivars'),
                                                     ('RICE', 'Rice', 'Paddy and milled rice'),
                                                     ('ONION', 'Onion', 'Fresh onion bulbs'),
                                                     ('POTATO', 'Potato', 'Table and processing-grade potato');

INSERT INTO pest_tags (code, label, severity_default) VALUES
                                                          ('BLIGHT', 'Blight', 'HIGH'),
                                                          ('APHID_DAMAGE', 'Aphid damage', 'MEDIUM'),
                                                          ('FUNGAL_SPOTTING', 'Fungal spotting', 'MEDIUM'),
                                                          ('ROOT_ROT', 'Root rot', 'HIGH'),
                                                          ('INSECT_BORING', 'Insect boring', 'MEDIUM');

INSERT INTO msp_rates (crop_code, region, price_per_kg, effective_date) VALUES
                                                                            ('WHEAT', 'PUNJAB', 22.75, '2026-04-01'),
                                                                            ('WHEAT', 'UTTAR PRADESH', 22.50, '2026-04-01'),
                                                                            ('RICE', 'PUNJAB', 21.83, '2026-04-01'),
                                                                            ('RICE', 'KARNATAKA', 21.60, '2026-04-01'),
                                                                            ('TOMATO', 'MAHARASHTRA', 18.00, '2026-04-01'),
                                                                            ('ONION', 'MAHARASHTRA', 15.50, '2026-04-01'),
                                                                            ('POTATO', 'UTTAR PRADESH', 12.25, '2026-04-01');

INSERT INTO warehouses (name, region, capacity_kg, current_occupied_kg) VALUES
                                                                            ('Nashik Central Storage', 'MAHARASHTRA', 500000.00, 120000.00),
                                                                            ('Ludhiana Grain Terminal', 'PUNJAB', 800000.00, 340000.00),
                                                                            ('Bengaluru Fresh Hub', 'KARNATAKA', 300000.00, 90000.00);