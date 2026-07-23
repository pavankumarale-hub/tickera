-- ============================================================
-- Axon Framework 4.9 JPA schema (PostgreSQL)
-- Event store, tracking tokens, and saga tables.
-- ============================================================

-- Event store: one row per domain event, append-only.
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

-- Snapshots reduce replay time for long-lived aggregates.
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

-- Tracking token per processor: allows each event-handler group
-- to replay from its own position independently.
CREATE TABLE token_entry (
    processor_name VARCHAR(255) NOT NULL,
    segment        INTEGER      NOT NULL,
    owner          VARCHAR(255),
    timestamp      VARCHAR(255) NOT NULL,
    token          BYTEA,
    token_type     VARCHAR(255),
    PRIMARY KEY (processor_name, segment)
);

-- Saga persistent state (BookingSaga).
CREATE TABLE saga_entry (
    saga_id         VARCHAR(255) NOT NULL,
    revision        VARCHAR(255),
    saga_type       VARCHAR(255),
    serialized_saga BYTEA,
    PRIMARY KEY (saga_id)
);

-- Association values let Axon look up the right saga instance
-- by a business key (e.g. bookingId).
CREATE TABLE association_value_entry (
    id                BIGSERIAL    NOT NULL,
    association_key   VARCHAR(255) NOT NULL,
    association_value VARCHAR(255),
    saga_id           VARCHAR(255),
    saga_type         VARCHAR(255),
    PRIMARY KEY (id)
);

CREATE INDEX ixk_ave_saga_type_key_value
    ON association_value_entry (saga_type, association_key, association_value);

CREATE INDEX ixk_ave_saga_id
    ON association_value_entry (saga_id);

-- ============================================================
-- Application read model
-- ============================================================

CREATE TABLE booking_summary (
    booking_id  VARCHAR(255) NOT NULL PRIMARY KEY,
    customer_id VARCHAR(255),
    event_name  VARCHAR(255),
    seats       INTEGER      NOT NULL,
    amount      NUMERIC(19, 2),
    currency    VARCHAR(3),
    status      VARCHAR(20)  NOT NULL,
    payment_id  VARCHAR(255),
    updated_at  TIMESTAMP WITH TIME ZONE
);
