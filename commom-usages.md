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
├── 📁 src/
│   └── 📁 main/
│       ├── 📁 java/
│       │   └── 📁 github/fekom/bond/
│       │       ├── 🎯 BondApplication.java
│       │       ├── 📁 controller/
│       │       │   └── 🎯 RateLimitController.java
│       │       ├── 📁 service/
│       │       │   ├── 🎯 RateLimitService.java
│       │       │   └── 📁 algorithm/
│       │       │       ├── 🔧 TokenBucketAlgorithm.java
│       │       │       └── 🔧 SlidingWindowAlgorithm.java
│       │       ├── 📁 model/
│       │       │   ├── 📦 RateLimitRequest.java
│       │       │   └── 📦 RateLimitResponse.java
│       │       └── 📁 util/
│       │           └── 🎯 ClientIPResolver.java
│       └── 📁 resources/
│           └── ⚙️ application.yml