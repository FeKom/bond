# Executar a aplicação
./gradlew bootRun

# Construir JAR
./gradlew build

# Apenas compilar
./gradlew compileJava

# Executar testes
./gradlew test

# Limpar e reconstruir
./gradlew clean build

# Ver dependências
./gradlew dependencies


# Desenvolvimento com auto-reload (se configurado)
./gradlew bootRun --continuous

# Build otimizado para produção
./gradlew build -x test

# Verificar dependências
./gradlew dependencies --configuration compileClasspath