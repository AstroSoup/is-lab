--liquibase formatted sql

--changeset s465592:001-create-coordinates
CREATE TABLE coordinates (
    id BIGSERIAL PRIMARY KEY,
    x  DOUBLE PRECISION NOT NULL,
    y  INTEGER NOT NULL,
    CONSTRAINT coordinates_id_positive CHECK (id > 0),
    CONSTRAINT coordinates_x_max       CHECK (x <= 580),
    CONSTRAINT coordinates_x_finite    CHECK (x > '-Infinity'::double precision),
    CONSTRAINT coordinates_y_max       CHECK (y <= 885)
);
--rollback DROP TABLE coordinates;

--changeset s465592:001-create-addresses
CREATE TABLE addresses (
    id     BIGSERIAL PRIMARY KEY,
    street TEXT,
    CONSTRAINT addresses_id_positive    CHECK (id > 0),
    CONSTRAINT addresses_street_nonblank CHECK (street IS NULL OR btrim(street) <> '')
);
--rollback DROP TABLE addresses;

--changeset s465592:001-create-organizations
CREATE TABLE organizations (
    id                  BIGSERIAL PRIMARY KEY,
    name                TEXT      NOT NULL,
    coordinates_id      BIGINT    NOT NULL REFERENCES coordinates (id) ON DELETE RESTRICT,
    creation_date       TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    official_address_id BIGINT    REFERENCES addresses (id) ON DELETE RESTRICT,
    annual_turnover     BIGINT    NOT NULL,
    employees_count     INTEGER   NOT NULL,
    rating              INTEGER   NOT NULL,
    full_name           TEXT,
    organization_type   VARCHAR(32),
    postal_address_id   BIGINT    REFERENCES addresses (id) ON DELETE RESTRICT,
    version             BIGINT    NOT NULL DEFAULT 0,
    CONSTRAINT organizations_id_positive       CHECK (id > 0),
    CONSTRAINT organizations_name_nonblank     CHECK (btrim(name) <> ''),
    CONSTRAINT organizations_turnover_positive CHECK (annual_turnover > 0),
    CONSTRAINT organizations_employees_positive CHECK (employees_count > 0),
    CONSTRAINT organizations_rating_positive   CHECK (rating > 0),
    CONSTRAINT organizations_fullname_nonblank CHECK (full_name IS NULL OR btrim(full_name) <> ''),
    CONSTRAINT organizations_type_valid        CHECK (organization_type IS NULL OR organization_type IN
        ('COMMERCIAL', 'PUBLIC', 'GOVERNMENT', 'TRUST', 'PRIVATE_LIMITED_COMPANY'))
);
CREATE INDEX organizations_coordinates_idx ON organizations (coordinates_id);
CREATE INDEX organizations_official_address_idx ON organizations (official_address_id);
CREATE INDEX organizations_postal_address_idx ON organizations (postal_address_id);
--rollback DROP TABLE organizations;

--changeset s465592:001-create-users
CREATE TABLE users (
    name          TEXT PRIMARY KEY,
    password_hash TEXT NOT NULL,
    CONSTRAINT users_name_nonblank CHECK (btrim(name) <> '')
);
--rollback DROP TABLE users;

