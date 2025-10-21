-- Script de inicialização do PostgreSQL
CREATE TABLE IF NOT EXISTS bond_configs (
    id BIGSERIAL PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    endpoint VARCHAR(255),
    requests_limit INTEGER,
    period_seconds INTEGER,
    algorithm VARCHAR(50),
    enabled BOOLEAN DEFAULT true,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO bond_configs (api_key, endpoint, requests_limit, period_seconds, algorithm) 
VALUES 
    ('test-key-1', '/api/users', 1000, 3600, 'token-bucket'),
    ('test-key-2', '/api/products', 500, 1800, 'sliding-window')
ON CONFLICT (api_key) DO NOTHING;