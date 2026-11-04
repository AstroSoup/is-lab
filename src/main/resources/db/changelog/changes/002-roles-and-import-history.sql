--liquibase formatted sql

--changeset s465592:002-user-role
ALTER TABLE users ADD COLUMN role VARCHAR(16) NOT NULL DEFAULT 'USER';
ALTER TABLE users ADD CONSTRAINT users_role_valid CHECK (role IN ('USER', 'ADMIN'));
--rollback ALTER TABLE users DROP COLUMN role;

--changeset s465592:002-create-import-operations
-- История операций импорта. Запись о неуспешной операции хранится без числа добавленных объектов.
-- Ограничение уникальности fullName организации в БД намеренно НЕ создаётся:
-- оно проверяется на уровне бизнес-логики приложения (см. UniquenessRules).
CREATE TABLE import_operations (
    id            BIGSERIAL   PRIMARY KEY,
    user_name     TEXT        NOT NULL REFERENCES users (name) ON DELETE RESTRICT,
    status        VARCHAR(16) NOT NULL,
    added_count   INTEGER,
    file_name     TEXT,
    error_message TEXT,
    created_at    TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT import_operations_id_positive  CHECK (id > 0),
    CONSTRAINT import_operations_status_valid CHECK (status IN ('SUCCESS', 'FAILED')),
    CONSTRAINT import_operations_count_consistent CHECK (
        (status = 'SUCCESS' AND added_count IS NOT NULL AND added_count >= 0)
        OR (status = 'FAILED' AND added_count IS NULL))
);
CREATE INDEX import_operations_user_idx ON import_operations (user_name, id DESC);
--rollback DROP TABLE import_operations;
