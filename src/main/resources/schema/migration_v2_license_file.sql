-- Migrate from DB-stored license claims to file-based active.lic storage.
-- Signed license limits/expiry are read from disk and verified on every check.

DROP TABLE IF EXISTS active_license;
DROP TABLE IF EXISTS license_binding;
DROP TABLE IF EXISTS server_identity;

-- After this migration, re-upload the .lic file to populate data/license/active.lic
