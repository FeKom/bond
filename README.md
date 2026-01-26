# Bond - Byte-based Token Bucket Rate Limiter

Um rate limiter baseado no algoritmo **Token Bucket** com consumo por **bytes comprimidos**. Diferente de rate limiters tradicionais que contam requisicoes, o Bond mede o tamanho real do payload comprimido (GZIP), permitindo um controle mais justo de banda.

## Visao Geral

O Bond permite limitar o uso de APIs baseado no volume de dados transferido, nao apenas no numero de requisicoes. Payloads maiores consomem mais tokens; payloads menores (ou que comprimem bem) consomem menos.

### Principais Caracteristicas

- **Token Bucket por Bytes**: Capacidade e refill medidos em bytes, nao em requisicoes
- **Compressao GZIP**: Payloads sao comprimidos antes de calcular o consumo
- **Sistema de Tiers**: FREE, STARTUP e ENTERPRISE com limites diferentes
- **Burst Support**: Permite picos temporarios acima da capacidade normal
- **Persistencia**: Estado do bucket salvo em PostgreSQL (JSONB)
- **Autenticacao**: Integrado com Keycloak (OAuth2/JWT)
- **Monitoramento**: Metricas Prometheus + Grafana inclusos

## Arquitetura

```
┌─────────────────────────────────────────────────────────────┐
│                      Security Layer                         │
│  ┌───────────────────────────────────────────────────────┐  │
│  │  Spring Security + OAuth2 Resource Server (JWT)       │  │
│  │  - Keycloak Integration                               │  │
│  │  - Role-based Access Control (ADMIN, USER)            │  │
│  └───────────────────────────────────────────────────────┘  │
├─────────────────────────────────────────────────────────────┤
│                        API Layer                            │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  │
│  │ ClientController│  │     RateLimiterController        │  │
│  │  POST /clients  │  │        POST /check               │  │
│  │  GET /clients/* │  │                                  │  │
│  └────────┬────────┘  └───────────────┬──────────────────┘  │
│           │                           │                     │
├───────────┼───────────────────────────┼─────────────────────┤
│           ▼                           ▼                     │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  │
│  │  ClientService  │  │       RateLimiterService         │  │
│  │                 │  │  - Busca/Cria RateLimiter        │  │
│  │                 │  │  - Verifica TokenBucket          │  │
│  └────────┬────────┘  └───────────────┬──────────────────┘  │
│           │                           │                     │
├───────────┼───────────────────────────┼─────────────────────┤
│           ▼                           ▼                     │
│  ┌─────────────────────────────────────────────────────┐    │
│  │              Domain Layer (Imutavel)                │    │
│  │  ┌─────────┐  ┌─────────────┐  ┌─────────────────┐  │    │
│  │  │ Client  │  │ RateLimiter │  │   TokenBucket   │  │    │
│  │  │ (record)│  │  (record)   │  │   (algoritmo)   │  │    │
│  │  └─────────┘  └─────────────┘  └─────────────────┘  │    │
│  └─────────────────────────────────────────────────────┘    │
│                                                             │
├─────────────────────────────────────────────────────────────┤
│                   Infrastructure Layer                      │
│  ┌─────────────────┐  ┌──────────────────────────────────┐  │
│  │  JPA Entities   │  │        Repositories              │  │
│  │  - Client       │  │  - ImpClientRepository           │  │
│  │  - RateLimiter  │  │  - RateLimiterRepository (JPA)   │  │
│  └─────────────────┘  └──────────────────────────────────┘  │
│                              │                              │
│                              ▼                              │
│                     ┌─────────────────┐                     │
│                     │   PostgreSQL    │                     │
│                     │    (JSONB)      │                     │
│                     └─────────────────┘                     │
└─────────────────────────────────────────────────────────────┘
```

## Sistema de Tiers

| Tier | Capacidade | Refill | Burst | Requests/hora* |
|------|------------|--------|-------|----------------|
| FREE | 32 KB | 32 KB/hora | 150% | ~432 |
| STARTUP | 10 MB | 10 MB/hora | 200% | ~140.000 |
| ENTERPRISE | 1 GB | 1 GB/hora | 300% | ~14.000.000 |

*Estimativa baseada em payload medio de ~75 bytes comprimidos

## Como Executar

### Pre-requisitos

- Java 21+
- Docker e Docker Compose
- Gradle (wrapper incluso)

### 1. Subir a Infraestrutura Completa

```bash
# Subir PostgreSQL, Redis e Keycloak
docker-compose up -d postgres redis keycloak
```

Aguarde o Keycloak inicializar (~30 segundos).

### 2. Build e Executar a Aplicacao

```bash
# Build
./gradlew build -x test

# Executar
./gradlew bootRun
```

A aplicacao inicia em `http://localhost:8080`

### 3. Autenticacao com Keycloak

O Bond utiliza Keycloak para autenticacao. Usuarios pre-configurados:

| Usuario | Senha | Role | Permissoes |
|---------|-------|------|------------|
| admin | admin123 | ADMIN | Criar clientes, verificar rate limit |
| user | user123 | USER | Apenas verificar rate limit |

#### Obter Token JWT

```bash
# Token para admin (pode criar clientes)
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/bond/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=bond-api" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" | jq -r '.access_token')

echo $TOKEN
```

### 4. Testar os Endpoints

#### Criar um Cliente (requer role ADMIN)

```bash
curl -X POST http://localhost:8080/clients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tier": "FREE"}'
```

Resposta:
```json
{
  "id": "API_KEY_019bf768-680a-7135-8a53-ea141f7755c8",
  "enabled": true,
  "createdAt": "2026-01-25T20:06:13.387285",
  "updatedAt": "2026-01-25T20:06:13.387285",
  "tier": "FREE",
  "message": "Client created successfully"
}
```

#### Verificar Rate Limit (requer autenticacao)

```bash
curl -X POST http://localhost:8080/check \
  -H "Authorization: Bearer $TOKEN" \
  -H "X-API-Key: API_KEY_019bf768-680a-7135-8a53-ea141f7755c8" \
  -H "Content-Type: application/json" \
  -d '{"data": "hello world"}'
```

Resposta (Permitido):
```json
{
  "allowed": true,
  "usagePercentage": 0.30,
  "waitTimeMs": 0
}
```

Headers retornados:
- `X-RateLimit-Used`: Bytes consumidos
- `X-RateLimit-Usage`: Porcentagem de uso

#### Quando o Limite e Excedido (HTTP 429)

```json
{
  "allowed": false,
  "usagePercentage": 99.85,
  "waitTimeMs": 5111
}
```

Header: `X-RateLimit-Reset-After: 5111` (ms para aguardar)

#### Buscar Cliente por ID

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/clients/API_KEY_019bf768-680a-7135-8a53-ea141f7755c8
```

#### Buscar Tier do Cliente

```bash
curl -H "Authorization: Bearer $TOKEN" \
  http://localhost:8080/clients/tier/API_KEY_019bf768-680a-7135-8a53-ea141f7755c8
```

### 5. Script de Teste Completo com Autenticacao

```bash
#!/bin/bash

echo "=== Obtendo token JWT ==="
TOKEN=$(curl -s -X POST "http://localhost:8180/realms/bond/protocol/openid-connect/token" \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "client_id=bond-api" \
  -d "username=admin" \
  -d "password=admin123" \
  -d "grant_type=password" | jq -r '.access_token')

if [ "$TOKEN" == "null" ] || [ -z "$TOKEN" ]; then
  echo "Erro ao obter token. Verifique se o Keycloak esta rodando."
  exit 1
fi
echo "Token obtido com sucesso!"

echo ""
echo "=== Criando cliente FREE ==="
RESPONSE=$(curl -s -X POST http://localhost:8080/clients \
  -H "Authorization: Bearer $TOKEN" \
  -H "Content-Type: application/json" \
  -d '{"tier": "FREE"}')
echo "$RESPONSE" | jq .

API_KEY=$(echo "$RESPONSE" | jq -r '.id')
echo "API_KEY: $API_KEY"

echo ""
echo "=== Testando Rate Limit (5 requests) ==="
for i in {1..5}; do
  echo "--- Request $i ---"
  curl -s -X POST http://localhost:8080/check \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-API-Key: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"data\": \"teste $i $RANDOM\"}" | jq .
done

echo ""
echo "=== Esgotando o limite ==="
for i in {1..300}; do
  RESPONSE=$(curl -s -X POST http://localhost:8080/check \
    -H "Authorization: Bearer $TOKEN" \
    -H "X-API-Key: $API_KEY" \
    -H "Content-Type: application/json" \
    -d "{\"data\": \"Request $i - $RANDOM $RANDOM\"}" \
    -w "|%{http_code}")
  
  HTTP_CODE=$(echo "$RESPONSE" | rev | cut -d'|' -f1 | rev)
  
  if [ "$HTTP_CODE" == "429" ]; then
    echo "RATE LIMIT ATINGIDO na request $i!"
    echo "$RESPONSE" | cut -d'|' -f1 | jq .
    break
  fi
  
  if [ $((i % 50)) -eq 0 ]; then
    echo "Request $i - HTTP $HTTP_CODE"
  fi
done
```

### 6. Subir Stack Completa (com Monitoramento)

```bash
docker-compose up -d
```

Servicos disponiveis:
- **API Bond**: http://localhost:7070
- **Swagger UI**: http://localhost:7070/swagger-ui.html
- **Keycloak**: http://localhost:8180 (admin console: admin/admin)
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **PostgreSQL**: localhost:5432
- **Redis**: localhost:6379

## Testes

O projeto inclui uma suite completa de testes:

### Executar Todos os Testes

```bash
./gradlew test
```

### Tipos de Testes

| Tipo | Descricao | Arquivos |
|------|-----------|----------|
| **Unit Tests** | Testes unitarios com Mockito | `TokenBucketTest`, `ClientServiceTest`, `RateLimiterServiceTest` |
| **Integration Tests** | Testes de controllers com MockMvc | `ClientControllerTest`, `RateLimiterControllerTest` |
| **E2E Tests** | Testes end-to-end com Testcontainers | `E2EIntegrationTest` |

### Cobertura de Testes

- **TokenBucket**: Algoritmo de consumo, refill, burst, serializacao JSON
- **Services**: Criacao de clientes, verificacao de rate limit, busca por ID
- **Controllers**: Autenticacao (401), autorizacao (403), requests validas, validacao de entrada
- **E2E**: Fluxo completo com banco de dados real (PostgreSQL via Testcontainers)

## Estrutura do Projeto

```
bond/
├── src/main/java/github/fekom/bond/
│   ├── algorithms/
│   │   └── TokenBucket.java           # Algoritmo Token Bucket
│   ├── api/
│   │   ├── ClientService.java         # Logica de negocios - Clientes
│   │   ├── RateLimiterService.java    # Logica de negocios - Rate Limit
│   │   └── dto/                       # Request/Response DTOs
│   ├── config/
│   │   └── SecurityConfig.java        # Configuracao Spring Security + OAuth2
│   ├── domain/
│   │   ├── entities/                  # Records imutaveis (Client, RateLimiter)
│   │   └── enums/TierType.java        # Configuracao dos Tiers
│   ├── infrastructure/
│   │   ├── persistence/               # Entidades JPA
│   │   ├── repository/                # Implementacoes de repositorio
│   │   └── web/                       # Controllers REST
│   └── resolver/
│       ├── GlobalExceptionHandler.java # Tratamento global de erros
│       ├── ClientIPResolver.java      # Extrai IP do cliente
│       └── PayloadCompressor.java     # Compressao GZIP
├── src/main/resources/
│   ├── application.yml                # Configuracao principal
│   └── db/migration/                  # Migrations Flyway
├── src/test/java/github/fekom/bond/
│   ├── algorithms/
│   │   └── TokenBucketTest.java       # Testes unitarios TokenBucket
│   ├── api/
│   │   ├── ClientServiceTest.java     # Testes unitarios ClientService
│   │   └── RateLimiterServiceTest.java # Testes unitarios RateLimiterService
│   ├── config/
│   │   └── TestSecurityConfig.java    # Configuracao de seguranca para testes
│   ├── infrastructure/web/
│   │   ├── ClientControllerTest.java  # Testes de integracao
│   │   └── RateLimiterControllerTest.java
│   └── E2EIntegrationTest.java        # Testes E2E com Testcontainers
├── docker/
│   └── keycloak/
│       └── bond-realm.json            # Configuracao do realm Keycloak
├── compose.yaml                       # Docker Compose
├── Dockerfile                         # Build da aplicacao
└── build.gradle.kts                   # Dependencias Gradle
```

## Calculo do Consumo de Tokens

```
1. Request recebida com payload JSON
                 │
                 ▼
2. Payload comprimido com GZIP
   (ex: 1000 bytes → 150 bytes comprimidos)
                 │
                 ▼
3. Tamanho comprimido = tokens a consumir
   (150 bytes = 150 tokens)
                 │
                 ▼
4. TokenBucket verifica disponibilidade
   ├─ Se tokens >= 150 → PERMITIDO (200 OK)
   └─ Se tokens < 150  → BLOQUEADO (429 Too Many Requests)
                 │
                 ▼
5. Estado do bucket persistido no PostgreSQL (JSONB)
```

## Seguranca

### Endpoints e Permissoes

| Endpoint | Metodo | Permissao | Descricao |
|----------|--------|-----------|-----------|
| `/clients` | POST | ROLE_ADMIN | Criar novo cliente |
| `/clients/{id}` | GET | Authenticated | Buscar cliente por ID |
| `/clients/tier/{id}` | GET | Authenticated | Buscar tier do cliente |
| `/check` | POST | Authenticated | Verificar rate limit |
| `/actuator/health` | GET | Public | Health check |
| `/swagger-ui/**` | GET | Public | Documentacao API |

### Configuracao OAuth2

O Bond atua como **Resource Server** OAuth2, validando tokens JWT emitidos pelo Keycloak.

```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/bond
```

## Configuracao

### application.yml

```yaml
server:
  port: 8080

spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/bond
    username: postgres
    password: postgres
  jpa:
    hibernate:
      ddl-auto: update
  flyway:
    baseline-on-migrate: true
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: http://localhost:8180/realms/bond
```

### Variaveis de Ambiente (Docker)

```yaml
SPRING_PROFILES_ACTIVE: docker
SPRING_DATASOURCE_URL: jdbc:postgresql://postgres:5432/bond
SPRING_DATASOURCE_USERNAME: postgres
SPRING_DATASOURCE_PASSWORD: postgres
SPRING_REDIS_HOST: redis
SPRING_SECURITY_OAUTH2_RESOURCESERVER_JWT_ISSUER_URI: http://keycloak:8080/realms/bond
```

## Tecnologias Utilizadas

- **Java 21** com preview features
- **Spring Boot 3.2.0**
- **Spring Security** + OAuth2 Resource Server
- **Spring Data JPA** + Hibernate
- **PostgreSQL 15** com JSONB
- **Redis 7** (preparado para cache distribuido)
- **Keycloak 23** para autenticacao
- **Flyway** para migrations
- **Micrometer** + Prometheus para metricas
- **SpringDoc OpenAPI** para documentacao
- **JUnit 5** + Mockito para testes
- **Testcontainers** para testes de integracao

## Licenca

Projeto experimental - GitHub/FeKom
