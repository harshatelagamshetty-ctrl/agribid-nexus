-- domain/crop/Category.java
CREATE TABLE categories (
                            id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                            code            VARCHAR(50) NOT NULL UNIQUE,
                            name            VARCHAR(120) NOT NULL,
                            description     TEXT
);

-- domain/crop/QualityGrade.java — populated by ai/vision/CropGradingService
-- after every Gemini grading call; never manually inserted except by seed data.
CREATE TABLE quality_grades (
                                id                          BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                                grade_label                 VARCHAR(10) NOT NULL,
                                estimated_shelf_life_days   INTEGER,
                                confidence_score            DOUBLE PRECISION,
                                assessed_at                 TIMESTAMP WITH TIME ZONE NOT NULL
);

-- domain/crop/PestTag.java
CREATE TABLE pest_tags (
                           id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           code                VARCHAR(60) NOT NULL UNIQUE,
                           label               VARCHAR(120) NOT NULL,
                           severity_default    VARCHAR(20)
);

-- domain/crop/CropLot.java
CREATE TABLE crop_lots (
                           id                  BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                           farmer_id           BIGINT NOT NULL REFERENCES farmer_profiles (id),
                           category_id         BIGINT REFERENCES categories (id),
                           quality_grade_id    BIGINT REFERENCES quality_grades (id),
                           quantity_kg         NUMERIC(12, 2) NOT NULL,
                           image_url           VARCHAR(500),
                           status              VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
                           created_at          TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_crop_lots_farmer_id ON crop_lots (farmer_id);
CREATE INDEX idx_crop_lots_status ON crop_lots (status);

-- Many-to-Many join table backing CropLot.pestTags
CREATE TABLE crop_lot_pest_tag (
                                   crop_lot_id     BIGINT NOT NULL REFERENCES crop_lots (id) ON DELETE CASCADE,
                                   pest_tag_id     BIGINT NOT NULL REFERENCES pest_tags (id) ON DELETE CASCADE,
                                   PRIMARY KEY (crop_lot_id, pest_tag_id)
);