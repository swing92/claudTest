CREATE TABLE company (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    name VARCHAR(200) NOT NULL,
    industry VARCHAR(100),
    url VARCHAR(500),
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_company_user_id ON company (user_id);

CREATE TABLE job_application (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL REFERENCES company (id),
    position_title VARCHAR(200) NOT NULL,
    apply_url VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'PREPARING'
        CHECK (status IN ('PREPARING', 'APPLIED', 'DOCUMENT_PASSED', 'INTERVIEW', 'FINAL_PASSED', 'REJECTED')),
    applied_at DATE,
    notes VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_application_user_id ON job_application (user_id);
CREATE INDEX idx_job_application_company_id ON job_application (company_id);

-- user_id here is intentionally denormalized from job_application.user_id (filtering without a join,
-- consistent with "every table carries user_id"). A DB constraint can't cross-check it against the
-- parent row's user_id, so JobApplicationEventService must verify the two match before insert/update.
CREATE TABLE job_application_event (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    job_application_id BIGINT NOT NULL REFERENCES job_application (id),
    event_type VARCHAR(30) NOT NULL
        CHECK (event_type IN ('DOCUMENT_SUBMITTED', 'DOCUMENT_RESULT', 'INTERVIEW', 'FINAL_RESULT')),
    event_date DATE NOT NULL,
    result VARCHAR(10) NOT NULL DEFAULT 'PENDING' CHECK (result IN ('PENDING', 'PASS', 'FAIL')),
    memo VARCHAR(2000),
    created_at TIMESTAMP NOT NULL DEFAULT now(),
    updated_at TIMESTAMP NOT NULL DEFAULT now()
);
CREATE INDEX idx_job_application_event_user_id ON job_application_event (user_id);
CREATE INDEX idx_job_application_event_job_application_id ON job_application_event (job_application_id);
