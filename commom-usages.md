# Construir e iniciar todos os serviços
docker-compose up --build

# Executar em background
docker-compose up -d --build

# Ver logs da aplicação
docker-compose logs rate-limiter-app -f

# Ver logs do PostgreSQL
docker-compose logs postgres -f

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
src/main
├── java/github/fekom/bond
│   ├── algorithms
│   │   ├── TokenBucket.java
│   │   └── RateLimiter.java
│   │
│   ├── config
│   │   └── RateLimiterConfig.java
│   │
│   ├── controller
│   │   └── BondController.java
│   │
│   ├── domain
│   │   ├── dto
│   │   │   ├── RequestPayload.java
│   │   │   └── RateLimitResult.java
│   │   │
│   │   └── model
│   │       ├── Client.java
│   │       ├── BlockedClient.java
│   │       └── RequestLog.java
│   │
│   ├── entities
│   │   ├── ClientEntity.java
│   │   ├── BlockedClientEntity.java
│   │   └── RequestLogEntity.java
│   │
│   ├── enums
│   │   └── TierType.java
│   │
│   ├── infrastructure/persistence
│   │   ├── ClientRepository.java
│   │   ├── BlockedClientRepository.java
│   │   └── RequestLogRepository.java
│   │
│   ├── resolver
│   │   └── ClientIPResolver.java
│   │
│   ├── service
│   │   ├── RateLimiterService.java
│   │   ├── ClientService.java
│   │   ├── BlockedClientService.java
│   │   └── RequestLogService.java
│   │
│   ├── utils
│   │   ├── IpHashUtil.java
│   │   └── UuidUtil.java
│   │
│   └── BondApplication.java
│
└── resources
    ├── application.yml
    └── application-dev.yml
