CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

CREATE TABLE IF NOT EXISTS customers (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    name            VARCHAR(100)      NOT NULL,
    contact_channel VARCHAR(50)       NOT NULL,
    created_at      TIMESTAMP         NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS policies (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    customer_id    UUID             NOT NULL REFERENCES customers(id),
    number         VARCHAR(50)      NOT NULL UNIQUE,
    product_type   VARCHAR(50)      NOT NULL,
    effective_from DATE             NOT NULL,
    effective_to   DATE             NOT NULL,
    created_at     TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS claims (
    id              BIGSERIAL PRIMARY KEY,
    claim_number    VARCHAR(30)      NOT NULL UNIQUE,
    policy_number   VARCHAR(50)      NOT NULL,
    customer_id     UUID             NULL REFERENCES customers(id),
    claimed_amount  NUMERIC(15,2)    NOT NULL,
    status          VARCHAR(30)      NOT NULL,
    description     TEXT             NOT NULL,
    accident_date   DATE             NOT NULL,
    claimant_name   VARCHAR(100)     NOT NULL,
    email           VARCHAR(200)     NULL,
    submitted_at    TIMESTAMP        NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP        NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS claim_events (
    id           UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id     BIGINT          NOT NULL REFERENCES claims(id),
    type         VARCHAR(50)     NOT NULL,
    payload      JSONB           NOT NULL,
    occurred_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE IF NOT EXISTS reviews (
    claim_id    BIGINT       PRIMARY KEY REFERENCES claims(id),
    decision    VARCHAR(30)  NOT NULL,
    reviewer    VARCHAR(100) NOT NULL,
    decided_at  TIMESTAMP    NOT NULL,
    reason      TEXT         NULL
);

CREATE TABLE IF NOT EXISTS payments (
    id             UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id       BIGINT       NOT NULL REFERENCES claims(id),
    amount         NUMERIC(15,2) NOT NULL,
    method         VARCHAR(30)   NOT NULL,
    payout_status  VARCHAR(30)   NOT NULL,
    processed_at   TIMESTAMP     NULL
);

CREATE TABLE IF NOT EXISTS notifications (
    id              UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id        BIGINT       NOT NULL REFERENCES claims(id),
    channel         VARCHAR(30)  NOT NULL,
    template        VARCHAR(50)  NOT NULL,
    delivery_status VARCHAR(30)  NOT NULL,
    sent_at         TIMESTAMP    NULL
);

CREATE TABLE IF NOT EXISTS audit_logs (
    id        UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    claim_id  BIGINT       NOT NULL REFERENCES claims(id),
    actor     VARCHAR(100) NOT NULL,
    action    VARCHAR(50)  NOT NULL,
    metadata  JSONB        NOT NULL,
    logged_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_claims_policy_status ON claims (policy_number, status);
CREATE INDEX IF NOT EXISTS idx_claims_submitted_at ON claims (submitted_at DESC);
CREATE INDEX IF NOT EXISTS idx_claim_events_type ON claim_events (type);
CREATE INDEX IF NOT EXISTS idx_payments_claim_id ON payments (claim_id);
CREATE INDEX IF NOT EXISTS idx_notifications_claim_id ON notifications (claim_id);
