erDiagram
    CLIENTS ||--o{ RATE_LIMITERS : has
    CLIENTS ||--o{ BLOCKED_CLIENTS : has
    RATE_LIMITERS ||--o{ REQUEST_LOGS : has

    CLIENTS {
        string id PK "API_KEY_..."
        string tier "FREE, STARTUP, ENTERPRISE"
        boolean enabled
        timestamp createdAt
        timestamp updatedAt
    }

    RATE_LIMITERS {
        string id PK "UUID v7"
        string clientId FK "referencia CLIENTS"
        string endpoint "/api/users"
        string tier "FREE, STARTUP, ENTERPRISE"
        long currentBytes "bytes disponíveis agora"
        long lastRefillTime "timestamp do último refill"
        timestamp createdAt
        timestamp updatedAt
    }

    BLOCKED_CLIENTS {
        string id PK "UUID v7"
        string clientId FK "referencia CLIENTS"
        string reason "DDoS detected, Abusive..."
        timestamp blockedAt
        timestamp unblockAt "null = permanente"
    }

    REQUEST_LOGS {
        string id PK "UUID v7"
        string clientId FK "referencia CLIENTS"
        string rateLimiterId FK "referencia RATE_LIMITERS"
        string ipAddress "192.168.1.100"
        string endpoint "/api/users"
        long requestSizeBytes
        boolean allowed
        timestamp createdAt
    }