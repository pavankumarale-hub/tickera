-- ============================================================
-- Axon Framework 4.9 JPA schema (PostgreSQL)
-- payment-service has no saga, so no saga_entry / association tables.
-- ============================================================

CREATE TABLE domain_event_entry (
    global_index         BIGSERIAL    NOT NULL,
    event_identifier     VARCHAR(255) NOT NULL,
    meta_data            BYTEA,
    payload              BYTEA        NOT NULL,
    payload_revision     VARCHAR(255),
    payload_type         VARCHAR(255) NOT NULL,
    time_stamp           VARCHAR(255) NOT NULL,
    aggregate_identifier VARCHAR(255) NOT NULL,
    sequence_number      BIGINT       NOT NULL,
    type                 VARCHAR(255),
    PRIMARY KEY (global_index),
    UNIQUE (aggregate_identifier, sequence_number, type),
    UNIQUE (event_identifier)
);

CREATE TABLE snapshot_event_entry (
    aggregate_identifier VARCHAR(255) NOT NULL,
    sequence_number      BIGINT       NOT NULL,
    type                 VARCHAR(255) NOT NULL,
    event_identifier     VARCHAR(255) NOT NULL,
    meta_data            BYTEA,
    payload              BYTEA        NOT NULL,
    payload_revision     VARCHAR(255),
    payload_type         VARCHAR(255) NOT NULL,
    time_stamp           VARCHAR(255) NOT NULL,
    PRIMARY KEY (aggregate_identifier, sequence_number, type),
    UNIQUE (event_identifier)
);

CREATE TABLE token_entry (
    processor_name VARCHAR(255) NOT NULL,
    segment        INTEGER      NOT NULL,
    owner          VARCHAR(255),
    timestamp      VARCHAR(255) NOT NULL,
    token          BYTEA,
    token_type     VARCHAR(255),
    PRIMARY KEY (processor_name, segment)
);

-- ============================================================
-- Application read model
-- ============================================================

CREATE TABLE payment_summary (
    payment_id VARCHAR(255) NOT NULL PRIMARY KEY,
    booking_id VARCHAR(255),
    amount     NUMERIC(19, 2),
    currency   VARCHAR(3),
    status     VARCHAR(20)  NOT NULL,
    reason     VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE
);
