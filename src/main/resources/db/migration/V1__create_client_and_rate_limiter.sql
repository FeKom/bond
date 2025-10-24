-- Flyway migration: create client and rate_limiter tables
-- Version: 1

-- Create client table
CREATE TABLE IF NOT EXISTS client (
    id VARCHAR(255) PRIMARY KEY,
    tier VARCHAR(255),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE
);

-- Create rate_limiter table
CREATE TABLE IF NOT EXISTS rate_limiter (
    id VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    end_point VARCHAR(1024),

    -- TokenBucket representation (expanded fields)
    bucket_tier VARCHAR(255),
    bucket_capacity BIGINT,
    bucket_refill_rate BIGINT,
    bucket_burst_multiplier DOUBLE PRECISION,
    bucket_max_burst BIGINT,
    bucket_current_bytes BIGINT,
    bucket_last_refill_time BIGINT,
    bucket_created_at TIMESTAMP WITHOUT TIME ZONE,

    created_at TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE,

    CONSTRAINT fk_rate_limiter_client
      FOREIGN KEY(client_id)
      REFERENCES client(id)
      ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX IF NOT EXISTS idx_rate_limiter_client_id ON rate_limiter(client_id);
CREATE INDEX IF NOT EXISTS idx_rate_limiter_endpoint ON rate_limiter(end_point);
