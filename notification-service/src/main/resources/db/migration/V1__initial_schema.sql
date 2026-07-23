-- notification-service has no Axon dependency — only the application table.

CREATE TABLE notification (
    id         BIGSERIAL    NOT NULL PRIMARY KEY,
    booking_id VARCHAR(255),
    channel    VARCHAR(50)  NOT NULL,
    message    TEXT         NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX ix_notification_booking_id ON notification (booking_id);
