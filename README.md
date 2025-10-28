# Bond — Byte-based Token Bucket Rate Limiter

Projeto para implementar um rate limiter baseado no algoritmo Token Bucket, focado em consumo por bytes. As requisições são comprimidas antes de calcular o custo em tokens: payloads comprimidos menores consomem menos tokens; payloads comprimidos maiores consomem mais.

## Visão geral

O objetivo deste projeto é fornecer um rate limiter eficiente que conte o consumo em bytes, não em número de requisições. Isso permite priorizar/limitar cargas maiores (grandes uploads, payloads volumosos) de forma justa.

Principais ideias:

- Implementação do algoritmo Token Bucket adaptado para bytes (capacidade e taxa de refill em bytes).
- Antes de contabilizar o custo de uma requisição, o payload é comprimido e o tamanho do payload comprimido determina quantos bytes (tokens) são consumidos.
- Cada cliente tem um `TierType` (p.ex. FREE, STARTUP, ENTERPRISE) que define capacidade, refill e burst.

## Arquitetura e conceitos importantes

- Domain: entidades imutáveis que representam `Client` e `RateLimiter` (core logic).
- Persistence: entidades JPA para `client` e `rate_limiter` com mapeamento para banco de dados.
- Algoritmo: `TokenBucket` representa a lógica do bucket (capacidade, refill, burst, currentBytes, lastRefillTime).
- Compressão: antes de medir custo, o payload recebido é comprimido (ex.: gzip). O tamanho comprimido é o que determina a quantidade de tokens a consumir.

## TierType (valores atuais)

Os tiers definidos no projeto são:

- `FREE` — capacidade ~32 KB
- `STARTUP` — capacidade ~10 MB
- `ENTERPRISE` — capacidade ~1 GB

Esses tiers vêm do enum `github.fekom.bond.domain.enums.TierType` e contêm valores de `capacityBytes`, `refillRateBytesPerSecond` e `burstMultiplier`.

## Estrutura do repositório

- `src/main/java/.../domain` — entidades e lógica de domínio (ex.: `Client`, `RateLimiter`).
- `src/main/java/.../infrastructure/persistence` — entidades JPA mapeadas para o banco.
- `src/main/java/.../algorithms` — implementação do `TokenBucket`.
- `src/main/resources/db/migration` — scripts Flyway para criação de tabelas (ex.: `V1__create_client_and_rate_limiter.sql`).

## Migrações / Banco de dados

As migrações Flyway estão em `src/main/resources/db/migration`. A migração inicial cria as tabelas `client` e `rate_limiter`. O projeto assume PostgreSQL por padrão (tipos SQL usados nas migrações são compatíveis com Postgres).

## Como rodar localmente

Pré-requisitos:

- Java 17+ (ou a versão configurada no `build.gradle.kts`)
- Gradle (o wrapper `./gradlew` está incluído)
- Docker (opcional, recomendado para Postgres em desenvolvimento)

Iniciar um banco Postgres com Docker (exemplo rápido):

```bash
# start postgres container (ajuste usuário/senha/porta conforme necessário)
docker run --rm --name bond-postgres -e POSTGRES_PASSWORD=postgres -e POSTGRES_USER=postgres -e POSTGRES_DB=bond -p 5432:5432 -d postgres:15
```

Build e run da aplicação:

```bash
./gradlew build
./gradlew bootRun
```

Ao iniciar a aplicação, o Flyway aplicará as migrações automaticamente (se a configuração do datasource estiver correta), criando as tabelas necessárias.

## Endpoints e DTOs (exemplo)

O endpoint para criar um cliente espera um payload que contenha o `tier`. O DTO de request atual é `CreateClientRequest` e utiliza o `TierType` diretamente.

Exemplo de requisição JSON para criar um cliente:

```json
{
	"tier": "STARTUP"
}
```

Observações:

- O domínio gera o `id` (uma chave tipo `API_KEY_*`) e timestamps ao criar o cliente via `Client.create(tier)`.
- Certifique-se de que o enum `TierType` contenha o valor passado (case-sensitive se usado `TierType.valueOf`).

## Como o consumo de tokens é calculado

Fluxo simplificado:

1. Recebe-se uma requisição com payload.
2. O payload é comprimido (p.ex. gzip) para reduzir o tamanho a ser contabilizado.
3. O tamanho em bytes do payload comprimido é convertido diretamente em tokens a consumir.
	 - Ex.: se o payload comprimido tiver 1.500 bytes, consome 1.500 tokens.
4. O `TokenBucket` do cliente é consultado: se houver tokens suficientes, a requisição é liberada e o bucket é decrementado; caso contrário, a requisição é bloqueada ou recebe um cabeçalho que indica quanto tempo aguardar.

Vantagens dessa abordagem:

- Controla tráfego com base no uso de banda efetiva em vez de apenas contar requisições.
- Payloads que se comprimem bem terão custo menor, incentivando uso eficiente.

## Configurações importantes

- `application.yml` / `application-dev.yml` — configurações de datasource, Flyway e porta da aplicação.
- Ajuste `TierType` se quiser limites diferentes por cliente.


## Próximos passos e melhorias sugeridas

- Persistir o `TokenBucket` como um JSON (`jsonb`) em vez de colunas separadas, se preferir serializar todo o estado do bucket.
- Adicionar métricas (Prometheus/Grafana) para monitorar uso por cliente, rejeições e consumo por tier.
- Implementar políticas de quota adicionais (p. ex. limites por endpoint ou por rota).
- Permitir configuração dinâmica de tiers via API administrativa.
