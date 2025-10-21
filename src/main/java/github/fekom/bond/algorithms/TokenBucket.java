package github.fekom.bond.algorithms;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public record TokenBucket(
    String id,
    //capacidade
    int capacity,
    //quantidade atual de tokens
    int tokens,
    // taxa de reabastecimento de tokens por segundo
    long refillRate,
    // taxa de geração de tokens por segundo
    long tokensRate,
    // timestamp do último reabastecimento
    String lastRefillTimestamp,
    // timestamp de criação
    String createdAt) {

        private static final DateTimeFormatter formatter = DateTimeFormatter.ISO_LOCAL_DATE_TIME;

        public boolean isEmpty() {
            return tokens <= 0;
        }
        public boolean isFull() {
            return tokens >= capacity;
        }

        // Retorna a porcentagem de tokens disponíveis em relação à capacidade total
        public double getTokensPercentage() {
            return ((double) tokens / capacity) * 100;
        }

        public static TokenBucket emptyBucket(String id, int capacity, long refillRate, long tokensRate) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(UUID.randomUUID().toString(), capacity, 0, refillRate, tokensRate, now, now);
        }

        public static TokenBucket fullBucket(String id, int capacity, long refillRate, long tokensRate) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(UUID.randomUUID().toString(), capacity, capacity, refillRate, tokensRate, now, now);
        }

        public static TokenBucket createBucket(String id, int capacity, long refillRate, long tokensRate) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(UUID.randomUUID().toString(), capacity, capacity, refillRate, tokensRate, now, now);
        }
        public static TokenBucket addTokensToBucket(TokenBucket bucket, int tokensToAdd) {
            int newTokenCount = Math.min(bucket.tokens + tokensToAdd, bucket.capacity);
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(bucket.id, bucket.capacity, newTokenCount, bucket.refillRate, bucket.tokensRate, now, bucket.createdAt);
        }
        public static TokenBucket removeTokensFromBucket(TokenBucket bucket, int tokensToRemove) {
            int newTokenCount = Math.max(bucket.tokens - tokensToRemove, 0);
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(bucket.id, bucket.capacity, newTokenCount, bucket.refillRate, bucket.tokensRate, now, bucket.createdAt);
        }
        public static TokenBucket allowRequests(TokenBucket bucket, int tokensRequired) {
            if (bucket.tokens >= tokensRequired) {
                String now = LocalDateTime.now().format(formatter);
                return new TokenBucket(
                    bucket.id,
                    bucket.capacity,
                    bucket.tokens - tokensRequired,
                    bucket.refillRate,
                    bucket.tokensRate,
                    now,
                    bucket.createdAt
                );
            } else {
                return bucket;
            }
        }
        public static TokenBucket refillBucket(TokenBucket bucket) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(
                bucket.id,
                bucket.capacity,
                Math.min(bucket.tokens + (int)bucket.refillRate, bucket.capacity),
                bucket.refillRate,
                bucket.tokensRate,
                now,
                bucket.createdAt
            );
        }
        public static TokenBucket generateTokens(TokenBucket bucket) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(
                bucket.id,
                bucket.capacity,
                Math.min(bucket.tokens + (int)bucket.tokensRate, bucket.capacity),
                bucket.refillRate,
                bucket.tokensRate,
                now,
                bucket.createdAt
            );
        }
        public static TokenBucket resetBucket(TokenBucket bucket) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(
                bucket.id,
                bucket.capacity,
                bucket.capacity,
                bucket.refillRate,
                bucket.tokensRate,
                now,
                bucket.createdAt
            );
        }
        public static TokenBucket clearBucket(TokenBucket bucket) {
            String now = LocalDateTime.now().format(formatter);
            return new TokenBucket(
                bucket.id,
                bucket.capacity,
                0,
                bucket.refillRate,
                bucket.tokensRate,
                now,
                bucket.createdAt
            );
        }
}
