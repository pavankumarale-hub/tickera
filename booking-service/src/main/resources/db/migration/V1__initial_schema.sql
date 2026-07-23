-- ============================================================
-- Axon Framework 4.9 JPA schema (PostgreSQL)
--
-- Rules derived from Axon's own entity annotations:
--   - Binary columns (@Lob byte[]) must be OID, not BYTEA — Hibernate 6 maps
--     @Lob byte[] to PostgreSQL oid by default.
--   - global_index uses @SequenceGenerator(sequenceName="domain_event_entry_seq").
--     BIGSERIAL would create domain_event_entry_global_index_seq (wrong name).
--   - association_value_entry.id similarly needs its own named sequence.
-- ============================================================

CREATE SEQUENCE domain_event_entry_seq;

CREATE TABLE domain_event_entry (
    global_index         BIGINT       NOT NULL,
    event_identifier     VARCHAR(255) NOT NULL,
    meta_data            OID,
    payload              OID          NOT NULL,
    payload_revision     VARCHAR(255),
    payload_type         VARCHAR(255) NOT NULL,
    time_stamp           VARCHAR(255) NOT NULL,
    aggregate_identifier VARCHAR(255) NOT NULL,
    sequence_number      BIGINT,
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
    meta_data            OID,
    payload              OID          NOT NULL,
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
    token          OID,
    token_type     VARCHAR(255),
    PRIMARY KEY (processor_name, segment)
);

CREATE TABLE saga_entry (
    saga_id         VARCHAR(255) NOT NULL,
    revision        VARCHAR(255),
    saga_type       VARCHAR(255),
    serialized_saga OID,
    PRIMARY KEY (saga_id)
);

CREATE SEQUENCE association_value_entry_seq;

CREATE TABLE association_value_entry (
    id                BIGINT       NOT NULL,
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
