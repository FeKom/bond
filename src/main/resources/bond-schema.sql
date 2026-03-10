-- Bond Schema Reference
-- This file is NOT executed automatically. It is provided as a reference
-- for consumers who use JpaBucketStore and need to create the required tables.

CREATE TABLE IF NOT EXISTS clients (
    id VARCHAR(255) PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    capacity_bytes BIGINT NOT NULL,
    refill_rate_bytes_per_second BIGINT NOT NULL,
    burst_multiplier DOUBLE PRECISION NOT NULL,
    created_at VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS requests (
    id VARCHAR(255) PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL,
    endpoint VARCHAR(1024) NOT NULL,
    bucket JSONB,
    created_at VARCHAR(50) NOT NULL,
    updated_at VARCHAR(50) NOT NULL
);

CREATE TABLE IF NOT EXISTS blocked_clients (
    id VARCHAR(255) PRIMARY KEY,
    ip_address VARCHAR(45) NOT NULL UNIQUE,
    reason VARCHAR(500),
    blocked_at VARCHAR(50) NOT NULL
);

CREATE INDEX idx_requests_ip_address ON requests(ip_address);
CREATE INDEX idx_requests_endpoint ON requests(endpoint);
CREATE INDEX idx_clients_ip_address ON clients(ip_address);
CREATE INDEX idx_blocked_clients_ip_address ON blocked_clients(ip_address);
