CREATE TABLE tickets (
    id          BIGINT          NOT NULL AUTO_INCREMENT PRIMARY KEY,
    title       VARCHAR(200)    NOT NULL,
    description TEXT            NULL,
    status      VARCHAR(20)     NOT NULL DEFAULT 'open',
    priority    VARCHAR(10)     NOT NULL DEFAULT 'medium',
    -- Stored in UTC: Hibernate is configured with jdbc.time_zone = UTC.
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT tickets_status_valid CHECK (status IN ('open', 'in_progress', 'closed')),
    CONSTRAINT tickets_priority_valid CHECK (priority IN ('low', 'medium', 'high')),
    -- Serve the default listing and the status-filtered listing in newest-first order.
    INDEX idx_tickets_created (created_at DESC, id DESC),
    INDEX idx_tickets_status_created (status, created_at DESC, id DESC)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci;
