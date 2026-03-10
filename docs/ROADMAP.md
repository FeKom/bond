# Bond Roadmap — Implementation Guide

This document describes how to implement each planned feature for Bond, with code examples referencing the current codebase. Features are ordered by priority — each one builds on the previous.

---

## Feature 1: Configurable Tiers via `@ConfigurationProperties`

**Problem:** Tiers are hardcoded in the `TierType` enum. Consumers can't define custom limits without forking the library.

**Approach:** Create a `BondProperties` class that reads `bond.*` from `application.yml`, and a new `TierConfig` POJO that replaces the hardcoded enum values at runtime.

### 1.1 Create `BondProperties`

New file: `config/BondProperties.java`

```java
@ConfigurationProperties(prefix = "bond")
public class BondProperties {

    private Map<String, TierProperties> tiers = new LinkedHashMap<>();
    private long defaultBytesPerToken = 4;

    public static class TierProperties {
        private long capacityBytes;
        private long refillRateBytesPerSecond;
        private double burstMultiplier = 1.5;

        // getters and setters
    }

    // getters and setters
}
```

### 1.2 Consumer usage in `application.yml`

```yaml
bond:
  default-bytes-per-token: 4
  tiers:
    free:
      capacity-bytes: 32768
      refill-rate-bytes-per-second: 9
      burst-multiplier: 1.5
    startup:
      capacity-bytes: 10485760
      refill-rate-bytes-per-second: 2912
      burst-multiplier: 2.0
    enterprise:
      capacity-bytes: 1073741824
      refill-rate-bytes-per-second: 298261
      burst-multiplier: 3.0
    # custom tiers:
    ai-heavy:
      capacity-bytes: 536870912    # 512MB
      refill-rate-bytes-per-second: 149130
      burst-multiplier: 2.5
```

### 1.3 Register in `BondAutoConfiguration`

```java
@AutoConfiguration
@EnableConfigurationProperties(BondProperties.class)
// ... existing annotations
public class BondAutoConfiguration {
    // BondProperties is now injectable anywhere
}
```

### 1.4 What changes in `TokenBucket`

`TokenBucket` currently takes a `TierType` enum in the constructor. Add a second constructor that takes raw values:

```java
// New constructor — used when tiers come from config
public TokenBucket(long capacityBytes, long refillRateBytesPerSecond, double burstMultiplier) {
    this.tierType = null;
    this.bucketCapacityBytes = capacityBytes;
    this.refillRateBytesPerSecond = refillRateBytesPerSecond;
    this.burstMultiplier = burstMultiplier;
    this.maxBurstBytes = (long) (capacityBytes * burstMultiplier);
    this.currentBytes = capacityBytes;
    this.lastRefillTime = System.currentTimeMillis();
    this.createdAt = LocalDateTime.now();
}
```

### 1.5 Create a `TierResolver`

New file: `config/TierResolver.java`

This resolves a tier name (String) to its configuration, checking `BondProperties` first, then falling back to the `TierType` enum:

```java
public class TierResolver {

    private final BondProperties properties;

    public TierResolver(BondProperties properties) {
        this.properties = properties;
    }

    public TokenBucket createBucket(String tierName) {
        // 1. Check custom tiers from properties
        var custom = properties.getTiers().get(tierName.toLowerCase());
        if (custom != null) {
            return new TokenBucket(
                custom.getCapacityBytes(),
                custom.getRefillRateBytesPerSecond(),
                custom.getBurstMultiplier()
            );
        }

        // 2. Fall back to built-in enum
        TierType builtIn = TierType.valueOf(tierName.toUpperCase());
        return new TokenBucket(builtIn);
    }
}
```

Register this bean in `BondAutoConfiguration` and inject it into `RateLimiterService`.

### Files touched

| File | Change |
|---|---|
| `config/BondProperties.java` | **NEW** |
| `config/TierResolver.java` | **NEW** |
| `config/BondAutoConfiguration.java` | Add `@EnableConfigurationProperties`, register `TierResolver` bean |
| `algorithms/TokenBucket.java` | Add constructor with raw values |
| `api/RateLimiterService.java` | Inject `TierResolver` instead of reading `TierType` directly |

---

## Feature 2: Per-Endpoint Rate Limiting

**Problem:** `RateLimiterService.checkRateLimit()` hardcodes `String endpoint = "/api"` on line 29. Every client gets a single shared bucket.

**Approach:** Make `endpoint` a parameter of `checkRateLimit()`. The database already supports per-endpoint buckets (`rate_limiters` table has an `endpoint` column, and `RateLimiterRepository.findByClientIdAndEndPoint` exists).

### 2.1 Change `RateLimiterService`

Current signature:
```java
public RateLimiterResult checkRateLimit(String clientId, long requestSizeBytes)
```

New signature:
```java
public RateLimiterResult checkRateLimit(String clientId, String endpoint, long requestSizeBytes)
```

Remove the hardcoded `String endpoint = "/api"` line. The rest of the method stays the same — it already uses `endpoint` correctly for lookup and creation.

### 2.2 Consumer usage

```java
// Different models, different endpoints, separate buckets
rateLimiterService.checkRateLimit(clientId, "/v1/chat/completions", promptBytes);
rateLimiterService.checkRateLimit(clientId, "/v1/embeddings", embeddingBytes);
rateLimiterService.checkRateLimit(clientId, "/v1/images/generations", imageBytes);
```

### 2.3 Optional: Per-endpoint tier overrides

After Feature 1 is done, you can allow per-endpoint limits in config:

```yaml
bond:
  endpoints:
    /v1/chat/completions:
      tier: ai-heavy
    /v1/embeddings:
      tier: free
```

This requires a new `EndpointProperties` in `BondProperties` and logic in `RateLimiterService` to resolve which tier applies for a given endpoint.

### Files touched

| File | Change |
|---|---|
| `api/RateLimiterService.java` | Add `endpoint` parameter, remove hardcoded `"/api"` |

---

## Feature 3: Token-Aware Counting

**Problem:** Consumers need to manually convert tokens to bytes. The library should offer a helper.

**Approach:** A `TokenCounter` abstraction that converts token counts to byte estimates. Don't bundle a tokenizer (too heavy) — instead provide the interface and a default bytes-per-token multiplier from config.

### 3.1 Create `TokenCounter` interface

New file: `algorithms/TokenCounter.java`

```java
public interface TokenCounter {
    /**
     * Estimates the byte cost of a given number of tokens.
     */
    long tokensToBytes(long tokenCount);

    /**
     * Estimates the byte cost of a text payload.
     * Default: uses UTF-8 byte length (accurate enough for most cases).
     */
    default long estimateBytes(String text) {
        return text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length;
    }
}
```

### 3.2 Default implementation

New file: `algorithms/DefaultTokenCounter.java`

```java
public class DefaultTokenCounter implements TokenCounter {

    private final long bytesPerToken;

    public DefaultTokenCounter(long bytesPerToken) {
        this.bytesPerToken = bytesPerToken;
    }

    @Override
    public long tokensToBytes(long tokenCount) {
        return tokenCount * bytesPerToken;
    }
}
```

### 3.3 Register in auto-config

```java
@Bean
@ConditionalOnMissingBean
public TokenCounter tokenCounter(BondProperties properties) {
    return new DefaultTokenCounter(properties.getDefaultBytesPerToken());
}
```

Consumers who need exact tokenization (e.g., using jtokkit) can override:

```java
@Bean
public TokenCounter tokenCounter() {
    EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
    Encoding enc = registry.getEncoding(EncodingType.CL100K_BASE);
    return new TokenCounter() {
        @Override
        public long tokensToBytes(long tokenCount) {
            return tokenCount * 4;
        }
        @Override
        public long estimateBytes(String text) {
            return enc.countTokens(text) * 4L;
        }
    };
}
```

### 3.4 Convenience method on `RateLimiterService`

```java
public RateLimiterResult checkRateLimitByTokens(String clientId, String endpoint, long tokenCount) {
    long bytes = tokenCounter.tokensToBytes(tokenCount);
    return checkRateLimit(clientId, endpoint, bytes);
}

public RateLimiterResult checkRateLimitByText(String clientId, String endpoint, String text) {
    long bytes = tokenCounter.estimateBytes(text);
    return checkRateLimit(clientId, endpoint, bytes);
}
```

### Files touched

| File | Change |
|---|---|
| `algorithms/TokenCounter.java` | **NEW** — interface |
| `algorithms/DefaultTokenCounter.java` | **NEW** — default impl |
| `config/BondAutoConfiguration.java` | Register `TokenCounter` bean |
| `api/RateLimiterService.java` | Inject `TokenCounter`, add convenience methods |

---

## Feature 4: Event/Callback Hooks

**Problem:** Consumers have no way to react to rate limit events (logging, alerting, queueing).

**Approach:** Publish Spring `ApplicationEvent` instances. Consumers subscribe with `@EventListener` — zero coupling.

### 4.1 Create event classes

New package: `events/`

```java
public class RateLimitEvent extends ApplicationEvent {
    private final String clientId;
    private final String endpoint;
    private final boolean allowed;
    private final RateLimiterResult result;

    // constructor, getters
}
```

```java
public class RateLimitExceededEvent extends RateLimitEvent {
    private final long requestedBytes;
    private final long waitTimeMs;

    // constructor, getters
}
```

### 4.2 Publish from `RateLimiterService`

Inject `ApplicationEventPublisher`:

```java
public class RateLimiterService {

    private final ApplicationEventPublisher eventPublisher;

    // ... in checkRateLimit(), after computing result:
    eventPublisher.publishEvent(new RateLimitEvent(this, clientId, endpoint, result));

    if (!result.allowed()) {
        eventPublisher.publishEvent(
            new RateLimitExceededEvent(this, clientId, endpoint, result, requestSizeBytes)
        );
    }
}
```

### 4.3 Consumer usage

```java
@Component
public class RateLimitLogger {

    @EventListener
    public void onRateLimitExceeded(RateLimitExceededEvent event) {
        log.warn("Client {} exceeded limit on {}: wait {}ms",
            event.getClientId(), event.getEndpoint(), event.getWaitTimeMs());
        slackNotifier.alert(event);
    }
}
```

### Files touched

| File | Change |
|---|---|
| `events/RateLimitEvent.java` | **NEW** |
| `events/RateLimitExceededEvent.java` | **NEW** |
| `api/RateLimiterService.java` | Inject `ApplicationEventPublisher`, publish events |

---

## Feature 5: Sliding Window Support

**Problem:** Token bucket handles bursts well but doesn't enforce hard limits over time periods (e.g., "max 1GB per day"). AI billing is typically per-period.

**Approach:** Add a `SlidingWindowCounter` that works alongside the existing `TokenBucket`. The bucket controls burst, the window controls period totals.

### 5.1 Create `SlidingWindowCounter`

New file: `algorithms/SlidingWindowCounter.java`

```java
public class SlidingWindowCounter {

    private final long windowSizeMs;     // e.g., 86_400_000 for 1 day
    private final long maxBytesPerWindow;
    private long windowStartTime;
    private long bytesConsumedInWindow;

    public SlidingWindowCounter(long windowSizeMs, long maxBytesPerWindow) {
        this.windowSizeMs = windowSizeMs;
        this.maxBytesPerWindow = maxBytesPerWindow;
        this.windowStartTime = System.currentTimeMillis();
        this.bytesConsumedInWindow = 0;
    }

    public boolean allowRequest(long requestSizeBytes) {
        rollWindow();
        if (bytesConsumedInWindow + requestSizeBytes <= maxBytesPerWindow) {
            bytesConsumedInWindow += requestSizeBytes;
            return true;
        }
        return false;
    }

    private void rollWindow() {
        long now = System.currentTimeMillis();
        if (now - windowStartTime >= windowSizeMs) {
            // simple fixed window reset
            windowStartTime = now;
            bytesConsumedInWindow = 0;
        }
    }

    public long getRemainingBytes() {
        rollWindow();
        return maxBytesPerWindow - bytesConsumedInWindow;
    }

    public long getWindowResetTimeMs() {
        return windowStartTime + windowSizeMs - System.currentTimeMillis();
    }

    // getters for JSON serialization
}
```

### 5.2 Integrate with `RateLimiterService`

Use **dual gating**: request must pass both the token bucket (burst) AND the sliding window (period total).

```java
boolean bucketAllowed = limiter.bucket().allowRequest(requestSizeBytes);
boolean windowAllowed = limiter.window().allowRequest(requestSizeBytes);
boolean allowed = bucketAllowed && windowAllowed;
```

### 5.3 Persistence

The `SlidingWindowCounter` state needs to be persisted alongside the `TokenBucket`. Add a `window` JSONB column to `rate_limiters`, or embed it inside the existing `bucket` JSON.

### 5.4 Make it optional via config

```yaml
bond:
  sliding-window:
    enabled: true
    window-size: 24h    # daily window
    # per-tier window limits:
    free: 32768          # 32KB/day
    startup: 104857600   # 100MB/day
    enterprise: 10737418240  # 10GB/day
```

### Files touched

| File | Change |
|---|---|
| `algorithms/SlidingWindowCounter.java` | **NEW** |
| `domain/entities/RateLimiter/RateLimiter.java` | Add `SlidingWindowCounter window` field |
| `infrastructure/persistence/RateLimiter.java` | Add `window` JSONB column |
| `api/RateLimiterService.java` | Dual gating logic |
| `config/BondProperties.java` | Add sliding window config |
| `bond-schema.sql` | Add `window JSONB` column to `rate_limiters` |

---

## Feature 6: Micrometer Metrics (Optional Auto-Configuration)

**Problem:** Consumers want observability but the library shouldn't force Micrometer as a dependency.

**Approach:** A separate `@AutoConfiguration` class that only activates when Micrometer is on the classpath. Use `@ConditionalOnClass`.

### 6.1 Add Micrometer as optional dependency

In `build.gradle.kts`:
```kotlin
compileOnly("io.micrometer:micrometer-core")
```

### 6.2 Create `BondMetricsAutoConfiguration`

New file: `config/BondMetricsAutoConfiguration.java`

```java
@AutoConfiguration(after = BondAutoConfiguration.class)
@ConditionalOnClass(MeterRegistry.class)
public class BondMetricsAutoConfiguration {

    @Bean
    public BondMetrics bondMetrics(MeterRegistry registry) {
        return new BondMetrics(registry);
    }
}
```

### 6.3 Create `BondMetrics`

New file: `metrics/BondMetrics.java`

```java
public class BondMetrics {

    private final Counter allowedRequests;
    private final Counter rejectedRequests;
    private final Counter bytesConsumed;
    private final Timer checkLatency;

    public BondMetrics(MeterRegistry registry) {
        this.allowedRequests = Counter.builder("bond.requests.allowed")
            .description("Allowed rate limit checks")
            .register(registry);
        this.rejectedRequests = Counter.builder("bond.requests.rejected")
            .description("Rejected rate limit checks")
            .register(registry);
        this.bytesConsumed = Counter.builder("bond.bytes.consumed")
            .description("Total bytes consumed")
            .register(registry);
        this.checkLatency = Timer.builder("bond.check.latency")
            .description("Rate limit check latency")
            .register(registry);
    }

    public void recordCheck(RateLimiterResult result, long requestBytes) {
        if (result.allowed()) {
            allowedRequests.increment();
            bytesConsumed.increment(requestBytes);
        } else {
            rejectedRequests.increment();
        }
    }

    // Use timer for wrapping checkRateLimit calls
}
```

### 6.4 Add to imports file

```
github.fekom.bond.config.BondAutoConfiguration
github.fekom.bond.config.BondMetricsAutoConfiguration
```

### 6.5 Use from `RateLimiterService`

Inject `Optional<BondMetrics>` to keep it optional:

```java
public class RateLimiterService {
    private final Optional<BondMetrics> metrics;

    // after computing result:
    metrics.ifPresent(m -> m.recordCheck(result, requestSizeBytes));
}
```

### Files touched

| File | Change |
|---|---|
| `metrics/BondMetrics.java` | **NEW** |
| `config/BondMetricsAutoConfiguration.java` | **NEW** |
| `build.gradle.kts` | Add `compileOnly("io.micrometer:micrometer-core")` |
| `AutoConfiguration.imports` | Add `BondMetricsAutoConfiguration` |
| `api/RateLimiterService.java` | Inject optional metrics, record results |

---

## Feature 7: Redis Backend (Optional)

**Problem:** In a horizontally-scaled deployment (multiple app instances), each instance has its own JPA-backed bucket state. Rate limits aren't shared.

**Approach:** A `RateLimiterStore` abstraction with JPA and Redis implementations. Redis becomes the source of truth using atomic Lua scripts.

### 7.1 Create `RateLimiterStore` interface

New file: `infrastructure/store/RateLimiterStore.java`

```java
public interface RateLimiterStore {
    Optional<TokenBucket> getBucket(String clientId, String endpoint);
    void saveBucket(String clientId, String endpoint, TokenBucket bucket);
}
```

### 7.2 JPA implementation (current behavior)

Wrap the existing `RateLimiterRepository` logic into a `JpaRateLimiterStore`.

### 7.3 Redis implementation

New file: `infrastructure/store/RedisRateLimiterStore.java`

```java
@ConditionalOnClass(RedisTemplate.class)
public class RedisRateLimiterStore implements RateLimiterStore {

    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    @Override
    public Optional<TokenBucket> getBucket(String clientId, String endpoint) {
        String key = "bond:bucket:" + clientId + ":" + endpoint;
        String json = redisTemplate.opsForValue().get(key);
        if (json == null) return Optional.empty();
        return Optional.of(objectMapper.readValue(json, TokenBucket.class));
    }

    @Override
    public void saveBucket(String clientId, String endpoint, TokenBucket bucket) {
        String key = "bond:bucket:" + clientId + ":" + endpoint;
        redisTemplate.opsForValue().set(key, objectMapper.writeValueAsString(bucket));
    }
}
```

### 7.4 Atomic check with Lua script

For true distributed rate limiting, use a Lua script to make the check-and-decrement atomic:

```lua
-- bond_check.lua
local key = KEYS[1]
local requestBytes = tonumber(ARGV[1])
local capacity = tonumber(ARGV[2])
local refillRate = tonumber(ARGV[3])

local bucket = redis.call('HMGET', key, 'currentBytes', 'lastRefillTime')
-- ... refill logic, check, decrement, return allowed/waitTime
```

### 7.5 Config

```yaml
bond:
  store: redis   # or "jpa" (default)
```

### 7.6 Build dependency

```kotlin
compileOnly("org.springframework.boot:spring-boot-starter-data-redis")
```

### Files touched

| File | Change |
|---|---|
| `infrastructure/store/RateLimiterStore.java` | **NEW** — interface |
| `infrastructure/store/JpaRateLimiterStore.java` | **NEW** — wraps current logic |
| `infrastructure/store/RedisRateLimiterStore.java` | **NEW** |
| `config/BondAutoConfiguration.java` | Conditional bean registration based on `bond.store` |
| `build.gradle.kts` | Add `compileOnly` Redis dependency |
| `api/RateLimiterService.java` | Use `RateLimiterStore` instead of `RateLimiterRepository` directly |

---

## Feature 8: Quota Reset Scheduling

**Problem:** Some billing models require hard resets at specific times (e.g., monthly quota reset on the 1st).

**Approach:** A scheduled task that resets buckets on a cron schedule.

### 8.1 Config

```yaml
bond:
  quota-reset:
    enabled: true
    cron: "0 0 0 1 * *"  # midnight on the 1st of each month
```

### 8.2 Create `QuotaResetScheduler`

```java
@ConditionalOnProperty(prefix = "bond.quota-reset", name = "enabled", havingValue = "true")
public class QuotaResetScheduler {

    private final RateLimiterRepository repository;
    private final TierResolver tierResolver;

    @Scheduled(cron = "${bond.quota-reset.cron}")
    public void resetAllBuckets() {
        List<RateLimiter> all = repository.findAll();
        for (RateLimiter entity : all) {
            TokenBucket fresh = tierResolver.createBucket(entity.getTierName());
            entity.setBucket(fresh);
            entity.setupdatedAt(LocalDateTime.now());
        }
        repository.saveAll(all);
    }
}
```

### Files touched

| File | Change |
|---|---|
| `config/QuotaResetScheduler.java` | **NEW** |
| `config/BondProperties.java` | Add quota-reset config |
| `config/BondAutoConfiguration.java` | Register scheduler bean |

---

## Feature 9: Spring WebFilter Module (Separate Artifact)

**Problem:** Some consumers want automatic HTTP-level rate limiting without calling the service manually.

**Approach:** A separate Gradle module `bond-spring-web` that depends on `bond` and provides a servlet filter.

### 9.1 Project structure

```
bond/
├── bond-core/          ← current library (rename)
└── bond-spring-web/    ← new module
    └── src/main/java/
        └── github/fekom/bond/web/
            ├── BondWebAutoConfiguration.java
            └── BondRateLimitFilter.java
```

### 9.2 `BondRateLimitFilter`

```java
public class BondRateLimitFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                     HttpServletResponse response,
                                     FilterChain chain) throws ServletException, IOException {

        String clientId = resolveClientId(request);  // from header, JWT, API key
        String endpoint = request.getRequestURI();
        long bodySize = request.getContentLengthLong();

        RateLimiterResult result = rateLimiterService.checkRateLimit(clientId, endpoint, bodySize);

        if (result.allowed()) {
            response.setHeader("X-RateLimit-Used", String.valueOf(result.usedBytes()));
            response.setHeader("X-RateLimit-Usage", String.format("%.1f%%", result.usagePercentage()));
            chain.doFilter(request, response);
        } else {
            response.setStatus(429);
            response.setHeader("Retry-After", String.valueOf(result.waitTimeMs() / 1000));
            response.getWriter().write("{\"error\":\"rate_limit_exceeded\"}");
        }
    }
}
```

### 9.3 Consumer config

```yaml
bond:
  web:
    enabled: true
    client-id-header: "X-API-Key"    # or extract from JWT
    include-paths: ["/v1/**"]
    exclude-paths: ["/health", "/metrics"]
```

### Files touched

All new files in a separate module. The core `bond` library is not modified.

---

## Implementation Order

```
Feature 1 (ConfigurationProperties)
    │
    ├── Feature 2 (Per-Endpoint) ── can be done in parallel
    │
    ▼
Feature 3 (Token Counter)
    │
    ▼
Feature 4 (Events) ── independent, can be done anytime
    │
    ▼
Feature 5 (Sliding Window)
    │
    ├── Feature 6 (Metrics) ── independent, can be done anytime
    │
    ▼
Feature 7 (Redis)
    │
    ▼
Feature 8 (Quota Reset)
    │
    ▼
Feature 9 (Web Filter Module) ── last, depends on per-endpoint
```

Features 2, 4, and 6 are independent and can be implemented at any point. Features 1 and 2 are the highest value — they unblock all the other features and make the library usable for real AI workloads.
