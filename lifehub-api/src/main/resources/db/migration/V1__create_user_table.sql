CREATE TABLE app_user (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) NOT NULL UNIQUE,
    name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);

-- v1 personal mode: single seed user, becomes id = 1 as the only row in a fresh DB.
-- lifehub.security.seed-user-id (application.yml) must match this row's id.
INSERT INTO app_user (email, name) VALUES ('me@lifehub.local', 'Me');
