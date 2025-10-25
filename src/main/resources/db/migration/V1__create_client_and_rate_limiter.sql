- V1__Create_client_and_rate_limiter_tables.sql

-- Create client table
CREATE TABLE IF NOT EXISTS clients (
    id VARCHAR(255) PRIMARY KEY,
    tier VARCHAR(50) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create rate_limiter table
CREATE TABLE IF NOT EXISTS rate_limiters (
    id VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    endpoint VARCHAR(1024) NOT NULL,
    tier VARCHAR(50) NOT NULL,

    -- TokenBucket state
    current_bytes BIGINT NOT NULL,
    last_refill_time BIGINT NOT NULL,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_rate_limiter_client
        FOREIGN KEY(client_id)
        REFERENCES clients(id)
        ON DELETE CASCADE
);

-- Create blocked_clients table
CREATE TABLE IF NOT EXISTS blocked_clients (
    id VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    reason VARCHAR(500),
    blocked_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    unblock_at TIMESTAMP,

    CONSTRAINT fk_blocked_client
        FOREIGN KEY(client_id)
        REFERENCES clients(id)
        ON DELETE CASCADE
);

-- Create request_logs table
CREATE TABLE IF NOT EXISTS request_logs (
    id VARCHAR(255) PRIMARY KEY,
    client_id VARCHAR(255) NOT NULL,
    ip_address VARCHAR(45),
    endpoint VARCHAR(1024),
    http_method VARCHAR(10),
    request_size_bytes BIGINT NOT NULL,
    allowed BOOLEAN NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_request_log_client
        FOREIGN KEY(client_id)
        REFERENCES clients(id)
        ON DELETE CASCADE
);

-- Indexes for performance
CREATE INDEX idx_rate_limiters_client_id ON rate_limiters(client_id);
CREATE INDEX idx_rate_limiters_endpoint ON rate_limiters(endpoint);
CREATE INDEX idx_blocked_clients_client_id ON blocked_clients(client_id);
CREATE INDEX idx_blocked_clients_unblock_at ON blocked_clients(unblock_at);
CREATE INDEX idx_request_logs_client_id ON request_logs(client_id);
CREATE INDEX idx_request_logs_created_at ON request_logs(created_at);
