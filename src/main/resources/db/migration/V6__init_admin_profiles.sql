-- domain/user/AdminProfile.java — required for JOINED inheritance to
-- work at all for ADMIN users. No extra columns beyond the shared PK,
-- since AdminProfile deliberately carries no additional fields.
CREATE TABLE admin_profiles (
    id BIGINT PRIMARY KEY REFERENCES users (id) ON DELETE CASCADE
);