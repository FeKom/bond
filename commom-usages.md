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
bond/
├── 📁 src/main/java/github/fekom/bond/
│   ├── 🎯 BondApplication.java
│   ├── 📁 config/
│   │   ├── 🔧 RateLimitConfig.java          // Configuração por cliente
│   │   └── 🔧 TierConfig.java               // Configuração dos planos
│   ├── 📁 service/
│   │   ├── 🎯 RateLimitService.java         // Serviço principal
│   │   ├── 🎯 TokenBucketService.java       // Implementação do algoritmo
│   │   ├── 🎯 TierService.java              // Gestão de planos
│   │   └── 📁 algorithm/
│   │       ├── 🔧 TokenBucket.java          // Algoritmo base (genérico)
│   │       └── 🔧 RateLimitAlgorithm.java   // Interface
│   ├── 📁 model/
│   │   ├── 📦 entity/
│   │   │   ├── 📦 Client.java               // Cliente + plano
│   │   │   └── 📦 RateLimitConfig.java      // Config dinâmica
│   │   ├── 📦 dto/
│   │   │   ├── 📦 RateLimitRequest.java
│   │   │   ├── 📦 RateLimitResponse.java
│   │   │   └── 📦 TierInfo.java
│   │   └── 🎯 enums/
│   │       └── 📦 TierType.java             // FREE, STARTUP, ENTERPRISE
│   └── 📁 repository/
│       ├── 🎯 ClientRepository.java
│       └── 🎯 RateLimitConfigRepository.java

