# Bond

Byte-based rate limiter with token bucket for Spring Boot.

Bond limits HTTP traffic by **compressed payload size** (not request count), so a 10 KB POST costs more than a 200 B GET. It uses the [token bucket algorithm](https://en.wikipedia.org/wiki/Token_bucket) with configurable capacity per endpoint, per-IP overrides, and IP blocking.

## Features

- **Byte-based** -- rate limits based on GZIP-compressed request size
- **Token bucket** -- smooth refill with configurable burst multiplier
- **Per-endpoint capacity** -- different limits for `/api/upload` vs `/api/data`
- **Per-IP overrides** -- give specific clients more (or less) capacity
- **IP blocking** -- block abusive IPs entirely (returns 403)
- **Auto HTTP filter** -- automatically applies to all requests when `spring-boot-starter-web` is present
- **Pluggable storage** -- in-memory by default, JPA with PostgreSQL opt-in, or bring your own
- **Zero config** -- works out of the box with sensible defaults

## Requirements

- Java 21+
- Spring Boot 3.2+

## Installation

### Gradle

```kotlin
implementation("io.github.fekom:bond:0.0.1")
```

### Maven

```xml
<dependency>
    <groupId>io.github.fekom</groupId>
    <artifactId>bond</artifactId>
    <version>0.0.1</version>
</dependency>
```

## Quick start

Add Bond to your Spring Boot project and it works immediately with defaults:

- 32 KB capacity per IP per endpoint
- 9 bytes/second refill rate
- 1.5x burst multiplier
- In-memory storage
- HTTP filter enabled on all endpoints

No configuration needed. Requests that exceed the limit get HTTP 429 with a `Retry-After` header.

## Configuration

Configure via `application.yml`:

```yaml
bond:
  capacity-bytes: 10485760                # 10 MB global default
  refill-rate-bytes-per-second: 2912      # ~10 MB/hour
  burst-multiplier: 2.0                   # allow bursts up to 2x capacity
  filter-enabled: true                    # auto rate-limit HTTP requests (default)
  endpoints:
    /api/upload:
      capacity-bytes: 52428800            # 50 MB for uploads
      refill-rate-bytes-per-second: 14563
      burst-multiplier: 1.5
    /api/data:
      capacity-bytes: 1048576             # 1 MB for data endpoints
      refill-rate-bytes-per-second: 291
      burst-multiplier: 2.0
```

### Properties reference

| Property | Default | Description |
|---|---|---|
| `bond.capacity-bytes` | `32768` | Global bucket capacity in bytes |
| `bond.refill-rate-bytes-per-second` | `9` | Bytes refilled per second |
| `bond.burst-multiplier` | `1.5` | Burst allowance multiplier |
| `bond.filter-enabled` | `true` | Enable automatic HTTP rate limiting filter |
| `bond.endpoints.<path>.capacity-bytes` | -- | Per-endpoint capacity override |
| `bond.endpoints.<path>.refill-rate-bytes-per-second` | -- | Per-endpoint refill rate |
| `bond.endpoints.<path>.burst-multiplier` | `1.5` | Per-endpoint burst multiplier |

### Capacity resolution order

When a request arrives, Bond resolves the capacity in this order:

1. **Per-IP override** (set programmatically via `BucketStore.saveCapacityOverride()`)
2. **Per-endpoint config** (from `bond.endpoints.*` in application.yml)
3. **Global default** (from `bond.capacity-bytes`)

## HTTP filter

When `spring-boot-starter-web` is on the classpath, Bond automatically registers a servlet filter that:

1. Resolves client IP from `X-Forwarded-For` > `X-Real-IP` > `remoteAddr`
2. Calculates request size using GZIP compression
3. Checks the rate limit
4. Adds response headers on every response:

| Header | Description |
|---|---|
| `X-RateLimit-Used-Bytes` | Bytes consumed from the bucket |
| `X-RateLimit-Usage-Percent` | Bucket usage as percentage |

**When the rate limit is exceeded (429):**

```
HTTP/1.1 429 Too Many Requests
Retry-After: 11
X-RateLimit-Wait-Ms: 11000
Content-Type: application/json

{"error":"rate_limit_exceeded","retryAfterSeconds":11}
```

**When an IP is blocked (403):**

```
HTTP/1.1 403 Forbidden
Content-Type: application/json

{"error":"ip_blocked"}
```

Disable the filter with `bond.filter-enabled=false` to use `RateLimiterService` manually.

## Programmatic usage

Inject `RateLimiterService` or `BucketStore` directly:

```java
@RestController
public class MyController {

    private final RateLimiterService rateLimiter;
    private final BucketStore bucketStore;

    public MyController(RateLimiterService rateLimiter, BucketStore bucketStore) {
        this.rateLimiter = rateLimiter;
        this.bucketStore = bucketStore;
    }

    // Manual rate limit check
    @PostMapping("/api/process")
    public ResponseEntity<?> process(@RequestBody byte[] body, HttpServletRequest req) {
        RequestResult result = rateLimiter.checkRateLimit(
            req.getRemoteAddr(), "/api/process", body.length
        );

        if (result.blocked()) {
            return ResponseEntity.status(403).body("{\"error\":\"ip_blocked\"}");
        }
        if (!result.allowed()) {
            return ResponseEntity.status(429).build();
        }

        // process request...
        return ResponseEntity.ok().build();
    }

    // Block an IP
    @PostMapping("/admin/block")
    public void blockIp(@RequestParam String ip, @RequestParam String reason) {
        bucketStore.blockIp(ip, reason);
    }

    // Unblock an IP
    @DeleteMapping("/admin/block")
    public void unblockIp(@RequestParam String ip) {
        bucketStore.unblockIp(ip);
    }

    // Override capacity for a specific IP
    @PostMapping("/admin/capacity")
    public void overrideCapacity(@RequestParam String ip) {
        bucketStore.saveCapacityOverride(ip, new Capacity(
            104_857_600,  // 100 MB
            29_127,       // ~100 MB/hour
            2.0
        ));
    }
}
```

## Storage

### In-memory (default)

Works out of the box. State is lost on application restart. Good for single-instance deployments or when persistence is not needed.

### JPA with PostgreSQL

Add the dependencies to your project and Bond auto-configures `JpaBucketStore`:

```kotlin
// build.gradle.kts
implementation("org.springframework.boot:spring-boot-starter-data-jpa")
runtimeOnly("org.postgresql:postgresql")
```

Create the tables using the reference schema provided in [`bond-schema.sql`](src/main/resources/bond-schema.sql):

```sql
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
```

Or use `spring.jpa.hibernate.ddl-auto=update` to let Hibernate create them automatically.

### Custom storage

Implement `BucketStore` and declare it as a Spring bean. Bond will use your implementation instead of the defaults:

```java
@Bean
public BucketStore redisBucketStore(RedisTemplate<String, byte[]> redis) {
    return new RedisBucketStore(redis); // your implementation
}
```

The `BucketStore` interface:

```java
public interface BucketStore {
    Optional<TokenBucket> findBucket(String ipAddress, String endpoint);
    void saveBucket(String ipAddress, String endpoint, TokenBucket bucket);
    Optional<Capacity> findCapacityByIp(String ipAddress);
    void saveCapacityOverride(String ipAddress, Capacity capacity);
    boolean isBlocked(String ipAddress);
    void blockIp(String ipAddress, String reason);
    boolean unblockIp(String ipAddress);
}
```

## How it works

```
Request arrives
    |
    v
Resolve client IP (X-Forwarded-For > X-Real-IP > remoteAddr)
    |
    v
Is IP blocked? ---yes---> 403 Forbidden
    |
    no
    |
    v
Compress request body with GZIP --> compressed size = bytes to consume
    |
    v
Find or create TokenBucket for (IP, endpoint)
    |
    v
bucket.allowRequest(compressedSize)
    |
    +----- allowed ----> 200 OK (pass to next filter)
    |
    +----- rejected ----> 429 Too Many Requests + Retry-After header
    |
    v
Save bucket state
```

## Project structure

```
bond/
  src/main/java/github/fekom/bond/
    algorithms/
      TokenBucket              # Token bucket algorithm (Jackson-serializable)
    api/
      BucketStore              # Storage abstraction (interface)
      RateLimiterService       # Core rate limiting logic
      PayloadCompressor        # GZIP compression utility
    config/
      BondAutoConfiguration    # Registers InMemoryBucketStore + RateLimiterService
      BondJpaAutoConfiguration # Registers JpaBucketStore (when JPA is present)
      BondWebAutoConfiguration # Registers BondRateLimitFilter (when Web is present)
      BondProperties           # @ConfigurationProperties(prefix = "bond")
    domain/
      Capacity                 # Rate limit config (bytes, refill rate, burst)
      RequestResult            # Result of a rate limit check
    infrastructure/
      InMemoryBucketStore      # Default in-memory storage
      persistence/             # JPA entities (Client, Request, BlockedClient)
      repository/              # JPA repositories + JpaBucketStore
      web/                     # BondRateLimitFilter + CachedBodyHttpServletRequest
```

## Building from source

```bash
git clone https://github.com/FeKom/bond.git
cd bond
./gradlew build
```

Tests require Docker for Testcontainers (PostgreSQL).

## License

[MIT](LICENSE)
