CREATE TABLE disputes (
    id              BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    order_id        BIGINT NOT NULL REFERENCES orders (id),
    raised_by       BIGINT NOT NULL REFERENCES users (id),
    reason          VARCHAR(1000) NOT NULL,
    status          VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    reviewed_by     BIGINT REFERENCES users (id),
    reviewed_at     TIMESTAMP WITH TIME ZONE,
    review_note     VARCHAR(1000),
    created_at      TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_disputes_status ON disputes (status);
