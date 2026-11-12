ALTER TABLE users
    ADD COLUMN updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP;

ALTER TABLE users
    ADD CONSTRAINT chk_users_account_status
        CHECK (account_status IN ('ACTIVE', 'SUSPENDED'));

ALTER TABLE user_roles
    ADD CONSTRAINT chk_user_roles_role
        CHECK (role IN ('DRIVER', 'HOST', 'ADMIN'));

