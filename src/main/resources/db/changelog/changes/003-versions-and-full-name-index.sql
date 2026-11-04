--liquibase formatted sql

--changeset s465592:003-coordinates-version
ALTER TABLE coordinates ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE coordinates DROP COLUMN version;

--changeset s465592:003-addresses-version
ALTER TABLE addresses ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
--rollback ALTER TABLE addresses DROP COLUMN version;

--changeset s465592:003-organizations-full-name-index
CREATE INDEX organizations_full_name_lower_idx ON organizations (lower(full_name));
--rollback DROP INDEX organizations_full_name_lower_idx;
