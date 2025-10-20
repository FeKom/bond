# Construir e iniciar todos os serviços
docker-compose up --build

# Executar em background
docker-compose up -d --build

# Ver logs da aplicação
docker-compose logs rate-limiter-app -f

# Ver logs do PostgreSQL
docker-compose logs postgres -f

# Parar todos os serviços
docker-compose down

# Parar e remover volumes (dados)
docker-compose down -v

# Recriar apenas a aplicação
docker-compose up -d --build rate-limiter-app

# Executar testes
docker-compose exec rate-limiter-app ./gradlew test


# Aplicação
curl http://localhost:8080/actuator/health

# Prometheus
curl http://localhost:9090/-/healthy

# Redis
docker-compose exec redis redis-cli ping

### Visualize Source
rate-limiter-platform/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── ratelimiter/
│       │           ├── RateLimiterApplication.java
│       │           ├── config/
│       │           │   ├── RedisConfig.java
│       │           │   ├── WebConfig.java
│       │           │   └── RateLimitProperties.java
│       │           ├── controller/
│       │           │   ├── RateLimitController.java
│       │           │   ├── AdminController.java
│       │           │   └── MetricsController.java
│       │           ├── service/
│       │           │   ├── RateLimitService.java
│       │           │   ├── RedisRateLimitService.java
│       │           │   ├── TokenBucketService.java
│       │           │   └── SlidingWindowService.java
│       │           ├── model/
│       │           │   ├── RateLimitRequest.java
│       │           │   ├── RateLimitResponse.java
│       │           │   ├── RateLimitConfig.java
│       │           │   └── ApiKey.java
│       │           ├── interceptor/
│       │           │   └── RateLimitInterceptor.java
│       │           ├── annotation/
│       │           │   └── RateLimited.java
│       │           ├── repository/
│       │           │   └── ApiKeyRepository.java
│       │           └── exception/
│       │               ├── RateLimitExceededException.java
│       │               └── GlobalExceptionHandler.java
│       └── resources/
│           ├── application.yml
│           ├── application-dev.yml
│           └── static
├── docker/
│   └── docker-compose.yml
├── pom.xml
├── Dockerfile
└── README.md