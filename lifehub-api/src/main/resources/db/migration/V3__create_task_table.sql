CREATE TABLE task (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(200) NOT NULL,
    description VARCHAR(2000),
    type VARCHAR(10) NOT NULL CHECK (type IN ('TODO', 'EVENT')),
    due_at TIMESTAMP,
    start_at TIMESTAMP,
    end_at TIMESTAMP,
    is_all_day BOOLEAN NOT NULL DEFAULT false,
    is_completed BOOLEAN NOT NULL DEFAULT false,
    completed_at TIMESTAMP,
    priority VARCHAR(10) NOT NULL DEFAULT 'MEDIUM' CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH')),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_task_user_id ON task (user_id);
CREATE INDEX idx_task_due_at ON task (due_at);
CREATE INDEX idx_task_start_at ON task (start_at);
