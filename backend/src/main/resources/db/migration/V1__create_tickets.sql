CREATE TABLE tickets (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    customer_email  VARCHAR(320) NOT NULL,
    subject         VARCHAR(200) NOT NULL,
    body            TEXT         NOT NULL,
    category        VARCHAR(20)  NOT NULL
        CHECK (category IN ('BILLING', 'TECHNICAL', 'ACCOUNT', 'FEEDBACK', 'OTHER')),
    priority        VARCHAR(20)  NOT NULL
        CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'URGENT')),
    sentiment       VARCHAR(20)  NOT NULL
        CHECK (sentiment IN ('POSITIVE', 'NEUTRAL', 'NEGATIVE')),
    summary         VARCHAR(500),
    suggested_reply TEXT,
    status          VARCHAR(20)  NOT NULL DEFAULT 'OPEN'
        CHECK (status IN ('OPEN', 'RESOLVED')),
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- The list endpoint sorts newest first and is commonly filtered by status.
CREATE INDEX idx_tickets_created_at ON tickets (created_at DESC);
CREATE INDEX idx_tickets_status ON tickets (status);
