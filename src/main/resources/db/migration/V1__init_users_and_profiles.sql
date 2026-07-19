-- Base identity table backing domain/user/User.java (InheritanceType.JOINED)
CREATE TABLE users (
                       id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
                       user_type       VARCHAR(20) NOT NULL,
                       email           VARCHAR(255) NOT NULL UNIQUE,
                       password_hash   VARCHAR(255) NOT NULL,
                       role            VARCHAR(20) NOT NULL,
                       kyc_verified    BOOLEAN NOT NULL DEFAULT FALSE,
                       enabled         BOOLEAN NOT NULL DEFAULT TRUE,
                       created_at      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_users_email ON users (email);

-- domain/user/FarmerProfile.java
CREATE TABLE farmer_profiles (
                                 id                  BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                                 fpo_affiliation     VARCHAR(255),
                                 district            VARCHAR(120) NOT NULL,
                                 state               VARCHAR(120) NOT NULL
);

-- domain/user/DistributorProfile.java
CREATE TABLE distributor_profiles (
                                      id                          BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                                      business_license_number    VARCHAR(120) NOT NULL UNIQUE,
                                      warehouse_region           VARCHAR(120)
);

-- domain/user/AgronomistProfile.java
CREATE TABLE agronomist_profiles (
                                     id                      BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE,
                                     certification_number   VARCHAR(120) NOT NULL UNIQUE,
                                     specialization          VARCHAR(255)
);